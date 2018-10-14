package com.eniro

import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.properties.PropertiesComponent
import org.apache.camel.impl.DefaultCamelContext
import scalaz.Scalaz._
import scalaz.Tag
import scalaz.Tags.Parallel
import scalaz.zio.{IO, RTS}
import scalaz.zio.interop._
import scalaz.zio.interop.scalaz72.ParIO

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

  val anchor = "ecoId"
  val extractKey = (line: String) => {
    val start = line.indexOf(anchor) + anchor.length
    val end = line.indexOf(",", start)
    line.substring(start, end).replaceAll("\\W", "")
  }

  val sendMessage: String => IO[Nothing, Unit] = (data: String) =>
    IO.sync(producerTemplate.sendBodyAndHeader("direct:toKafka", data, "key", extractKey(data)))

  val sendMessages: Stream[String] => IO[Nothing, Unit] = (dataStream: Stream[String]) =>
    dataStream.foldLeft(IO.unit)((acc, data) => {
      println(Thread.currentThread().getId + " " + data)
      acc.mappend(sendMessage(data))
    })

  //type ParIO[A] = IO[Nothing, A]

  val program: IO[Exception, Unit] = for {
    dataStream <- readFile("/home/enrs/tools/fbevents.json")
    _ <- sendMessages(dataStream)
  } yield ()

  def main(args: Array[String]): Unit = {
    println(unsafeRun(program))
  }

}
