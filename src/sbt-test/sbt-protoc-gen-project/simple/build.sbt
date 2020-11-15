version := "0.1"
scalaVersion := "2.12.1"

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
  mainClass in Compile := Option("example.CodeGenerator")
)

lazy val e2e = project
  .in(file("e2e"))
  .enablePlugins(LocalCodeGenPlugin)
  .settings(
    skip in publish := true,
    codeGenClasspath in Compile := (codeGen / Compile / fullClasspath).value,
    PB.targets in Compile := Seq(
      genModule("example.CodeGenerator$") -> (Compile / sourceManaged).value
    )
  )
