package com.eniro


import java.util.concurrent.Executors

import cakesolutions.kafka.KafkaProducer.Conf
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import scalaz.zio.{IO, RTS}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.io.Source

object KafkaZIOProducer extends RTS {

  val producer = KafkaProducer(
    Conf(new StringSerializer(), new StringSerializer(), lingerMs = 0,
      bootstrapServers = "172.18.0.2:9092,172.18.0.4:9092,172.18.0.5:9092")
  )

  val extractKey: String => String = (line: String) => {
    val start = line.indexOf(":")
    val end = line.indexOf(",")
    line.substring(start + 2, end - 1)
  }

  val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  val readFile: String => IO[Nothing, (String => Unit) => Unit] = (file: String) =>
    IO.sync(Source.fromFile(file).getLines().foreach(_))

  val sendMessage: String => Unit = (data: String) => {
    val record = KafkaProducerRecord("julio.genio.stream", Some(extractKey(data)), data)
    producer.send(record)
  }

  val program: String => IO[Nothing, Unit] = (filename: String) => for {
    fe <- readFile(filename).on(ec)
  } yield fe(sendMessage)

  def main(args: Array[String]): Unit = {
    val filename = args(0)
    val t0 = System.currentTimeMillis()
    unsafeRun(program(filename))
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
  }

}
