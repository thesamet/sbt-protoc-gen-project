# sbt-protoc-gen-project

An sbt AutoPlugin for testing local protoc generators and publishing executable generator JARs.

## Compatibility

The plugin is cross-published for sbt 1.x and sbt 2.x. Building the plugin requires JDK 17.

`LocalCodeGenPlugin` uses sbt-protoc's sandbox, which shares sbt's Scala standard library.
Compile the generator project with Scala 2.12 when running sbt 1, and Scala 2.13 (or Scala 3)
when running sbt 2. The project consuming generated sources can use a different Scala version.

## Testing

Run `sbt +scripted` to test code generation and executable JAR packaging with both sbt versions.
Run `sbt scalafmtCheckAll scalafmtSbtCheck` to check formatting.

## Publishing

The release workflow uses sbt-ci-release to publish both plugin variants from version tags.
Use `sbt +publishLocal` for local integration testing before a release.
