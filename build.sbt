name := """sbt-protoc-gen-project"""
organization := "com.thesamet"
version := "0.0.1-SNAPSHOT"

sbtPlugin := true

bintrayPackageLabels := Seq("sbt", "plugin")
bintrayVcsUrl := Some("""git@github.com:thesamet/sbt-protoc-gen-project.git""")

initialCommands in console := """import sbtprotocgenproject._"""

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.32")

enablePlugins(ScriptedPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
