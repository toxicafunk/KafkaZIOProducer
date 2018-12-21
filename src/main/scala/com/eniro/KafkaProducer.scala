package com.eniro

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import fs2.{Stream, io, text}
import java.nio.file.Paths
import java.util.concurrent.Executors

import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import cakesolutions.kafka.KafkaProducer.Conf
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object KafkaFS2Producer extends IOApp {
  private val blockingExecutionContext =
    Resource.make(IO(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))))(ec => IO(ec.shutdown()))

  val producer = KafkaProducer(
    Conf(new StringSerializer(), new StringSerializer(), lingerMs = 0,
      bootstrapServers = "172.18.0.2:9092,172.18.0.4:9092,172.18.0.5:9092")
  )

  val extractKey: String => String = (line: String) => {
    val start = line.indexOf(":")
    val end = line.indexOf(",")
    val msg = Try(line.substring(start + 2, end - 1))
    msg match {
      case Success(m) => m
      case Failure(_) => {
        println(line)
        ""
      }
    }
  }

  val sendMessage: String => Future[RecordMetadata] = (data: String) => {
    val record = KafkaProducerRecord("julio.genio.stream", Some(extractKey(data)), data)
    producer.send(record)
  }

  val processMessages: String => Stream[IO, Unit] = (file: String) => Stream.resource(blockingExecutionContext).flatMap { blockingEC =>
    io.file.readAll[IO](Paths.get(file), blockingEC, 4096)
      .through(text.utf8Decode)
      .through(text.lines)
      .map(line => sendMessage(line))
      /*.mapAsyncUnordered(6)(line => IO(sendMessage(line).onComplete {
        case Success(mdt) => ()
        case Failure(err) => println(err)
      }(blockingEC)))*/
  }

  def run(args: List[String]): IO[ExitCode] = {
    val filename = args(0)
    val t0 = System.currentTimeMillis()
    processMessages(filename).compile.drain.map(_ =>  {
      val t1 = System.currentTimeMillis()
      println("Elapsed time: " + (t1 - t0) + "ms")
    }).as(ExitCode.Success)
  }
}
