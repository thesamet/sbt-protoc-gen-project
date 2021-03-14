inThisBuild(
  List(
    organization := "com.thesamet",
    homepage := Some(url("https://github.com/thesamet/sbt-protoc-gen-project")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "thesamet",
        "Nadav Samet",
        "thesamet@gmail.com",
        url("https://www.thesamet.com")
      )
    )
  )
)

name := """sbt-protoc-gen-project"""

sbtPlugin := true

initialCommands in console := """import sbtprotocgenproject._"""

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.2")

enablePlugins(ScriptedPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
