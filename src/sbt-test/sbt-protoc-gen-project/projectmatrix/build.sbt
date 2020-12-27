version := "0.1"

lazy val codeGen = projectMatrix
  .in(file("codegen"))
  .settings(
    name := "test-codegen",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % "0.10.4"
    )
  )
  .jvmPlatform(scalaVersions = Seq("2.13.2", "2.12.10"))

lazy val codeGen12 = codeGen.jvm("2.12.10")

lazy val protocGenTest = protocGenProject(
  "protoc-gen-test",
  codeGen12
).settings(
  mainClass in Compile := Option("example.CodeGenerator")
)

lazy val e2e = project
  .in(file("e2e"))
  .enablePlugins(LocalCodeGenPlugin)
  .settings(
    skip in publish := true,
    crossScalaVersions := Seq("2.13.2", "2.12.10"),
    scalaVersion := "2.13.2",
    codeGenClasspath in Compile := (codeGen12 / Compile / fullClasspath).value,
    PB.targets in Compile := Seq(
      genModule("example.CodeGenerator$") -> (Compile / sourceManaged).value
    )
  )
