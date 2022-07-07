import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension)
enablePlugins(ExtensionDocumentationPlugin)

version    := "1.0.0"
isSnapshot := true

netLogoVersion        := "6.2.2"
netLogoClassManager   := "org.nlogo.extensions.simpler.RExtension"
netLogoExtName        := "sr"
netLogoPackageExtras ++= Seq((baseDirectory.value / "src" / "rext.R", None), (baseDirectory.value / "src" / "rlibs.R", None))
netLogoZipExtras     ++= Seq(baseDirectory.value / "demos", baseDirectory.value / "README.md")

scalaVersion          := "2.12.12"
Test / scalaSource    := baseDirectory.value / "src" / "test"
Compile / scalaSource := baseDirectory.value / "src" / "main"
scalacOptions        ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint")

Compile / packageBin / artifactPath := {
  val oldPath = (Compile / packageBin / artifactPath).value.toPath
  val newPath = oldPath.getParent / s"${netLogoExtName.value}.jar"
  newPath.toFile
}

resolvers           += "netlogo-lang-extension" at "https://dl.cloudsmith.io/public/netlogo/language-library/maven"
libraryDependencies ++= Seq(
  "org.nlogo.languagelibrary" %% "language-library" % "1.1.0"
)
