package scala.instrumentation

import scala.language.implicitConversions

import dotty.tools.dotc._
import core._
import Contexts._
import Symbols._
import Flags._
import SymDenotations._

import Decorators._
import ast.Trees._
import ast.tpd

class Setting(configFile: Option[String]) {
  private[this] var methodId = -1
  private[this] val methods = new scala.collection.mutable.ArrayBuffer[tpd.DefDef](256)

  private[this] var config: Config = readConfig()

  def nextId(): Int =
    methodId += 1
    methodId

  def add(meth: tpd.DefDef): Int =
    methods.append(meth)
    nextId()

  def writeMethods()(using Context) = {
    val file = new java.io.File(config.methodsCSV)
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(file))
    (0 to methodId).foreach { id =>
      val methTree = methods(id)
      val meth = methTree.symbol
      // id, method, enclosing class, top-level class path, line number
      bw.write(
        id.toString + ", " +
        meth.name + ", " +
        meth.enclosingClass.name + ", " +
        meth.topLevelClass.showFullName + ", " +
        methTree.namePos.source + ", " +
        methTree.namePos.line + "\n"
      )
    }
    bw.close()
  }

  def runtimeOutputFile: String = config.resultsCSV

  def runtimeObject: String = config.runtimeObject

  private def readConfig(): Config = {
    val default = Config(methodsCSV = "methods.csv", resultsCSV = "results.csv", runtimeObject = "scala.instrumentation.Counter")

    configFile.map { file =>
      import scala.io.Source
      val bufferedSource = Source.fromFile(file)

      val config = bufferedSource.getLines.foldLeft(default) { (config, line) =>
        if line.startsWith("#") then config
        else {
          val parts = line.split(':')
          assert(parts.size == 2, "incorrect config file " + file + ", line = " + line)
          parts(0) match
            case "methodsCSV"      => config.copy(methodsCSV = parts(1).trim())
            case "resultsCSV"      => config.copy(resultsCSV = parts(1).trim())
            case "runtimeObject"   => config.copy(runtimeObject = parts(1).trim())
        }
      }
      bufferedSource.close()

      config
    }.getOrElse(default)
  }

  private case class Config(methodsCSV: String, resultsCSV: String, runtimeObject: String)

  def instrumentable(ddef: tpd.DefDef)(using Context) =
    val meth = ddef.symbol
    !ddef.rhs.isEmpty
    && !meth.isOneOf(Synthetic | Deferred | Private | Accessor)
    && (meth.owner.isTopLevelClass || meth.owner.isStaticOwner)
    || ctx.platform.isMainMethod(meth) // always instrument main
}
