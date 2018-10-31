package com.eniro

import fs2.Stream
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.properties.PropertiesComponent
import org.apache.camel.impl.DefaultCamelContext
import scalaz.zio.RTS
import scalaz.zio.interop.Task
import scalaz.zio.interop.catz._

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

  val anchor = "ecoId"
  val extractKey = (line: String) => {
    val start = line.indexOf(anchor) + anchor.length
    val end = line.indexOf(",", start)
    line.substring(start, end).replaceAll("\\W", "")
  }

  val readFile: String => Stream[Task, String] = (file: String) => Stream.emits(Source.fromFile(file).getLines().toStream)

  val program: Stream[Task, Unit] = for {
    data <- readFile("/home/enrs/tools/fbevents.json")
  } yield producerTemplate.sendBodyAndHeader("direct:toKafka", data, "key", extractKey(data))

  def main(args: Array[String]): Unit = {
    val t0 = System.nanoTime()
    println(unsafeRun(program.compile.toList))
    val t1 = System.nanoTime()
    println("Elapsed time: " + (t1 - t0) + "ns")
  }

}
