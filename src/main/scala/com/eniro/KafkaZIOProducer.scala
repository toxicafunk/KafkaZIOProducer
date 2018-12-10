package com.eniro


import java.util.concurrent.Executors

import cakesolutions.kafka.KafkaProducer.Conf
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import scalaz.zio.{IO, RTS, Ref}

import scala.concurrent.ExecutionContext
import scala.io.Source

object KafkaZIOProducer extends RTS {

  val producer = KafkaProducer(
    Conf(new StringSerializer(), new StringSerializer(), lingerMs = 0,
      bootstrapServers = "172.18.0.2:9092,172.18.0.4:9092,172.18.0.5:9092")
  )

  val anchor = "id"
  val extractKey = (line: String) => {
    val start = line.indexOf(anchor) + anchor.length
    val end = line.indexOf(",", start)
    line.substring(start + 5, end - 1) //.replaceAll("\\W", "")
  }

  val executor = Executors.newCachedThreadPool()
  val ec = ExecutionContext.fromExecutor(executor)

  val readFile: String => IO[Nothing, (String => Unit) => Unit] = (file: String) =>
    IO.sync(Source.fromFile(file).getLines().foreach(_))

  val sendMessage: String => Unit = (data: String) => {
    val record = KafkaProducerRecord("julio.genio.stream", Some(extractKey(data)), data)
    producer.send(record)
  }

  val program: String => IO[Nothing, Unit] = (filename: String) => for {
    fe <- readFile(filename).on(ec)
    fib <- IO.sync(fe(sendMessage)).fork
    _ <- fib.join
  } yield ()

  def main(args: Array[String]): Unit = {
    val filename = args(0)
    val t0 = System.currentTimeMillis()
    unsafeRun(program(filename))
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
  }

}
