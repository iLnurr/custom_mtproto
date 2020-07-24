package test

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, IO}
import client.Client
import fs2.Stream
import fs2.io.tcp.SocketGroup
import server.ServerImpl
import models.PQMessage.pqInnerDataCodec
import models._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class ServerSpec
    extends AnyFlatSpec
    with should.Matchers
    with BeforeAndAfterAll {
  def mkSocketGroup(implicit cs: ContextShift[IO]): Stream[IO, SocketGroup] =
    Stream.resource(Blocker[IO].flatMap(blocker => SocketGroup[IO](blocker)))

  implicit val cs: ContextShift[IO]     = IO.contextShift(ExecutionContext.global)
  implicit val ce: ConcurrentEffect[IO] = IO.ioConcurrentEffect(cs)

  val timeout: FiniteDuration = 30.seconds
  val nonce       = util.Random.nextLong()
  val serverNonce = System.currentTimeMillis()
  val pqInnerData = PqInnerData(
    pq          = "15",
    p           = "5",
    q           = "3",
    nonce       = nonce,
    serverNonce = serverNonce,
    newNonce    = util.Random.nextLong()
  )
  val ser = pqInnerDataCodec.encode(pqInnerData).require
  val encryptedPqInnerData = EncyptedPqInnerData(
    newNonce      = pqInnerData.newNonce,
    data          = ser.toString(),
    dataWithHash  = ser.toString(),
    encryptedData = ser.toString()
  )
  val testMsgs = List(
    UnencryptedMessage(
      Random.nextLong(),
      System.currentTimeMillis(),
      ReqPQ(util.Random.nextLong())
    ),
    UnencryptedMessage(
      nonce,
      serverNonce,
      ReqDHParams(
        util.Random.nextLong(),
        System.currentTimeMillis(),
        "3",
        "5",
        1L,
        encryptedPqInnerData.toString
      )
    )
  )

  val testServer: SocketGroup => Stream[IO, Unit] = socketGroup =>
    new ServerImpl[IO].server(socketGroup)

  val testClient: SocketGroup => Stream[IO, UnencryptedMessage] = sg =>
    new Client[IO](testMsgs).client(sg)

  "Server" should "process client msgs" in {
    mkSocketGroup
      .flatMap { socketGroup =>
        Stream(testServer(socketGroup).drain, testClient(socketGroup))
          .parJoin(2)
          .take(2)
      }
      .compile
      .toVector
      .unsafeRunTimed(timeout)
      .get
  }
}
