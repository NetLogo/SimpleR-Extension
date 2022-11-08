import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

name := "Simple R Extension"

enablePlugins(ExtensionDocumentationPlugin)

lazy val rScriptFiles   = settingKey[Seq[File]]("list of R scripts to include in package and testing")
lazy val commonSettings = Seq(
  version    := "1.0.2"
, isSnapshot := true

, rScriptFiles := Seq(baseDirectory.value / ".." / "src" / "rext.R", baseDirectory.value / ".." / "src" / "rlibs.R")

, netLogoVersion        := "6.3.0"
, netLogoPackageExtras ++= rScriptFiles.value.map( (f) => (f, None) )
, netLogoZipExtras     ++= Seq(baseDirectory.value / ".." / "demos", baseDirectory.value / ".." / "README.md")

, scalaVersion          := "2.12.17"
, Test / scalaSource    := baseDirectory.value / "test"
, Compile / scalaSource := baseDirectory.value / ".." / "src" / "main"
, scalacOptions        ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint", "-release", "11")

, Compile / packageBin / artifactPath := {
    val oldPath = (Compile / packageBin / artifactPath).value.toPath
    val newPath = oldPath.getParent / s"${netLogoExtName.value}.jar"
    newPath.toFile
  }

, (Test / testOptions) ++= Seq(
    Tests.Setup( () => {
      rScriptFiles.value.foreach( (file) => IO.copyFile(file, baseDirectory.value / ".." / file.getName) )
    })
  , Tests.Cleanup( () => {
      rScriptFiles.value.foreach( (file) => IO.delete(baseDirectory.value / ".." / file.getName) )
    })
  )

, resolvers += "netlogo-lang-extension" at "https://dl.cloudsmith.io/public/netlogo/language-library/maven"
, libraryDependencies ++= Seq(
    "org.nlogo.languagelibrary" %% "language-library" % "2.2.1"
, )
)

lazy val root = (project in file("."))
  .settings(
    Compile / skip := true
  , Test    / skip := true
  )

lazy val simpleR = (project in file("root-simple-r"))
  .settings(commonSettings: _*)
  .settings(
    name := "Simple R Extension"
  , netLogoClassManager   := "org.nlogo.extensions.simpler.SimpleRExtension"
  , netLogoExtName        := "sr"
  )
  .enablePlugins(NetLogoExtension)

lazy val deprecatedR = (project in file("root-deprecated-r"))
  .settings(commonSettings: _*)
  .settings(
    name := "R Extension (Deprecated)"
  , netLogoClassManager   := "org.nlogo.extensions.simpler.DeprecatedRExtension"
  , netLogoExtName        := "r"
  , netLogoLongDescription := "Deprecated!  Please use Simple R extensions instead."
  )
  .enablePlugins(NetLogoExtension)
