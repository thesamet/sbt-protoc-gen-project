package sbtprotocgenproject

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbtprotoc.ProtocPlugin
import protocbridge.{SandboxedJvmGenerator, Target}
import ProtocPlugin.autoImport.PB
import sbt.internal.inc.classpath.ClasspathUtilities
import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox
import collection.mutable.ListBuffer
import sbt.util.FileInfo.full
import scala.tools.nsc.util.ClassPath
import protocbridge.ProtocCodeGenerator

object MagicGenPlugin extends AutoPlugin {

  override def trigger = NoTrigger
  override def requires = JvmPlugin && ProtocPlugin

  private val MagicArtifact =
    protocbridge.Artifact("MagicGroup", "MagicArgifact", "MagicVersion")

  object autoImport {
    def protocGenProject(
        projName: String,
        codeGen: ProjectReference
    ): ProtocGenProject = ProtocGenProject(projName, codeGen)

    def magicGen(name: String, mainClass: String): SandboxedJvmGenerator =
      SandboxedJvmGenerator.forModule(
        name,
        MagicArtifact,
        mainClass,
        Nil
      )

    val codeGenClassPath = taskKey[Classpath]("code-gen-classpath")
  }

  import autoImport._

  private def mkArtifactResolver = Def.task {
    val oldResolver = (Compile / PB.artifactResolver).value
    val cp = (Compile / codeGenClassPath).value.map(_.data)
    (a: protocbridge.Artifact) =>
      a match {
        case MagicArtifact => cp
        case other         => oldResolver(other)
      }
  }

  override def projectSettings: Seq[Def.Setting[_]] = List(
    Compile / PB.cacheClassLoaders := false,
    Compile / PB.recompile := true,
    Compile / PB.artifactResolver := mkArtifactResolver.value
  )
}
