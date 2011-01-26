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
import scala.tools.nsc.symtab.Flags._
import scala.tools.nsc.plugins.PluginComponent

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

  def valueCode(v: String) = FlatCode[String](Seq(), Seq(), Seq(v))
  def statementCode(s: String) = FlatCode[String](Seq(), Seq(s), Seq())
  def convert(body: Tree): FlatCode[String] = {
    def cast(expr: Tree, clType: String) =
      convert(expr).mapEachValue(v => Seq("((" + clType + ")" + v + ")"))

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
        tupleArgs.map(convert).reduceLeft(_ ++ _)
      case Literal(Constant(value)) =>
        assert(value != ())
        valueCode(value.toString)
      case Ident(name) =>
        valueCode(name.toString)
      case If(condition, then: Tree, thenElse: Tree) =>
        val Seq(c, t, e) = Seq(condition, then, thenElse).map(convert)
        val Seq(condValue) = c.values
        FlatCode[String](
          c.outerDefinitions ++ t.outerDefinitions ++ e.outerDefinitions,
          c.statements ++
          Seq(
            "if (" + condValue + ") {\n" + t.statements.mkString("\n") + "\n} else {\n" + e.statements.mkString("\n") + "\n}\n"
          ),
          Seq()
        )
        //out("((", condition, ") ? (", then, ") : (", thenElse, "))")
      case Apply(Select(target, applyName()), List(singleArg)) =>
        merge(Seq(target, singleArg).map(convert):_*) { case Seq(t, a) => Seq(t + "[" + a + "]") }
      case Assign(lhs, rhs) =>
        merge(Seq(lhs, rhs).map(convert):_*) { case Seq(l, r) => Seq(l + " = " + r + ";") }
      //case Block(statements, expression) =>
      //  assert(false)
        /*
        out(statements.flatMap(List(_, "\n")):_*)
        if (expression != EmptyTree) {
          val sub = doConvertExpr(expression, true, conversion, outer)
          out(sub._1.toString, "\n")
          retExprsBuilders = sub._2
          //out(expression, "\n")
        }*/
      case DefDef(mods, name, tparams, vparamss, tpt, body) =>
        val b = new StringBuilder
        b ++= convertTpe(body.tpe) + " " + name + "("
        var first = true
        for (param <- vparamss.flatten) {
          if (first)
            first = false
          else
            b ++= ", "
          b ++= constPref(param.mods) + convertTpe(param.tpt.tpe) + " " + param.name
        }
        b ++= ") {\n"
        val convBody = convert(body)
        convBody.statements.foreach(b ++= _)
        if (!convBody.values.isEmpty) {
          val Seq(ret) = convBody.values
          b ++= "return " + ret + ";"
        }
        b ++= "\n}"
        FlatCode[String](
          convBody.outerDefinitions ++ Seq(b.toString),
          Seq(),
          Seq()
        )
      case vd @ ValDef(paramMods, paramName, tpt: TypeTree, rhs) =>
        val convValue = convert(rhs)
        FlatCode[String](
          convValue.outerDefinitions,
          convValue.statements ++
          Seq(
            constPref(paramMods) + convertTpt(tpt) + " " + paramName + (
              if (rhs != EmptyTree) {
                val Seq(value) = convValue.values
                " = " + value
              } else 
                ""
            ) + ";"
          ),
          Seq()
        )
      //case Typed(expr, tpe) =>
      //  out(expr)
      case Match(ma @ Ident(matchName), List(CaseDef(pat, guard, body))) =>
        //for ()
        //x0$1 match {
        //  case (_1: Long,_2: Float)(Long, Float)((i @ _), (c @ _)) => i.+(c)
        //}
        //Match(Ident("x0$1"), List(CaseDef(Apply(TypeTree(), List(Bind(i, Ident("_")), Bind(c, Ident("_"))), EmptyTree Apply(Select(Ident("i"), "$plus"), List(Ident("c")
        convert(body)
      case Select(expr, toSizeTName()) => cast(expr, "size_t")
      case Select(expr, toLongName()) => cast(expr, "long")
      case Select(expr, toIntName()) => cast(expr, "int")
      case Select(expr, toShortName()) => cast(expr, "short")
      case Select(expr, toByteName()) => cast(expr, "char")
      case Select(expr, toCharName()) => cast(expr, "short")
      case Select(expr, toDoubleName()) => cast(expr, "double")
      case Select(expr, toFloatName()) => cast(expr, "float")
      case ScalaMathFunction(functionType, funName, args) =>
        convertMathFunction(functionType, funName, args)
      case Apply(s @ Select(left, name), args) =>
        val List(right) = args
        NameTransformer.decode(name.toString) match {
          case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "^^" | "&" | "&&" | "|" | "||" | "<<" | ">>" | "==" | "<" | ">" | "<=" | ">=" | "!=") =>
            merge(Seq(left, right).map(convert):_*) { case Seq(l, r) => Seq("(" + l + " " + op + " " + r + ")") }
          case n if left.toString == "scala.math.package" =>
            convertMathFunction(s.tpe, name, args)
            //merge(Seq(right).map(convert):_*) { case Seq(v) => Seq(n + "(" + v + ")") }
          case n =>
            println(nodeToStringNoComment(body))
            error("[ScalaCL] Unhandled method name in Scala -> OpenCL conversion : " + name + "\n\tleft = " + left + ",\n\targs = " + args)
            valueCode("/* Error: failed to convert " + body + " */")
        }
      case s @ Select(expr, fun) =>
        convert(expr).mapEachValue(v => {
          val fn = fun.toString
          if (fn.matches("_\\d+")) {
            Seq(v + "." + fn)
          } else {
            error("Unknown function " + s)
            Seq("/* Error: failed to convert " + body + " */")
          }
        })
      case WhileLoop(condition, content) =>
        val cs = content.map(convert)
        convert(condition).mapEachValue(v => {
          assert(cs.forall(_.values.isEmpty), cs)
          Seq(
            "while (" + v + ") {\n" +
              cs.flatMap(_.statements).mkString("\n") + "\n" +
            "}"
          )
        }).addOuters(cs.flatMap(_.outerDefinitions))
      case Apply(target, args) =>
        merge((target :: args).map(convert):_*)(seq => {
          val t :: a = seq.toList
          Seq(t + "(" + a.mkString(", ") + ")") 
        })
      case _ =>
        //println(nodeToStringNoComment(body))
        error("Failed to convert " + body.getClass.getName + ": \n" + body + " : \n" + nodeToStringNoComment(body))
        valueCode("/* Error: failed to convert " + body + " */")
    }
  }
  def convertMathFunction(functionType: Type, funName: Name, args: List[Tree]) = {
    var outers = Seq[String]()//"#include <math.h>")
    val hasDoubleParam = args.exists(_.tpe == DoubleClass.tpe)
    if (hasDoubleParam)
      outers ++= Seq("#pragma OPENCL EXTENSION cl_khr_fp64: enable")

    val normalizedArgs = args.map(_ match {
      case Select(a, toDoubleName()) => a
      case arg => arg
    })
    val convArgs = normalizedArgs.map(convert)

    assert(convArgs.forall(_.statements.isEmpty), convArgs)
    FlatCode[String](
      convArgs.flatMap(_.outerDefinitions) ++ outers,
      convArgs.flatMap(_.statements),
      Seq(
        funName + "(" +
        convArgs.zip(normalizedArgs).map({ case (convArg, normalizedArg) =>
          assert(convArg.statements.isEmpty, convArg)
          val Seq(value) = convArg.values
          //"(" + convertTpe(normalizedArg.tpe) + ")" + value
          functionType match {
            case _ //MethodType(List(param), resultType) 
            if normalizedArg.tpe != DoubleClass.tpe =>
              "(float)" + value
            case _ =>
              "(" + convertTpe(normalizedArg.tpe) + ")" + value
          }
        }).mkString(", ") +
        ")"
      )
    )
  }
  def constPref(mods: Modifiers) =
    (if (mods.hasFlag(MUTABLE)) "" else "const ") 
      
  def convertTpt(tpt: TypeTree) = convertTpe(tpt.tpe)
  def convertTpe(tpe: Type) = {
    if (tpe == null) {
      error("Null type cannot be converted to OpenCL !")
      "?"
    } else if (tpe == NoType) 
      "void" 
    else 
      tpe.toString match {
        case "Int" => "int"
        case "Long" => "long"
        case "Short" => "short"
        case "Char" => "short"
        case "Byte" => "char"
        case "Float" => "float"
        case "Double" => "double"
        case "Boolean" => "char"
        case "org.bridj.SizeT" => "size_t"
        case _ => error("Cannot convert unknown type " + tpe + " to OpenCL")
      }
  }
}
