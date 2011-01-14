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
import scala.tools.nsc.Global
import tools.nsc.plugins.PluginComponent

trait OpenCLConverter
extends MiscMatchers 
   with CodeFlattening 
{
  this: PluginComponent with WithOptions =>
  
  val global: Global
  import global._
  import definitions._

  def nodeToStringNoComment(tree: Tree): String
  
  var openclLabelIds = new Ids
  
  var placeHolderRefs = new Stack[String]

  class Conversion {
    var internalSymbols = new scala.collection.mutable.HashSet[Symbol]
  }
  def convertExpr(body: Tree): (String, Seq[String]) = { 
    val (sb, vals) = doConvertExpr(body, true, new Conversion)
    if (vals == null)
      ("", Seq(sb.toString))
    else
      (sb.toString, vals.map(_.toString))
  }
  def doConvertExpr(body: Tree, isTopLevel: Boolean, conversion: Conversion, b: StringBuilder = new StringBuilder): (StringBuilder, Seq[StringBuilder]) = {
    
    var retExprsBuilders: Seq[StringBuilder] = null
    
    def out(args: Any*): Unit = args.foreach(_ match {
      case t: Tree =>
        doConvertExpr(t, false, conversion, b)._1
      case items: List[_] =>
        var first = true
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

      /*
    def convertForeach(from: Tree, to: Tree, isUntil: Boolean, by: Tree, function: Tree) = {
        val Function(List(vd @ ValDef(paramMods, paramName, tpt, rhs)), body) = function
        val id = openclLabelIds.next
        val iVar = "iVar$" + id
        val nVal = "nVal$" + id

        out("int ", iVar, ";\n")
        out("const int ", nVal, " = ", to, ";\n")
        out("for (", iVar, " = ", from, "; ", iVar, " ", if (isUntil) "<" else "<=", " ", nVal, "; ", iVar, " += ", by, ") {\n")
        doConvertExpr(argNames + (vd.symbol -> iVar), body, false, conversion, b)._1
        out("\n}")
    }*/
    
    body match {
      case TupleCreation(tupleArgs) =>//Apply(TypeApply(Select(TupleObject(), applyName()), tupleTypes), tupleArgs) if isTopLevel =>
        retExprsBuilders = tupleArgs.map(arg => doConvertExpr(arg, false, conversion)._1)
      case Literal(Constant(value)) =>
        if (value != ())
          out(value)
      case Ident(name) =>
        out(name)/*
        val ns = name.toString
        if (ns == "_") {
          if (placeHolderRefs.isEmpty)
            error("Not expecting a placeholder here !")
          val ph = placeHolderRefs.top
          placeHolderRefs = placeHolderRefs.pop
          out(ph)
        } else {
          out(argNames.get(body.symbol).getOrElse(
            if (conversion.internalSymbols.contains(body.symbol))
              name
            else
              error("Unknown identifier : '" + name + "' (expected any of " + argNames.keys.map("'" + _ + "'").mkString(", ") + ") in : \n" + body + "\n")
          ))
        }*/
      case If(condition, then: Tree, thenElse: Tree) =>
        out("((", condition, ") ? (", then, ") : (", thenElse, "))")
      case Apply(Select(target, applyName()), List(singleArg)) =>
        out(target, "[", singleArg, "]")
      case Assign(lhs, rhs) =>
        out(lhs, " = ", rhs, ";\n")
      case Block(statements, expression) =>
        out(statements.flatMap(List(_, "\n")):_*)
        if (expression != EmptyTree) {
          val sub = doConvertExpr(expression, true, conversion)
          out(sub._1.toString, "\n")
          retExprsBuilders = sub._2
          //out(expression, "\n")
        }
      case vd @ ValDef(paramMods, paramName, tpt: TypeTree, rhs) =>
        //conversion.internalSymbols += vd.symbol //-> None
        println("vd " + vd + " : " + vd.tpe + " (sym = " + vd.symbol + ")")
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
      case Match(ma @ Ident(matchName), List(CaseDef(pat, guard, body))) =>
        //for ()
        //x0$1 match {
        //  case (_1: Long,_2: Float)(Long, Float)((i @ _), (c @ _)) => i.+(c)
        //}
        //Match(Ident("x0$1"), List(CaseDef(Apply(TypeTree(), List(Bind(i, Ident("_")), Bind(c, Ident("_"))), EmptyTree Apply(Select(Ident("i"), "$plus"), List(Ident("c")

        doConvertExpr(body, false, conversion, b)._1
      case Select(expr, toSizeTName()) => cast(expr, "size_t")
      case Select(expr, toLongName()) => cast(expr, "long")
      case Select(expr, toIntName()) => cast(expr, "int")
      case Select(expr, toShortName()) => cast(expr, "short")
      case Select(expr, toByteName()) => cast(expr, "char")
      case Select(expr, toCharName()) => cast(expr, "short")
      case Select(expr, toDoubleName()) => cast(expr, "double")
      case Select(expr, toFloatName()) => cast(expr, "float")
      case Apply(
        f @ Select(
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
        f.tpe match {
          case MethodType(List(param), resultType) if param.tpe == DoubleClass.tpe =>
            out(funName, "((float)", args, ")")
          case _ =>
            out(funName, "(", args, ")")
        }
      //case Apply(TypeApply(Select(Apply(Select(Apply(Select(predef, intWrapperName()), List(from)), funToName), List(to)), foreachName()), List(fRetType)), List(f @ List(vd @ ValDef(paramMods, paramName, tpt, rhs)), body)) =>
        //conversion.internalSymbols += vd.symbol //-> None
        //convertForeach(from, to, funToName.toString == "until", Literal(Constant(1)), f)
      //case IntRangeForeach(from, to, by, isUntil, Function(List(ValDef(paramMods, paramName, tpt, rhs)), body)) =>
      //case Apply(TypeApply(Select(Apply(Select(Apply(Select(Apply(Select(predef, intWrapperName()), List(from)), funToName), List(to)), byName()), List(by)), foreachName()), List(fRetType)), List(f: Function)) =>
      //  convertForeach(from, to, funToName.toString == "until", by, f)
      //case Apply(s @ Select(expr, fun), Nil) =>
      case Apply(s @ Select(left, name), args) =>
        NameTransformer.decode(name.toString) match {
          case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "^^" | "&" | "&&" | "|" | "||" | "<<" | ">>" | "==" | "<" | ">" | "<=" | ">=" | "!=") =>
            out("(", left, " ", op, " ", args(0), ")")
          case n =>
            println(nodeToStringNoComment(body))
            error("[ScalaCL] Unhandled method name in Scala -> OpenCL conversion : " + name)
        }
      case s @ Select(expr, fun) =>
        val fn = fun.toString
        if (fn.matches("_\\d+")) {
          out(expr, ".", fn)
        } else {
          error("Unknown function " + s)
        }
      case WhileLoop(condition, content) =>
        // all the foreach, map and reduce-like operations should have been converted to while loops already
        out("while (", condition, ") {\n")
        content.foreach(s => out(s))
        out("\n}")
      case _ =>
        println("Failed to convert " + body.getClass.getName + ": " + body)
        println(nodeToStringNoComment(body))
    }
    (b, retExprsBuilders)
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
