package com.eniro


import scalaz.zio.{RTS, IO}

import scala.io.Source
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

  //val filename = "/home/enrs/tools/profile-events.json"
  val filename = "./just5k.json"
  val program: IO[Exception, Unit] = for {
    fe <- readFile(filename)
    fib <- IO.sync(fe(sendMessage)).fork
  } yield fib.join

  def main(args: Array[String]): Unit = {
    val t0 = System.currentTimeMillis()
    unsafeRun(program)
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
  }

}
