# sbt-protoc-gen-project

An sbt AutoPlugin for testing local protoc generators and publishing executable generator JARs.

## Compatibility

The build runs on sbt 2.0.10 with Scala 3.8.4 and requires JDK 17.
The plugin is cross-published for sbt 1.x and sbt 2.x.

`LocalCodeGenPlugin` uses sbt-protoc's sandbox, which shares sbt's Scala standard library.
Compile the generator project with Scala 2.12 when running sbt 1, and Scala 2.13 (or Scala 3)
when running sbt 2. The project consuming generated sources can use a different Scala version.

## Testing

With Nix and direnv installed and the direnv hook enabled in your shell, run
`direnv allow` from the repository root. The checked-in `.envrc` loads `shell.nix`,
which provides sbt and JDK 17 using your configured `<nixpkgs>`. Alternatively,
run `nix-shell` to enter the environment without direnv.

Run `sbt +scripted` to test code generation and executable JAR packaging with both sbt versions.
Run `sbt ";scalafmtCheckAll;scalafmtSbtCheck"` to check formatting.

## Publishing

The release workflow uses sbt-ci-release to publish both plugin variants from version tags.
Use `sbt +publishLocal` for local integration testing before a release.
