package sbtprotocgenproject

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

object ProtocGenProjectPlugin extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = JvmPlugin

  object autoImport {
    def protocGenProject(
        projName: String,
        codeGen: ProjectReference
    ): ProtocGenProject = ProtocGenProject(projName, codeGen)
  }
}
