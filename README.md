# sbt-protoc-gen-project

Develop, test, and package JVM-based Protocol Buffer code generators in an sbt
build. This plugin connects a generator subproject to
[sbt-protoc](https://github.com/thesamet/sbt-protoc) and creates executable JARs
for distributing that generator.

## Goals and motivation

A code generator is easiest to develop when its implementation, sample `.proto`
files, and generated-code tests live in the same build. Publishing a new generator
artifact for every edit slows down that feedback loop. Testing only the generator's
output also misses whether the generated source actually compiles and runs.

This project supports two related workflows:

- **Develop locally:** run a generator directly from a subproject's compiled
  classpath, then compile and test its output in another subproject. No intermediate
  publication or assembly is required.
- **Distribute the generator:** assemble the same implementation and its dependencies
  into executable JARs with Unix and Windows launch scripts, and publish them under
  one module name.

The generator implements the protoc plugin protocol; sbt-protoc handles invoking
protoc and managing generated sources. This plugin supplies the project wiring and
packaging needed when you maintain the generator yourself.

## Installation

Add to `project/plugins.sbt`:

```scala
addSbtPlugin("com.thesamet" % "sbt-protoc-gen-project" % "0.2.0")
```

The plugin depends on sbt-protoc and sbt-assembly. You do not need to add them
separately for the examples below.

## Run a local generator

Use two subprojects: `codeGen` contains the generator implementation, and `e2e`
contains example protobuf definitions and code that exercises the generated output.

```text
project/plugins.sbt
build.sbt
codegen/src/main/scala/example/CodeGenerator.scala
e2e/src/main/protobuf/test.proto
e2e/src/main/scala/Main.scala
```

In `build.sbt`:

```scala
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val codeGen = project
  .in(file("codegen"))
  .settings(
    scalaVersion := (
      if (sbtVersion.value.startsWith("2.")) "2.13.18" else "2.12.21"
    ),
    libraryDependencies +=
      "com.thesamet.scalapb" %% "compilerplugin" % "0.10.4"
  )

lazy val e2e = project
  .in(file("e2e"))
  .enablePlugins(LocalCodeGenPlugin)
  .settings(
    scalaVersion := "2.13.18",
    publish / skip := true,
    Compile / codeGenClasspath := (codeGen / Compile / fullClasspath).value,
    Compile / PB.targets := Seq(
      genModule("example.CodeGenerator$") -> (Compile / sourceManaged).value
    )
  )
```

The dependency version above matches the repository's integration fixtures; use
the generator libraries appropriate for your own implementation. For a complete
`CodeGenerator` extending `protocbridge.codegen.CodeGenApp`, see the
[example generator](src/sbt-test/sbt-protoc-gen-project/simple/codegen/src/main/scala/example/CodeGenerator.scala).
The trailing `$` in `genModule` identifies the compiled Scala singleton object.

Run `sbt e2e/compile` to compile the generator, generate sources from the protobuf
files, and compile those sources. Use `sbt e2e/run` or `sbt e2e/test` to exercise
them. `LocalCodeGenPlugin` disables artifact-resolution caching and requests
protobuf regeneration so that generator edits are picked up during development.

The classpath task creates the build dependency on `codeGen`. The application does
not need a `.dependsOn(codeGen)` dependency: the generator is a build tool, and its
implementation need not become part of the application's runtime classpath.

## Package and publish a generator

Add a composite project alongside the `codeGen` project above:

```scala
lazy val protocGenExample = protocGenProject(
  "protoc-gen-example",
  codeGen
).settings(
  Compile / mainClass := Some("example.CodeGenerator")
).aggregateProjectSettings(
  organization := "com.example"
)
```

This creates three sbt projects:

| Project | Purpose |
| --- | --- |
| `protoc-gen-example-unix` | Assembles a JAR with a Unix-compatible launch script. |
| `protoc-gen-example-windows` | Assembles a JAR with a Windows-compatible launch script. |
| `protoc-gen-example` | Publishes both assemblies as classified artifacts. |

Build the assemblies or publish them locally:

```sh
sbt protoc-gen-example-unix/assembly
sbt protoc-gen-example-windows/assembly
sbt protoc-gen-example/publishLocal
```

Use `protoc-gen-example/publish` after configuring your publishing repository and
credentials. The publishing project attaches a `.sh` artifact with classifier
`unix` and a `.bat` artifact with classifier `windows`. The two assembly projects
skip their own publication. These are JVM programs with launch scripts, so running
them still requires Java.

`.settings(...)` configures both assembly projects; `.aggregateProjectSettings(...)`
configures the publishing project. Each assembly inherits the generator project's
Scala version. The publishing module has `crossPaths := false`, so its name has no
Scala version suffix.

Generated projects use directories named `.protoc-gen-example`,
`.protoc-gen-example-unix`, and `.protoc-gen-example-windows` under the build root.

## Cross-building

There are three independent choices: the sbt version loading this plugin, the
Scala version used to compile your generator, and the Scala version compiling the
generated source.

### sbt plugin compatibility

This plugin is cross-built for sbt 1 and sbt 2. `addSbtPlugin` selects the artifact
appropriate for the sbt version in your build; use the same declaration for both.
The current build targets are:

| Host sbt | Scala used to build this sbt plugin | Generator Scala version for local execution |
| --- | --- | --- |
| 1.13.0 | 2.12.21 | Scala 2.12 |
| 2.0.10 | 3.8.4 | Scala 2.13, or compatible Scala 3 |

Local execution uses sbt-protoc's sandbox, which shares sbt's Scala standard
library. Select a generator compatible with that library. In particular, a
Scala 2.13 generator cannot be loaded this way by sbt 1. The integration tests use
Scala 2.12.21 for the generator on sbt 1 and Scala 2.13.18 on sbt 2.

### Cross-building generated code

The project consuming generated source can use a different Scala version from the
generator. For example, add these settings to `e2e`:

```scala
crossScalaVersions := Seq("2.12.21", "2.13.18"),
scalaVersion := "2.13.18"
```

Then run `sbt +e2e/compile`. This checks the generated source against both Scala
versions while the generator stays on the version required by the running sbt.
Your generated code and its runtime dependencies must support each target version.

### Select a generator from a project matrix

If the generator itself is a matrix project, select one concrete JVM project before
passing it to `protocGenProject` or using its classpath:

```scala
lazy val codeGen = projectMatrix
  .in(file("codegen"))
  .settings(
    libraryDependencies +=
      "com.thesamet.scalapb" %% "compilerplugin" % "0.10.4"
  )
  .jvmPlatform(scalaVersions = Seq("2.13.18", "2.12.21"))

lazy val codeGenForSbt = codeGen.jvm(
  if (scala.util.Properties.versionNumberString.startsWith("2.12."))
    "2.12.21"
  else
    "2.13.18"
)
```

Use `codeGenForSbt` in place of `codeGen` in the earlier `codeGenClasspath` and
`protocGenProject` examples. sbt 2 provides project matrices; on sbt 1, add
`sbt-projectmatrix` to the build's plugins. See the
[complete matrix fixture](src/sbt-test/sbt-protoc-gen-project/projectmatrix),
including its conditional plugin dependency, and sbt's
[cross-building documentation](https://www.scala-sbt.org/2.x/docs/en/reference/cross-building-setup.html).

Unix and Windows packaging is separate from Scala cross-building. Both artifacts
bundle the selected generator and its Scala dependencies. Because the publishing
module is not Scala-suffixed, choose one generator variant to distribute per
module/version rather than publishing multiple Scala variants to the same coordinates.

## Developing this plugin

The repository build uses sbt 2.0.10, defaults to Scala 3.8.4, and requires JDK 17.
With Nix and direnv installed and the direnv shell hook enabled, run `direnv allow`
from the repository root. `.envrc` loads `shell.nix`, which supplies sbt, JDK 17,
and the Linux C++ runtime needed by the downloaded protoc binary. Alternatively,
enter the environment with `nix-shell`.

```sh
# Compile the plugin for sbt 1 and sbt 2.
sbt +compile

# Run the simple and project-matrix integration fixtures on both sbt versions.
sbt +scripted

# Check source and build formatting.
sbt ";scalafmtCheckAll;scalafmtSbtCheck"

# Publish both plugin variants to the local Ivy repository.
sbt +publishLocal
```

The scripted tests generate and compile source, run the consuming application,
assemble both platform launchers, and generate publishing metadata. Building a
Windows launcher in these tests does not verify execution on Windows.

Releases use sbt-ci-release and Git-derived versions. The release workflow publishes
snapshots from `master` and both plugin variants for `v`-prefixed release tags.
Full Git history and tags are required to derive the version correctly.
