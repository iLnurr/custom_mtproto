package test

import cats.Applicative
import cats.effect.{ContextShift, IO}
import client.Client
import models.{ReqPQ, UnencryptedMessage}
import server.Server

import scala.concurrent.ExecutionContext

object TestApp extends App {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  val program = for {
    server <- Server.run[IO]
    client <- Client.run(
      fs2.Stream.eval(
        Applicative[IO].pure(
          UnencryptedMessage.toBytes(UnencryptedMessage(message = ReqPQ(1L)))
        )
      )
    )
  } yield {
    println(s"Server code $server")
    println(s"Client code $client")
  }

  program.unsafeRunSync()

}
