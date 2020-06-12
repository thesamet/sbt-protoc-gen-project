package example

object Main {
  def main(args: Array[String]): Unit = {
    val messages = output.Result.messages
    println(messages)
    assert(messages == Seq("Person"))
  }
}
