/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalacl

import scala.collection.immutable.Stack
import scala.reflect.NameTransformer
import scala.reflect.generic.{Names, Trees, Types, Constants, Universe}

trait OpenCLConverter extends MiscMatchers {
  val global: Universe//Trees with Names with Types with Constants
  import global._

  def nodeToStringNoComment(tree: Tree): String
  
  var openclLabelIds = new Ids
  
  var placeHolderRefs = new Stack[String]

  def convertExpr(argNames: Map[String, String], body: Tree, b: StringBuilder = new StringBuilder): StringBuilder = {
    def out(args: Any*): Unit = args.foreach(_ match {
      case t: Tree =>
        convertExpr(argNames, t, b)
      case items: List[_] =>
        var first = false
        for (item <- items) {
          if (first)
            first = false
          else
            b.append(", ")
          out(item)
        }
      case s: Any =>
        b.append(s)
    })
    def cast(expr: Tree, clType: String) =
      out("((", clType, ")", expr, ")")

    def convertForeach(from: Tree, to: Tree, isUntil: Boolean, by: Tree, function: Tree) = {
        val Function(List(ValDef(paramMods, paramName, tpt, rhs)), body) = function
        val id = openclLabelIds.next
        val iVar = "iVar$" + id
        val nVal = "nVal$" + id

        out("int ", iVar, ";\n")
        out("const int ", nVal, " = ", to, ";\n")
        out("for (", iVar, " = ", from, "; ", iVar, " ", if (isUntil) "<" else "<=", " ", nVal, "; ", iVar, " += ", by, ") {\n")
        convertExpr(argNames + (paramName.toString -> iVar), body, b)
        out("\n}")
    }
    body match {
      case Literal(Constant(value)) =>
        out(value)
      case Ident(name) =>
        val ns = name.toString
        if (ns == "_") {
          if (placeHolderRefs.isEmpty)
            error("Not expecting a placeholder here !")
          val ph = placeHolderRefs.top
          placeHolderRefs = placeHolderRefs.pop
          out(ph)
        }
        out(argNames.getOrElse(
          ns,
          error("Unknown identifier : '" + name + "' (expected any of " + argNames.keys.map("'" + _ + "'").mkString(", ") + ")")
        ))
      case Assign(lhs, rhs) =>
        out(lhs, " = ", rhs, ";\n")
      case Block(statements, expression) =>
        out(statements.flatMap(List(_, "\n")):_*)
        if (expression != EmptyTree)
          out(expression, "\n")
      case ValDef(paramMods, paramName, tpt: TypeTree, rhs) =>
        out(convertTpt(tpt), " ", paramName)
        rhs match {
          case Block(statements, expression) =>
            out(";\n{\n")
            out(Block(statements, EmptyTree))
            out(paramName, " = ", expression, ";\n")
            out("}\n")
          case tt: Tree =>
            if (tt != EmptyTree)
              out(" = ", tt, ";\n")
        }
      //case Typed(expr, tpe) =>
      //  out(expr)
      case Match(Ident(matchName), List(CaseDef(pat, guard, body))) =>
        //for ()
        //x0$1 match {
        //  case (_1: Long,_2: Float)(Long, Float)((i @ _), (c @ _)) => i.+(c)
        //}
        //Match(Ident("x0$1"), List(CaseDef(Apply(TypeTree(), List(Bind(i, Ident("_")), Bind(c, Ident("_"))), EmptyTree Apply(Select(Ident("i"), "$plus"), List(Ident("c")

        convertExpr(argNames + (matchName.toString + "._1" -> "?"), body, b)
      case Apply(Select(expr, toSizeTName()), Nil) => cast(expr, "size_t")
      case Apply(Select(expr, toLongName()), Nil) => cast(expr, "long")
      case Apply(Select(expr, toIntName()), Nil) => cast(expr, "int")
      case Apply(Select(expr, toShortName()), Nil) => cast(expr, "short")
      case Apply(Select(expr, toByteName()), Nil) => cast(expr, "char")
      case Apply(Select(expr, toCharName()), Nil) => cast(expr, "short")
      case Apply(Select(expr, toDoubleName()), Nil) => cast(expr, "double")
      case Apply(Select(expr, toFloatName()), Nil) => cast(expr, "float")
      case Apply(
        Select(
          Select(
            Select(
              Ident(scalaName()),
              mathName()
            ),
            packageName()
          ),
          funName
        ),
        args
      ) =>
        out(funName, "(", args, ")")
      case Apply(TypeApply(Select(Apply(Select(Apply(Select(predef, intWrapperName()), List(from)), funToName), List(to)), foreachName()), List(fRetType)), List(f: Function)) =>
        convertForeach(from, to, funToName.toString == "until", Literal(Constant(1)), f)
      //case IntRangeForeach(from, to, by, isUntil, Function(List(ValDef(paramMods, paramName, tpt, rhs)), body)) =>
      case Apply(TypeApply(Select(Apply(Select(Apply(Select(Apply(Select(predef, intWrapperName()), List(from)), funToName), List(to)), byName()), List(by)), foreachName()), List(fRetType)), List(f: Function)) =>
        convertForeach(from, to, funToName.toString == "until", by, f)
      case Apply(s @ Select(expr, fun), Nil) =>
        val fn = fun.toString
        if (fn.matches("_\\d+")) {
          out(expr, ".", fn)
        } else {
          error("Unknown function " + s)
        }
      case Apply(s @ Select(left, name), args) =>
        NameTransformer.decode(name.toString) match {
          case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "^^" | "&" | "&&" | "|" | "||" | "<<" | ">>") =>
            out(left, " ", op, " ", args(0))
          case n =>
            println(nodeToStringNoComment(body))
            error("Unhandled method name : " + name)
        }
      case _ =>
        println("Failed to convert " + body.getClass.getName + ": " + body)
        println(nodeToStringNoComment(body))
    }
    b
  }
  def convertTpt(tpt: TypeTree) = tpt.toString match {
    case "Int" => "int"
    case "Long" => "long"
    case "org.bridj.SizeT" => "size_t"
    case "Float" => "float"
    case "Double" => "double"
    case _ => error("Cannot convert unknown type " + tpt + " to OpenCL")
  }
}
