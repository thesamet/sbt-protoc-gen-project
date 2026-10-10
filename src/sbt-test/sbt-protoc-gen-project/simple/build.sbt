ThisBuild / version := "0.1"
ThisBuild / scalaVersion := (if (sbtVersion.value.startsWith("2.")) "2.13.18" else "2.12.21")

lazy val codeGen = project
  .in(file("codegen"))
  .settings(
    name := "test-codegen",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % "0.10.4"
    )
  )

lazy val protocGenTest = protocGenProject(
  "protoc-gen-test",
  codeGen
).settings(
  Compile / mainClass := Option("example.CodeGenerator")
)

lazy val e2e = project
  .in(file("e2e"))
  .enablePlugins(LocalCodeGenPlugin)
  .settings(
    publish / skip := true,
    Compile / codeGenClasspath := (codeGen / Compile / fullClasspath).value,
    Compile / PB.targets := Seq(
      genModule("example.CodeGenerator$") -> (Compile / sourceManaged).value
    )
  )
