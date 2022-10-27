package org.nlogo.extensions.simpler


import org.nlogo.api.{ Argument, Command, Context, ExtensionManager, OutputDestinationJ, PrimitiveManager, Reporter }
import org.nlogo.core.{ Syntax }
import org.nlogo.headless.HeadlessWorkspace
import org.nlogo.workspace.{ AbstractWorkspace, ExtensionManager => WorkspaceExtensionManager }

class DeprecatedRExtension extends SimpleRExtension {
  override def load(manager: PrimitiveManager): Unit = {
    // load all of the existing SimpleR prims
    super.load(manager)

    val pleaseUse = (p: String) =>
      s"Please use the Simple R extension's `sr:$p` primitive instead."

    // load deprecated versions of the old R extension prims, advising on new alternative
    manager.addPrimitive("put", DeprecatedCommand("put", Set, pleaseUse("set")))
    manager.addPrimitive("get", DeprecatedReporter("get", RunResult, pleaseUse("runresult")))

    manager.addPrimitive("eval", DeprecatedCommand("eval", Run, pleaseUse("run")))
    manager.addPrimitive("__evalDirect", DeprecatedCommand("__evalDirect", Run, pleaseUse("run")))

    manager.addPrimitive("putList", DeprecatedCommand("putList", SetList, pleaseUse("set-list")))
    manager.addPrimitive("putNamedList", DeprecatedCommand("putNamedList", SetNamedList, pleaseUse("set-named-list")))
    manager.addPrimitive("putDataframe", DeprecatedCommand("putDataFrame", SetDataFrame, pleaseUse("set-data-frame")))
    manager.addPrimitive("putAgent", DeprecatedCommand("putAgent", SetAgent, pleaseUse("set-agent")))
    manager.addPrimitive("putAgentDf", DeprecatedCommand("putAgentDf", SetAgentDataFrame, pleaseUse("set-agent-data-frame")))

    manager.addPrimitive("clear", DeprecatedCommand("clear", SetupR, pleaseUse("setup")))
    manager.addPrimitive("clearLocal", DeprecatedCommand("clearLocal", SetupR, pleaseUse("setup")))

    manager.addPrimitive("setPlotDevice", DeprecatedCommand("setPlotDevice", SetPlotDevice, pleaseUse("set-plot-device")))
    manager.addPrimitive("interactiveShell", DeprecatedCommand("interactiveShell", ShowConsole, pleaseUse("show-console")))

    val noLongerRequired =
      "No equivalent exists for this primitive in the Simple R Extension as it is no longer required.  You can safely remove this from your code."
    manager.addPrimitive("gc", DeprecatedCommand("gc", DummyCommand, noLongerRequired))
    manager.addPrimitive("stop", DeprecatedCommand("stop", DummyCommand, noLongerRequired))
    manager.addPrimitive("jri-path", DeprecatedReporter("jri-path", DummyStringReporter(""), noLongerRequired))
  }

  override def runOnce(em: ExtensionManager): Unit = {
    SimpleRExtension.resetProps("r", "R Extension (Deprecated)", classOf[DeprecatedRExtension])

    super.runOnce(em)

    val wem = em.asInstanceOf[WorkspaceExtensionManager]

    DeprecatedRExtension._warn = wem.workspace match {
      case hw: HeadlessWorkspace =>
        (message) => hw.warningMessage(message)

      case aw: AbstractWorkspace =>
        (message) => aw.outputObject(message, null, true, false, OutputDestinationJ.NORMAL)
    }
  }
}

object DeprecatedRExtension {
  protected var _warn: (String) => Unit = (_) => { }
  def warn(message: String): Unit = {
    DeprecatedRExtension._warn(message)
  }

  def createMessage(primName: String, deprecationMessage: String): String = {
    s"$deprecationMessage\n\nThe R extension and `r:$primName` are deprecated and will be removed from a future version of NetLogo.  The Simple R extension is its replacement, and it includes all of the same functionality with a much easier setup.  This version of the R extension is actually using the Simple R extension's code, but with the old R extension primitive names.\n\nPlease see the Simple R extension documentation for more info: https://github.com/NetLogo/SimpleR-Extension/blob/main/README.md#using"
  }
}

object DummyCommand extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List())
  override def perform(args: Array[Argument], context: Context): Unit = {}
}

case class DummyStringReporter(string: String) extends Reporter {
  override def getSyntax: Syntax = Syntax.reporterSyntax(right = List(), ret = Syntax.StringType)
  override def report(args: Array[Argument], context: Context): AnyRef = string
}

case class DeprecatedCommand(primName: String, prim: Command, deprecationMessage: String) extends Command {
  var hasDisplayed = false

  override def getSyntax: Syntax = prim.getSyntax

  override def perform(args: Array[Argument], context: Context): Unit = {
    if (!hasDisplayed) {
      hasDisplayed = true
      val message = DeprecatedRExtension.createMessage(primName, deprecationMessage)
      DeprecatedRExtension.warn(message)
    }
    prim.perform(args, context)
  }
}

case class DeprecatedReporter(primName: String, prim: Reporter, val deprecationMessage: String) extends Reporter {
  var hasDisplayed = false

  override def getSyntax: Syntax = prim.getSyntax

  override def report(args: Array[Argument], context: Context): AnyRef = {
    if (!hasDisplayed) {
      hasDisplayed = true
      val message = DeprecatedRExtension.createMessage(primName, deprecationMessage)
      DeprecatedRExtension.warn(message)
    }
    prim.report(args, context)
  }
}
