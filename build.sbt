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

console / initialCommands := """import sbtprotocgenproject._"""

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.5.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.1.0-RC2")

enablePlugins(ScriptedPlugin)
// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

ThisBuild / crossScalaVersions := Seq("2.12.21", "3.8.4")
ThisBuild / scalaVersion := "2.12.21"
pluginCrossBuild / sbtVersion := {
  if (scalaBinaryVersion.value == "3") "2.0.10" else "1.13.0"
}
scalacOptions ++= (if (scalaBinaryVersion.value == "2.12") Seq("-Xsource:3")
                   else Nil)
addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.2.0")

scriptedSbt := (pluginCrossBuild / sbtVersion).value
