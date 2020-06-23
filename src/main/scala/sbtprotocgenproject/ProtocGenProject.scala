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
    commonSettings: Seq[Def.SettingsDefinition]
) extends CompositeProject {
  def projDef(name: String, shebang: Boolean) =
    sbt
      .Project(name, new File(name))
      .enablePlugins(AssemblyPlugin)
      .dependsOn(codeGen)
      .settings(
        (Seq(
          assemblyOption in assembly := (assemblyOption in assembly).value.copy(
            prependShellScript = Some(
              sbtassembly.AssemblyPlugin
                .defaultUniversalScript(shebang = shebang)
            )
          ),
          // Remove when Message[T] disappears in ScalaPB 0.11.x
          assemblyMergeStrategy in assembly := {
            case PathList("scalapb", "package.class")  => MergeStrategy.discard
            case PathList("scalapb", "package$.class") => MergeStrategy.discard
            case x                                     => (assemblyMergeStrategy in assembly).value(x)
          },
          skip in publish := true
        ) ++ commonSettings): _*
      )

  def unix = projDef(s"$projName-unix", shebang = true)

  def windows = projDef(s"$projName-windows", shebang = false)

  def agg =
    sbt
      .Project(projName, new File(projName))
      .settings(
        name := projName,
        publishArtifact in (Compile, packageDoc) := false,
        publishArtifact in (Compile, packageSrc) := false,
        crossPaths := false,
        addArtifact(
          Artifact(projName, "jar", "sh", "unix"),
          assembly in (unix, Compile)
        ),
        addArtifact(
          Artifact(projName, "jar", "bat", "windows"),
          assembly in (windows, Compile)
        ),
        autoScalaLibrary := false
      )

  override def componentProjects: Seq[Project] = Seq(unix, windows, agg)

  private def isWindows = sys.props("os.name").startsWith("Windows")

  private def jarProject = if (isWindows) windows else unix

  private val osName: String = if (isWindows) "windows" else "unix"

  /* Add custom settings to both windows and unix projects */
  def settings(moreSettings: Def.SettingsDefinition*): ProtocGenProject =
    copy(commonSettings = commonSettings ++ moreSettings)

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
  ): ProtocGenProject = new ProtocGenProject(projName, codeGen, Nil)
}
