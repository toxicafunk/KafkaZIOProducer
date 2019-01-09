package com.eniro


import java.util.concurrent.Executors

import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

import org.apache.kafka.common.serialization.StringSerializer
import scalaz.zio.{IO, RTS}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.io.Source

object KafkaZIOProducer extends RTS {

  val props = new Properties()
  props.put("bootstrap.servers", "172.18.0.2:9092,172.18.0.4:9092,172.18.0.5:9092")
  props.put("linger.ms", new Integer(1))
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("buffer.memory", new Integer(33554432))
  props.put("batch.size", new Integer(16384))
  /*props.put("acks", "all")
  props.put("retries", 0)
  */

  val producer = new KafkaProducer[String, String](props)

  val extractKey: String => String = (line: String) => {
    val start = line.indexOf(":")
    val end = line.indexOf(",")
    line.substring(start + 2, end - 1)
  }

  val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  val readFile: String => IO[Nothing, (String => Unit) => Unit] = (file: String) =>
    IO.unyielding(IO.sync(Source.fromFile(file).getLines().foreach(_)))

  val sendMessage: String => Unit = (data: String) => {
    val record = new ProducerRecord("julio.genio.stream", extractKey(data), data)
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
    producer.close()
  }

}
