ThisBuild / version := "0.1"

lazy val codeGen = projectMatrix
  .in(file("codegen"))
  .settings(
    name := "test-codegen",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % "0.10.4"
    )
  )
  .jvmPlatform(scalaVersions = Seq("2.13.18", "2.12.21"))

lazy val codeGenForSbt = codeGen.jvm(
  if (scala.util.Properties.versionNumberString.startsWith("2.12.")) "2.12.21" else "2.13.18"
)

lazy val protocGenTest = protocGenProject(
  "protoc-gen-test",
  codeGenForSbt
).settings(
  Compile / mainClass := Option("example.CodeGenerator")
)

lazy val e2e = project
  .in(file("e2e"))
  .enablePlugins(LocalCodeGenPlugin)
  .settings(
    publish / skip := true,
    crossScalaVersions := Seq("2.13.18", "2.12.21"),
    scalaVersion := "2.13.18",
    Compile / codeGenClasspath := (codeGenForSbt / Compile / fullClasspath).value,
    Compile / PB.targets := Seq(
      genModule("example.CodeGenerator$") -> (Compile / sourceManaged).value
    )
  )
