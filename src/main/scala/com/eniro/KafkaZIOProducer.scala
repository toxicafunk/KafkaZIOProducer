package com.eniro

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.properties.PropertiesComponent
import org.apache.camel.impl.DefaultCamelContext
import scalaz.Scalaz._
import scalaz.zio.{IO, RTS}

import scala.io.Source

object KafkaZIOProducer extends RTS {

  val ctx: CamelContext = new DefaultCamelContext()
  ctx.addRoutes(new RouteBuilder() {
    override def configure(): Unit = {
      from("direct:toKafka")
        .to("log:com.eniro.ZioProducer?level=INFO&showHeaders=true")
        .to("kafka:{{kafka.topic}}?brokers={{kafka.brokers}}&clientId=kafkaZioProducer")
    }
  })

  val pc = new PropertiesComponent
  pc.setLocation("classpath:/com/eniro/app.properties")
  ctx.addComponent("properties", pc)

  val producerTemplate = ctx.createProducerTemplate()
  ctx.start()

  val readFile: String => IO[Exception, Stream[String]] = (file: String) => IO.syncException(Source.fromFile(file).getLines().toStream)

  val anchor = "id"
  val extractKey = (line: String) => {
    val start = line.indexOf(anchor) + anchor.length
    val end = line.indexOf(",", start)
    line.substring(start, end).replaceAll("\\W", "")
  }

  val sendMessage: String => IO[Nothing, Unit] = (data: String) =>
    IO.sync(producerTemplate.sendBodyAndHeader("direct:toKafka", data, "key", extractKey(data)))

  /*val sendMessages: Stream[String] => IO[Nothing, Unit] = (dataStream: Stream[String]) =>
    dataStream.foldLeft(IO.unit)((acc, data) => {
      sendMessage(data).fork.flatMap(fib => acc.mappend(fib.join))
    })*/

  val sendMessages: Stream[String] => IO[Nothing, Unit] = (dataStream: Stream[String]) =>
    dataStream.foldLeft(IO.unit)((acc, data) => {
      acc.mappend(sendMessage(data))
    })

  val filename = "/home/enrs/tools/profile-events.json"
  //val filename = "/home/enrs/tools/just5k.json"
  val program: IO[Exception, Unit] = for {
    dataStream <- readFile(filename)
    _ <- sendMessages(dataStream)
  } yield ()

  def main(args: Array[String]): Unit = {
    val t0 = System.currentTimeMillis()
    unsafeRun(program)
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
  }

}
