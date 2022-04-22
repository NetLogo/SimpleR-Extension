package org.nlogo.extensions.simpler.config

import java.io.{ File, FileInputStream, FileOutputStream }
import java.nio.file.Paths
import java.util.Properties

import org.nlogo.api.{ FileIO, ExtensionException }

object Config {

  def getExtensionRuntimeDirectory(codeName: String) = {
    val configLoader       = classOf[org.nlogo.extensions.simpler.config.Config].getClassLoader.asInstanceOf[java.net.URLClassLoader]
    val loaderUrls         = configLoader.getURLs()
    val loaderFiles        = loaderUrls.map( (url) => new File(url.toURI.getPath) )
    val jarName            = s"$codeName.jar"
    val maybeExtensionFile = loaderFiles.find( (f) => jarName.equals(f.getName()))
    val extensionFile      = maybeExtensionFile.getOrElse(
      throw new ExtensionException(s"Could not locate the extension $jarName file to determine the runtime directory?")
    )
    extensionFile.getParentFile
  }

  def createForPropertyFile(codeName: String): Config = {
    val propertyFileName          = s"$codeName.properties"
    val extensionRuntimeDirectory = getExtensionRuntimeDirectory(codeName)
    val maybePropertyFile         = new File(extensionRuntimeDirectory, propertyFileName)
    val propertyFile              = if (maybePropertyFile.exists) {
      maybePropertyFile
    } else {
      new File(FileIO.perUserDir(codeName), propertyFileName)
    }
    Config(propertyFile)
  }

  private def checkRuntimePath(checkPath: String, checkFlags: Seq[String]): Boolean = {
    import scala.sys.process._
    val procSetup = Seq(checkPath) ++ checkFlags
    try {
      procSetup.! == 0
    } catch {
      case _: Throwable => false
    }
  }

  def getRuntimePath(extLangBin: String, maybeConfigPath: String, checkFlags: String*): Option[String] = {
    val platformExtLangBin = extLangBin//s"$extLangBin${if (Platform.isWindows) { ".exe" } else { "" }}"
    val configRuntimePath = Paths.get(maybeConfigPath.trim(), platformExtLangBin).toString
    if (configRuntimePath != extLangBin && checkRuntimePath(configRuntimePath, checkFlags)) {
      Some(configRuntimePath)
    } else {
      // fallback to hoping it's on the PATH...
      if (checkRuntimePath(platformExtLangBin, checkFlags)) {
        Some(platformExtLangBin)
      } else {
        None
      }
    }
  }

}

case class Config(propertyFile: File) {

  protected val properties = new Properties

  if (propertyFile.exists) {
    Using(new FileInputStream(propertyFile)) { f => properties.load(f) }
  }

  def save(): Unit = {
    Using(new FileOutputStream(propertyFile)) { f => properties.store(f, "") }
  }

  def runtimePath: Option[String] = {
    Option(properties.getProperty("runtimePath")).flatMap( path => if (path.trim.isEmpty) None else Some(path) )
  }

  def runtimePath_=(path: String): Unit = {
    properties.setProperty("runtimePath", path)
  }

}
