package com.eniro


import scalaz.zio.{IO, RTS}

import scala.io.{BufferedSource, Source}
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import cakesolutions.kafka.KafkaProducer.Conf
import org.apache.kafka.common.serialization.StringSerializer

object KafkaZIOProducer extends RTS {

  // Create a org.apache.kafka.clients.producer.KafkaProducer
  val producer = KafkaProducer(
    Conf(new StringSerializer(), new StringSerializer(), bootstrapServers = "172.18.0.2:9092,172.18.0.4:9092,172.18.0.5:9092")
  )

  val anchor = "id"
  val extractKey = (line: String) => {
    val start = line.indexOf(anchor) + anchor.length
    val end = line.indexOf(",", start)
    line.substring(start + 5, end - 1) //.replaceAll("\\W", "")
  }

  val readFile: String => IO[Nothing, (String => Unit) => Unit] = (file: String) =>
    IO.sync(Source.fromFile(file).getLines().foreach(_))

  val sendMessage: String => Unit = (data: String) => {
    val record = KafkaProducerRecord("genio.pixel.stream", Some(extractKey(data)), data)
    producer.send(record)
  }

  val program: String => IO[Nothing, Unit] = (filename: String) => for {
    fe <- readFile(filename)
    fib <- IO.sync(fe(sendMessage)).fork
  } yield fib.join

  def main(args: Array[String]): Unit = {
    val filename = args(0)
    val t0 = System.currentTimeMillis()
    unsafeRun(program(filename))
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
  }

}
