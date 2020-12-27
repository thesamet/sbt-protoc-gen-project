package example

import protocbridge.codegen._
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File
import scala.collection.JavaConverters._

object CodeGenerator extends CodeGenApp {
  def process(request: CodeGenRequest): CodeGenResponse = {
    val messages = for {
      file <- request.filesToGenerate
      msg <- file.getMessageTypes().asScala
    } yield s""""${msg.getName()}""""

    CodeGenResponse.succeed(
      Seq(
        File.newBuilder
          .setName("Result.scala")
          .setContent(s"""package output
               |object Result {
               |  val messages = Seq(${messages.mkString(", ")})
               |}
               """.stripMargin)
          .build
      )
    )
  }
}
