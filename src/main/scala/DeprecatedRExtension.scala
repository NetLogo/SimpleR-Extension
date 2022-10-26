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

    // manager.addPrimitive("putNamedList", new PutNamedList())
    // manager.addPrimitive("putList", new PutList())
    // manager.addPrimitive("putDataframe", new PutDataframe())
    // manager.addPrimitive("putAgent", new PutAgent())
    // manager.addPrimitive("putAgentDf", new PutAgentDataFrame())
    // manager.addPrimitive("eval", new Eval())
    // manager.addPrimitive("__evalDirect", new EvalDirect())
    // manager.addPrimitive("gc", new GC())
    // manager.addPrimitive("clear", new ClearWorkspace())
    // manager.addPrimitive("clearLocal", new ClearLocalWorkspace())
    // manager.addPrimitive("interactiveShell", new interactiveShell())
    // manager.addPrimitive("setPlotDevice", new SetPlotDevice())
    // manager.addPrimitive("stop", new Stop())
    // manager.addPrimitive("r-home", new DebugPrim(new RPath()))
    // manager.addPrimitive("jri-path", new DebugPrim(new JRIPath()))
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

case class DeprecatedCommand(primName: String, prim: Command, deprecationMessage: String) extends Command {
  override def getSyntax: Syntax = prim.getSyntax

  override def perform(args: Array[Argument], context: Context): Unit = {
    val message = DeprecatedRExtension.createMessage(primName, deprecationMessage)
    DeprecatedRExtension.warn(message)
    prim.perform(args, context)
  }
}

case class DeprecatedReporter(primName: String, prim: Reporter, val deprecationMessage: String) extends Reporter {
  override def getSyntax: Syntax = prim.getSyntax

  override def report(args: Array[Argument], context: Context): AnyRef = {
    val message = DeprecatedRExtension.createMessage(primName, deprecationMessage)
    DeprecatedRExtension.warn(message)
    prim.report(args, context)
  }
}
