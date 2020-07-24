package server

import java.net.InetSocketAddress

import cats.effect.concurrent.MVar
import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode}
import cats.syntax.all._
import fs2.io.tcp.{Socket, SocketGroup}
import fs2.{Chunk, Pipe, Stream}
import models._
import scodec.stream.StreamDecoder

class ServerImpl[F[_]: Concurrent: ContextShift] {
  def start: F[ExitCode] =
    Blocker[F]
      .use(SocketGroup[F](_).use(server(_).compile.drain))
      .as(ExitCode.Success)

  def server(
    socketGroup: SocketGroup
  ): Stream[F, Unit] =
    socketGroup
      .server(new InetSocketAddress(5555))
      .map(Stream.resource(_).flatMap(handleClient))
      .parJoinUnbounded

  private def handleClient(socket: Socket[F]): Stream[F, Unit] =
    Stream
      .eval(MVar[F].empty[PQMessage])
      .flatMap(
        mvar => {
          socket
            .reads(8192)
            .through(inPipe)
            .map { message =>
              println(s"Accepted new msg: `$message`")
              message
            }
            .through(msgPipe(mvar))
            .map { response =>
              println(s"Response: `$response`")
              response
            }
            .through(outPipe)
            .through(socket.writes())
            .onFinalize(socket.endOfOutput)
        }
      )

  def inPipe: Pipe[F, Byte, UnencryptedMessage] =
    StreamDecoder.many(UnencryptedMessage.codec).toPipeByte
  def msgPipe(
    mvar: MVar[F, PQMessage]
  ): Pipe[F, UnencryptedMessage, UnencryptedMessage] = _.flatMap {
    case UnencryptedMessage(authKeyId, messageId, ReqPQ(nonce)) =>
      val res = ResPQ(nonce, System.currentTimeMillis(), "15", List(1L))
      Stream
        .eval(mvar.put(res))
        .flatMap(mvar => {
          println(mvar)
          Stream.emit(
            UnencryptedMessage(
              authKeyId,
              messageId,
              res
            )
          )
        })
    case UnencryptedMessage(
        authKeyId,
        messageId,
        r @ ReqDHParams(_, _, _, _, _, _)
        ) =>
      Stream
        .eval(mvar.take)
        .flatMap(
          mvar => {
            mvar match {
              case ResPQ(nonce, serverNonce, _, _)
                  if nonce == r.nonce && serverNonce == r.serverNonce =>
                Stream.emit(
                  UnencryptedMessage(
                    authKeyId,
                    messageId,
                    ResDHParamsOk(nonce, serverNonce)
                  )
                )
              case other =>
                println(s"Got broken msg $other")
                Stream.emit(
                  UnencryptedMessage(
                    authKeyId,
                    messageId,
                    ResDHParamsFail(r.nonce, r.serverNonce)
                  )
                )
            }
          }
        )

    case _ =>
      Stream.empty
  }
  def outPipe: Pipe[F, UnencryptedMessage, Byte] =
    _.flatMap(msg => Stream.chunk(Chunk.bytes(UnencryptedMessage.toBytes(msg))))

}
