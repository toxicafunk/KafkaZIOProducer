package com.eniro

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import fs2.{Stream, io, text}
import java.nio.file.Paths
import java.util.concurrent.Executors

import cakesolutions.kafka.KafkaProducer
import cakesolutions.kafka.KafkaProducer.Conf
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.ExecutionContext

object Converter extends IOApp {
  private val blockingExecutionContext =
    Resource.make(IO(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))))(ec => IO(ec.shutdown()))

  val producer = KafkaProducer(
    Conf(new StringSerializer(), new StringSerializer(), lingerMs = 0,
      bootstrapServers = "172.18.0.2:9092,172.18.0.4:9092,172.18.0.5:9092")
  )

  val extractKey: String => String = (line: String) => {
    val start = line.indexOf(":")
    val end = line.indexOf(",")
    line.substring(start + 2, end - 1)
  }

  val processMessages: Stream[IO, Unit] = (file: String) => Stream.resource(blockingExecutionContext).flatMap { blockingEC =>

    io.file.readAll[IO](Paths.get(file), blockingEC, 4096)
      .through(text.utf8Decode)
      .through(text.lines)
      .map(line => fahrenheitToCelsius(line.toDouble).toString)
      .intersperse("\n")
      .through(text.utf8Encode)
      .through(io.file.writeAll(Paths.get("testdata/celsius.txt"), blockingEC))
  }

  def run(args: List[String]): IO[ExitCode] =
    processMessages.compile.drain.as(ExitCode.Success)
}
