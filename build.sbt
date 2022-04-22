import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension)
enablePlugins(ExtensionDocumentationPlugin)

version    := "0.1.0"
isSnapshot := true

netLogoVersion       := "6.2.2"
netLogoClassManager  := "org.nlogo.extensions.simpler.RExtension"
netLogoExtName       := "sr"
netLogoPackageExtras += (baseDirectory.value / "src" / "rext.R", None)
netLogoZipExtras    ++= Seq(baseDirectory.value / "demos", baseDirectory.value / "README.md")

scalaVersion           := "2.12.12"
scalaSource in Test    := baseDirectory.value / "src" / "test"
scalaSource in Compile := baseDirectory.value / "src" / "main"
scalacOptions         ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Xlint")

resolvers           += "netlogo-lang-extension" at "https://dl.cloudsmith.io/public/netlogo/netlogoextensionlanguageserverlibrary/maven"
libraryDependencies ++= Seq(
  "org.nlogo.langextension" %% "lang-extension-lib" % "0.3.1"
)
