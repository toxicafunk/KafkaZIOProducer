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

  val anchor = "id"
  val extractKey = (line: String) => {
    val start = line.indexOf(anchor) + anchor.length
    val end = line.indexOf(",", start)
    line.substring(start + 5, end - 1)//.replaceAll("\\W", "")
  }

  val readFile: String => IO[Nothing, (String => Unit) => Unit] = (file: String) => IO.sync(Source.fromFile(file).getLines().foreach(_))

  val sendMessage: String => Unit = (data: String) =>
    producerTemplate.sendBodyAndHeader("direct:toKafka", data, "key", extractKey(data))

  val filename = "./just5k.json"
  val program: IO[Exception, Unit] = for {
    fe <- readFile(filename)
    _ <- IO.sync(fe(sendMessage))
  } yield ()

  def main(args: Array[String]): Unit = {
    val t0 = System.currentTimeMillis()
    unsafeRun(program)
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
  }

}
