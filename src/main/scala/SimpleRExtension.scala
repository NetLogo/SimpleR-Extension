package org.nlogo.extensions.simpler

import com.fasterxml.jackson.core.JsonParser
import org.json4s.jackson.JsonMethods.mapper
import org.json4s.JsonDSL._

import org.nlogo.languagelibrary.Subprocess
import org.nlogo.languagelibrary.config.{ Config, Menu }
import org.nlogo.agent.{ Agent, AgentSet }
import org.nlogo.api.{ Argument, Command, Context, DefaultClassManager, ExtensionException, ExtensionManager, FileIO, PrimitiveManager, Reporter }
import org.nlogo.core.{ LogoList, Syntax }

import java.io.File
import java.net.ServerSocket

object SimpleRExtension {
  val codeName   = "sr"
  val longName   = "SimpleR Extension"
  val extLangBin = "Rscript"

  object MessageIds {
    val SET_NAMED_LIST = 900
    val SET_DATA_FRAME = 901
  }

  var menu: Option[Menu] = None
  val config: Config     = Config.createForPropertyFile(classOf[SimpleRExtension], SimpleRExtension.codeName)

  private var _rProcess: Option[Subprocess] = None

  def rProcess: Subprocess =
    _rProcess.getOrElse(throw new ExtensionException(
      "R process has not been started. Please run sr:setup first before any other SimpleR extension primitive"
    ))

  def rProcess_=(proc: Subprocess): Unit = {
    _rProcess.foreach(_.close())
    _rProcess = Some(proc)
  }

  def killR(): Unit = {
    _rProcess.foreach(_.close())
    _rProcess = None
  }

}

class SimpleRExtension extends DefaultClassManager {
  def load(manager: PrimitiveManager): Unit = {
    manager.addPrimitive("setup", SetupR)
    manager.addPrimitive("run", Run)
    manager.addPrimitive("runresult", RunResult)
    manager.addPrimitive("set", Set)

    manager.addPrimitive("set-list", SetList)
    manager.addPrimitive("set-named-list", SetNamedList)
    manager.addPrimitive("set-data-frame", SetDataFrame)
    manager.addPrimitive("set-agent", SetAgent)
    manager.addPrimitive("set-agent-data-frame", SetAgentDataFrame)

    // primManager.addPrimitive("setPlotDevice", new SetPlotDevice());
  }

  override def runOnce(em: ExtensionManager): Unit = {
    super.runOnce(em)
    mapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)

    SimpleRExtension.menu = Menu.create(em, SimpleRExtension.longName, SimpleRExtension.extLangBin, SimpleRExtension.config)
  }

  override def unload(em: ExtensionManager): Unit = {
    super.unload(em)
    SimpleRExtension.killR()
    SimpleRExtension.menu.foreach(_.unload())
  }

}

object SetupR extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List())

  override def perform(args: Array[Argument], context: Context): Unit = {
    val dummySocket = new ServerSocket(0);
    val port = dummySocket.getLocalPort
    dummySocket.close()

    val rExtensionDirectory = Config.getExtensionRuntimeDirectory(classOf[SimpleRExtension], SimpleRExtension.codeName)
    // see docs in `rlibs.R` for what this is about
    val maybeRLibFile     = new File(rExtensionDirectory, "rlibs.R")
    val rLibFile          = if (maybeRLibFile.exists) { maybeRLibFile } else { (new File("rlibs.R")).getCanonicalFile }
    val rLibFilePath      = rLibFile.toString
    val maybeRExtFile     = new File(rExtensionDirectory, "rext.R")
    val rExtFile          = if (maybeRExtFile.exists) { maybeRExtFile } else { (new File("rext.R")).getCanonicalFile }
    val rExtFilePath      = rExtFile.toString
    val maybeRRuntimePath = Config.getRuntimePath(
      SimpleRExtension.extLangBin
    , SimpleRExtension.config.runtimePath.getOrElse("")
    , "--version"
    )
    val rRuntimePath = maybeRRuntimePath.getOrElse(
      throw new ExtensionException(s"We couldn't find an R executable file to run.  Please make sure R is installed on your system.  Then you can tell the ${SimpleRExtension.longName} where it's located by opening the SimplerR Extension menu and selecting Configure to choose the location yourself or putting making sure ${SimpleRExtension.extLangBin} is available on your PATH.\n")
    )
    val rExtUserDirPath = FileIO.perUserDir(SimpleRExtension.codeName)

    try {
      // see docs in `rlibs.R` for what this is about
      import scala.sys.process._
      Seq(rRuntimePath, rLibFilePath, rExtUserDirPath).!
      SimpleRExtension.rProcess = Subprocess.start(
        context.workspace
      , Seq(rRuntimePath)
      , Seq(rExtFilePath, port.toString, rExtUserDirPath)
      , SimpleRExtension.codeName
      , SimpleRExtension.longName
      , Some(port)
      )
      SimpleRExtension.menu.foreach(_.setup(SimpleRExtension.rProcess.evalStringified))
    } catch {
      case e: Exception => {
        println(e)
        throw new ExtensionException(s"""The ${SimpleRExtension.longName} didn't want to start.  Make sure you are using version 4 of R.  You can also try to manually install the rjson package is installed for use by R: `install.packages("rjson", repos = "http://cran.us.r-project.org", quiet = TRUE)`.""", e)
      }
    }
  }
}

object Run extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType | Syntax.RepeatableType)
  )

  override def perform(args: Array[Argument], context: Context): Unit =
    SimpleRExtension.rProcess.exec(args.map(_.getString).mkString("\n"))
}

object RunResult extends Reporter {
  override def getSyntax: Syntax = Syntax.reporterSyntax(
    right = List(Syntax.StringType),
    ret = Syntax.WildcardType
  )

  override def report(args: Array[Argument], context: Context): AnyRef =
    SimpleRExtension.rProcess.eval(args.map(_.getString).mkString("\n"))
}

object Set extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(right = List(Syntax.StringType, Subprocess.convertibleTypesSyntax))
  override def perform(args: Array[Argument], context: Context): Unit =
    SimpleRExtension.rProcess.assign(args(0).getString, args(1).get)
}

object SetList extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType, Subprocess.convertibleTypesSyntax | Syntax.RepeatableType)
  , minimumOption = Some(2)
  , defaultOption = Some(2)
  )

  override def perform(args: Array[Argument], context: Context): Unit = {
    val name = args(0).getString
    val list = LogoList.fromIterator(args.toSeq.drop(1).map(_.get).iterator)
    SimpleRExtension.rProcess.assign(name, list)
  }
}

object SetNamedList extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType, Syntax.StringType, Subprocess.convertibleTypesSyntax | Syntax.RepeatableType)
  , minimumOption = Some(3)
  , defaultOption = Some(3)
  )

  override def perform(args: Array[Argument], context: Context): Unit = {
    val varName       = args(0).getString
    val nameValueArgs = args.toSeq.drop(1)
    if (nameValueArgs.length % 2 != 0) {
      throw new ExtensionException("Each value must have a name.")
    }
    val (logoNames, logoValues) = (0 until nameValueArgs.length by 2).map( (i) => {
      val name  = nameValueArgs(i).getString
      val value = nameValueArgs(i + 1).get
      (name, value)
    }).unzip
    val names  = SimpleRExtension.rProcess.convert.toJson(LogoList.fromIterator(logoNames.iterator))
    val values = SimpleRExtension.rProcess.convert.toJson(LogoList.fromIterator(logoValues.iterator))
    val body   = ("varName" -> varName) ~ ("values" -> values) ~ ("names" -> names)
    SimpleRExtension.rProcess.genericJson(SimpleRExtension.MessageIds.SET_NAMED_LIST, body)
  }
}

object SetDataFrame extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType, Syntax.StringType, Subprocess.convertibleTypesSyntax | Syntax.RepeatableType)
  , minimumOption = Some(3)
  , defaultOption = Some(3)
  )

  override def perform(args: Array[Argument], context: Context): Unit = {
    val varName = args(0).getString
    val nameValueArgs = args.toSeq.drop(1)
    if (nameValueArgs.length % 2 != 0) {
      throw new ExtensionException("Each value must have a name.")
    }
    val (logoNames, logoValues) = (0 until nameValueArgs.length by 2).map( (i) => {
      val name  = nameValueArgs(i).getString
      val value = nameValueArgs(i + 1).get match {
        case l: LogoList => l
        case v           => LogoList(v)
      }
      (name, value)
    }).unzip
    val logoRows = logoValues.transpose.map( (logoRow) => LogoList.fromIterator(logoRow.iterator) )
    val names    = SimpleRExtension.rProcess.convert.toJson(LogoList.fromIterator(logoNames.iterator))
    val rows     = SimpleRExtension.rProcess.convert.toJson(LogoList.fromIterator(logoRows.iterator))
    val body     = ("varName" -> varName) ~ ("rows" -> rows) ~ ("names" -> names)
    SimpleRExtension.rProcess.genericJson(SimpleRExtension.MessageIds.SET_DATA_FRAME, body)
  }
}

object SetAgent extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType, Syntax.AgentType | Syntax.AgentsetType, Syntax.StringType | Syntax.RepeatableType)
  , minimumOption = Some(3)
  , defaultOption = Some(3)
  )

  def agentToValues(agent: Agent, variableIndices: Seq[Int]): Seq[AnyRef] = {
    variableIndices.map( (i) => {
      // this logic mirrors what the Language-Library does with `agentToJson()` -Jeremy B October 2022
      agent.getVariable(i) match {
        case set: AgentSet => set.printName // <plural agentset name>
        case agent: Agent  => agent.toString // <singular agentset name> <id>
        case other: AnyRef => other
      }
    })
  }

  override def perform(args: Array[Argument], context: Context): Unit = {
    val varName  = args(0).getString
    val agentArg = args(1).get
    val names    = args.toSeq.drop(2).map(_.getString)
    val agents   = agentArg match {
      case agent: Agent  =>
        val variableIndices = names.map( (varName) => agent.world.indexOfVariable(agent, varName.toUpperCase) )
        val values          = LogoList.fromIterator(SetAgent.agentToValues(agent, variableIndices).iterator)
        values

      case set: AgentSet =>
        import scala.collection.JavaConverters._
        val sampleAgent     = set.agents.iterator.next.asInstanceOf[Agent]
        val variableIndices = names.map( (varName) => sampleAgent.world.indexOfVariable(sampleAgent, varName.toUpperCase) )
        val agentsAsRows    = set.agents.asScala.map( (agent) => SetAgent.agentToValues(agent.asInstanceOf[Agent], variableIndices) )
        val variablesAsRows = agentsAsRows.transpose.map( (values) => LogoList.fromIterator(values.iterator) )
        LogoList.fromIterator(variablesAsRows.iterator)
    }
    val values = SimpleRExtension.rProcess.convert.toJson(agents)
    val body   = ("varName" -> varName) ~ ("values" -> values) ~ ("names" -> names)
    SimpleRExtension.rProcess.genericJson(SimpleRExtension.MessageIds.SET_NAMED_LIST, body)
  }
}

object SetAgentDataFrame extends Command {
  override def getSyntax: Syntax = Syntax.commandSyntax(
    right = List(Syntax.StringType, Syntax.AgentType | Syntax.AgentsetType, Syntax.StringType | Syntax.RepeatableType)
  , minimumOption = Some(3)
  , defaultOption = Some(3)
  )

  override def perform(args: Array[Argument], context: Context): Unit = {
    val varName  = args(0).getString
    val agentArg = args(1).get
    val names    = args.toSeq.drop(2).map(_.getString)
    val agents   = agentArg match {
      case agent: Agent  =>
        val variableIndices = names.map( (varName) => agent.world.indexOfVariable(agent, varName.toUpperCase) )
        val values          = LogoList.fromIterator(SetAgent.agentToValues(agent, variableIndices).iterator)
        LogoList(values)

      case set: AgentSet =>
        import scala.collection.JavaConverters._
        val sampleAgent     = set.agents.iterator.next.asInstanceOf[Agent]
        val variableIndices = names.map( (varName) => sampleAgent.world.indexOfVariable(sampleAgent, varName.toUpperCase) )
        val agentsAsRows    = set.agents.asScala.map( (agent) => LogoList.fromIterator(SetAgent.agentToValues(agent.asInstanceOf[Agent], variableIndices).iterator) )
        LogoList.fromIterator(agentsAsRows.iterator)
    }
    val values = SimpleRExtension.rProcess.convert.toJson(agents)
    val body   = ("varName" -> varName) ~ ("rows" -> values) ~ ("names" -> names)
    SimpleRExtension.rProcess.genericJson(SimpleRExtension.MessageIds.SET_DATA_FRAME, body)
  }
}
