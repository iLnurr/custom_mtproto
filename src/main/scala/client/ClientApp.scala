package client

import java.util.concurrent.Executors

import cats.effect.IO
import models.{ReqDHParams, ReqPQ, UnencryptedMessage}

import scala.concurrent.ExecutionContext
import scala.util.Random

object ClientApp extends App {
  implicit val cs = IO.contextShift(
    ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  )
  val testMsgs = List(
    UnencryptedMessage(
      Random.nextLong(),
      System.currentTimeMillis(),
      ReqPQ(util.Random.nextLong())
    ),
    UnencryptedMessage(
      Random.nextLong(),
      System.currentTimeMillis(),
      ReqDHParams(
        util.Random.nextLong(),
        System.currentTimeMillis(),
        "test",
        "test",
        1L,
        "test"
      )
    )
  )
  new Client[IO](testMsgs).run.unsafeRunSync()
}
