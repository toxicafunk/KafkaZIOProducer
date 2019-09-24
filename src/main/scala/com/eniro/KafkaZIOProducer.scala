package com.eniro

import cakesolutions.kafka.KafkaProducer.Conf
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import scalaz.zio.blocking._
import scalaz.zio.{blocking => _, _}

import scala.io.Source

object FileReader {

  trait Service {
    def readFile(file: String): ZIO[Blocking, Throwable, Iterator[String]]
  }

}

trait FileReader {
  val reader: FileReader.Service
}

object fileReader {
  def readFile(file: String): ZIO[FileReader with Blocking, Throwable, Iterator[String]] =
    ZIO.accessM(_.reader.readFile(file))
}

trait FileReaderLive extends FileReader {
  val reader: FileReader.Service = new FileReader.Service {
    override def readFile(file: String): ZIO[Blocking, Throwable, Iterator[String]] =
      blocking(IO.effect(Source.fromFile(file).getLines()))
  }
}

object FileReaderLive extends FileReaderLive with Blocking.Live

object Messaging {

  trait Service {
    def send(msg: String): Task[Unit]
  }

}

trait Messaging {
  val messenger: Messaging.Service
}

trait MessagingLive extends Messaging {
  val producer = KafkaProducer(
    Conf(new StringSerializer(), new StringSerializer(), lingerMs = 0,
      bootstrapServers = "172.18.0.2:9092,172.18.0.4:9092,172.18.0.5:9092")
  )

  val extractKey: String => String = (line: String) => {
    val start = line.indexOf(":")
    val end = line.indexOf(",")
    line.substring(start + 2, end - 1)
  }

  val messenger: Messaging.Service = new Messaging.Service {
    override def send(msg: String): Task[Unit] = {
      val record = KafkaProducerRecord("julio.genio.stream", Some(extractKey(msg)), msg)
      ZIO.fromFuture(implicit ec => producer.send(record).map(_ => ()))
    }
  }
}
object messaging {
  def send(msg: String): ZIO[Messaging, Throwable, Unit] =
    ZIO.accessM(_.messenger.send(msg))
}

object MessagingLive extends MessagingLive

object KafkaZIOProducer extends DefaultRuntime {

  import scalaz.zio.internal.PlatformLive

  val myRuntime =
    Runtime(new MessagingLive with FileReaderLive with Blocking.Live, PlatformLive.Default)

  val program = (filename: String) => for {
    it <- fileReader.readFile(filename)
    li <- ZIO.foreachParN(5000)(it.toList)(messaging.send)
  } yield li

  def main(args: Array[String]): Unit = {
    val filename = args(0)
    val t0 = System.currentTimeMillis()
    myRuntime.unsafeRun(program(filename))
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + (t1 - t0) + "ms")
  }

}
