package org.nlogo.extensions.simpleR

import com.fasterxml.jackson.core.JsonParser
import org.json4s.jackson.JsonMethods.mapper
import org.nlogo.langextension.{ShellWindow, Subprocess}
import org.nlogo.api
import org.nlogo.api._
import org.nlogo.app.App
import org.nlogo.core.Syntax

import java.awt.{GraphicsEnvironment, MenuBar}
import java.io.File
import java.net.ServerSocket
import javax.swing.JMenu

object RExtension {
  private var _rProcess: Option[Subprocess] = None
  var shellWindow : Option[ShellWindow] = None

  val extDirectory: File = new File(
    getClass.getClassLoader.asInstanceOf[java.net.URLClassLoader].getURLs()(0).toURI.getPath
  ).getParentFile

  def rProcess: Subprocess =
    _rProcess.getOrElse(throw new ExtensionException((
      "R process has not been started. Please run simpleR:setup first before any other simpleR extension primitive"
      )))

  def rProcess_=(proc: Subprocess): Unit = {
    _rProcess.foreach(_.close())
    _rProcess = Some(proc)
  }

  def killR(): Unit = {
    _rProcess.foreach(_.close())
    _rProcess = None
  }
}

class RExtension extends DefaultClassManager {
  var extensionMenu: Option[JMenu] = None

  def load(manager: PrimitiveManager): Unit = {
    manager.addPrimitive("setup", SetupR)
    manager.addPrimitive("run", Run)
    manager.addPrimitive("runresult", RunResult)
    manager.addPrimitive("set", Set)
  }

  override def runOnce(em: ExtensionManager): Unit = {
    super.runOnce(em)
    mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)

    if (!GraphicsEnvironment.isHeadless) {
      RExtension.shellWindow = Some(new ShellWindow())

      val menuBar = App.app.frame.getJMenuBar
      val maybeMenuItem = menuBar.getComponents.collectFirst{
        case mi: JMenu if mi.getText == ExtensionMenu.name => mi
      }
      if (maybeMenuItem.isEmpty) {
        extensionMenu = Option(menuBar.add(new ExtensionMenu))
      }
    }
  }

  override def unload(em: ExtensionManager): Unit = {
    super.unload(em);
    RExtension.killR()
    RExtension.shellWindow.foreach(sw => sw.setVisible(false))
    if (!GraphicsEnvironment.isHeadless) {
      extensionMenu.foreach(App.app.frame.getJMenuBar.remove(_))
    }
  }
}

object SetupR extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List())

  override def perform(args: Array[Argument], context: Context): Unit = {
    val dummySocket = new ServerSocket(0);
    val port = dummySocket.getLocalPort
    dummySocket.close()

    val rScript: String = new File(RExtension.extDirectory, "rext.R").toString
    try {
      RExtension.rProcess = Subprocess.start(context.workspace,
        Seq("Rscript"),
        Seq(rScript, port.toString),
        "simpleR",
        "Simple R Extension",
        Some(port))
      RExtension.shellWindow.foreach(sw => sw.setEvalStringified(Some(RExtension.rProcess.evalStringified)))
    } catch {
      case e: Exception => {
        println(e)
        throw new ExtensionException("SimpleR didn't want to start")
      }
    }
  }
}

object Run extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType | Syntax.RepeatableType)
  )

  override def perform(args: Array[Argument], context: Context): Unit =
    RExtension.rProcess.exec(args.map(_.getString).mkString("\n"))
}

object RunResult extends api.Reporter {
  override def getSyntax: Syntax = Syntax.reporterSyntax(
    right = List(Syntax.StringType),
    ret = Syntax.WildcardType
  )

  override def report(args: Array[Argument], context: Context): AnyRef =
    RExtension.rProcess.eval(args.map(_.getString).mkString("\n"))
}

object Set extends api.Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List(Syntax.StringType, Subprocess.convertibleTypesSyntax))
  override def perform(args: Array[Argument], context: Context): Unit =
    RExtension.rProcess.assign(args(0).getString, args(1).get)
}

object ExtensionMenu {
  val name = "SimpleR"
}

class ExtensionMenu extends JMenu("SimpleR") {
  add("Pop-out Interpreter").addActionListener{ _ =>
    RExtension.shellWindow.map(sw => sw.setVisible(!sw.isVisible))
  }
}