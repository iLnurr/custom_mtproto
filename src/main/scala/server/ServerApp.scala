package server

import java.util.concurrent.Executors

import cats.effect.IO

import scala.concurrent.ExecutionContext

object ServerApp extends App {
  implicit val cs = IO.contextShift(
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  )
  new ServerImpl[IO].start.unsafeRunSync()
}
