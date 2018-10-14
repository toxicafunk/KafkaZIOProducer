package com

import scalaz.Monoid
import scalaz.zio.IO

package object eniro {
  implicit val ioMonoid = new Monoid[IO[Nothing, Unit]] {
    override def zero: IO[Nothing, Unit] = IO.unit

    override def append(f1: IO[Nothing, Unit], f2: => IO[Nothing, Unit]): IO[Nothing, Unit] = for {
      io1 <- f1
      io2 <- f2
    } yield io2
  }
}
