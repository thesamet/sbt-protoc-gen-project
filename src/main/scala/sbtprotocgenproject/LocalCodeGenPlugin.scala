package sbtprotocgenproject

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbtprotoc.ProtocPlugin
import protocbridge.SandboxedJvmGenerator
import ProtocPlugin.autoImport.PB
import scala.reflect.runtime.universe
import protocbridge.ProtocCodeGenerator

/** Lets you use code generators from local subprojects */
object LocalCodeGenPlugin extends AutoPlugin {

  override def trigger = NoTrigger
  override def requires = JvmPlugin && ProtocPlugin

  private val DummyArtifact =
    protocbridge.Artifact("DummyGroup", "DummyArtifact", "DummyVersion")

  object autoImport {
    def protocGenProject(
        projName: String,
        codeGen: ProjectReference
    ): ProtocGenProject = ProtocGenProject(projName, codeGen)

    def genModule(mainClass: String): SandboxedJvmGenerator =
      SandboxedJvmGenerator.forModule(
        "codegen",
        DummyArtifact,
        mainClass,
        Nil
      )

    val codeGenClasspath = taskKey[Classpath]("code-gen-classpath")
  }

  import autoImport._

  private def mkArtifactResolver = Def.task {
    val oldResolver = (Compile / PB.artifactResolver).value
    val cp = (Compile / codeGenClasspath).value.map(_.data)
    (a: protocbridge.Artifact) =>
      a match {
        case DummyArtifact => cp
        case other         => oldResolver(other)
      }
  }

  override def projectSettings: Seq[Def.Setting[_]] = List(
    Compile / PB.cacheArtifactResolution := false,
    Compile / PB.recompile := true,
    Compile / PB.artifactResolver := mkArtifactResolver.value
  )
}
