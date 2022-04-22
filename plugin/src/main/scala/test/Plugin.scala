package test

import dotty.tools.dotc.ast.Trees.*
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Decorators.*
import dotty.tools.dotc.core.StdNames.*
import dotty.tools.dotc.core.Symbols.*
import dotty.tools.dotc.core.Names.*
import dotty.tools.dotc.core.Types.*
import dotty.tools.dotc.core.Flags.*

import dotty.tools.dotc.printing.*

import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}
import dotty.tools.dotc.transform.{Pickler, Inlining}
import scala.quoted.runtime.impl.QuotesImpl
import scala.quoted.*

class Plugin extends StandardPlugin:
  val name: String                 = "Debug"
  override val description: String = "Debug"

  def init(options: List[String]): List[PluginPhase] =
    (new DebugPhase) :: Nil

class DebugPhase extends PluginPhase:
  import tpd.*

  val phaseName = "Debug"

  override val runsAfter  = Set(Pickler.name)
  override val runsBefore = Set(Inlining.name)

  private var printlnSym: Symbol = _

  override def prepareForUnit(tree: Tree)(using ctx: Context): Context =
    //get println symbol
    val predef = requiredModule("scala.Predef")
    printlnSym = predef.requiredMethod("println", List(defn.AnyType))
    ctx

  override def transformDefDef(tree: DefDef)(using ctx: Context): Tree =
    val sym = tree.symbol

    // ignore abstract and synthetic methods
    if tree.rhs.isEmpty|| sym.isOneOf(Synthetic | Deferred | Private | Accessor)
    then return tree
    try {
      println("\n\n\n\n")
      println("========================== tree ==========================")
      println(tree.show)

      // val body = {tree.rhs}
      val body = ValDef(
        newSymbol(
        tree.symbol, termName("body"), tree.symbol.flags, tree.rhs.tpe),
        Block(Nil, tree.rhs)
      )
      
      // println(body)
      val bodyRef  = ref(body.symbol)
      val printRes = ref(printlnSym).appliedTo(bodyRef)

      // shove it all together in a block
      val rhs1 = tpd.Block(body :: printRes :: Nil, bodyRef)

      //replace RHS with new
      val newDefDef = cpy.DefDef(tree)(rhs = rhs1)
      println("====================== transformed ======================")
      println(newDefDef.show)
      newDefDef
    } catch {
      case e =>
        println("====================== error ===========================")
        println(e)
        println(e.printStackTrace)
        tree
    }