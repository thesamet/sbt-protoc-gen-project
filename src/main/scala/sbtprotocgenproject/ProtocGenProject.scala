package sbtprotocgenproject

import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyKeys._
import sbt.Keys._
import sbt.Compile
import java.io.File
import sbt.ProjectRef
import sbt.addArtifact
import sbt.librarymanagement.Artifact
import sbt.Project
import sbt.ProjectReference
import sbt.CompositeProject
import sbtprotoc.ProtocPlugin.autoImport.PB
import sbt.io.syntax._
import sbt.Def
import sbtassembly.PathList
import sbtassembly.MergeStrategy

final case class ProtocGenProject private (
    projName: String,
    codeGen: ProjectReference,
    commonSettings: Seq[Def.SettingsDefinition],
    aggSettings: Seq[Def.SettingsDefinition]
) extends CompositeProject {
  def projDef(name: String, shebang: Boolean) =
    sbt
      .Project(name, new File("." + name))
      .enablePlugins(AssemblyPlugin)
      .dependsOn(codeGen)
      .settings(
        (Seq(
          assembly / assemblyOption := (assembly / assemblyOption).value
            .withPrependShellScript(
              sbtassembly.AssemblyPlugin
                .defaultUniversalScript(shebang = shebang)
            ),
          // Remove when Message[T] disappears in ScalaPB 0.11.x
          assembly / assemblyMergeStrategy := {
            case PathList("scalapb", "package.class")  => MergeStrategy.discard
            case PathList("scalapb", "package$.class") => MergeStrategy.discard
            // Workaround for https://github.com/scala/a-collection-compat/issues/426
            case PathList(
                  "scala",
                  "annotation",
                  "nowarn.class" | "nowarn$.class"
                ) =>
              MergeStrategy.first
            // compilerplugin and runtime ship with the Java generated Scalapb.proto
            case PathList("scalapb", "options", _*) => MergeStrategy.first
            case x => (assembly / assemblyMergeStrategy).value(x)
          },
          publish / skip := true
        ) ++ commonSettings): _*
      )

  def unix = projDef(s"$projName-unix", shebang = true)

  def windows = projDef(s"$projName-windows", shebang = false)

  def agg =
    sbt
      .Project(projName, new File("." + projName))
      .settings(
        (Seq(
          name := projName,
          Compile / packageDoc / publishArtifact := false,
          Compile / packageSrc / publishArtifact := false,
          crossPaths := false,
          addArtifact(
            Artifact(projName, "jar", "sh", "unix"),
            unix / Compile / assembly
          ),
          addArtifact(
            Artifact(projName, "jar", "bat", "windows"),
            windows / Compile / assembly
          ),
          autoScalaLibrary := false
        ) ++ aggSettings): _*
      )

  override def componentProjects: Seq[Project] = Seq(unix, windows, agg)

  private def isWindows = sys.props("os.name").startsWith("Windows")

  private def jarProject = if (isWindows) windows else unix

  private val osName: String = if (isWindows) "windows" else "unix"

  /* Adds custom settings to both windows and unix projects */
  def settings(moreSettings: Def.SettingsDefinition*): ProtocGenProject =
    copy(commonSettings = commonSettings ++ moreSettings)

  /* Adds custom settings to the aggregated project */
  def aggregateProjectSettings(
      moreSettings: Def.SettingsDefinition*
  ): ProtocGenProject =
    copy(aggSettings = aggSettings ++ moreSettings)

  /* Use this in PB.targets to depend on this plugin, required incorporating the addDependency setting in
     the callin project
   */
  def plugin = Def.setting(
    PB.gens.plugin(
      "assembled",
      (jarProject / assembly / target).value / s"$projName-$osName-assembly-" + version.value + ".jar"
    )
  )
}

object ProtocGenProject {
  def apply(
      projName: String,
      codeGen: ProjectReference
  ): ProtocGenProject = new ProtocGenProject(projName, codeGen, Nil, Nil)
}
