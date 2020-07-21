package server

import java.net.InetSocketAddress

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode}
import fs2.Chunk
import fs2.io.tcp.SocketGroup
import fs2.Stream
import models._
import scodec.stream._
import cats.syntax.all._

object Server {
  implicit val messageCodec = UnencryptedMessage.variableSizeBitsCodes
  def run[F[_]: Concurrent: ContextShift]: F[ExitCode] =
    Blocker[F]
      .use { blocker =>
        SocketGroup[F](blocker).use { socketGroup =>
          server[F](socketGroup)
        }
      }
      .as(ExitCode.Success)

  def server[F[_]: Concurrent: ContextShift](
    socketGroup: SocketGroup
  ): F[Unit] =
    socketGroup
      .server(new InetSocketAddress(5555))
      .map { clientResource =>
        Stream.resource(clientResource).flatMap { socket =>
          socket
            .reads(8192)
            .through(StreamDecoder.many(messageCodec).toPipeByte)
            .evalMap {
              case UnencryptedMessage(messageId, ReqPQ(nonce)) =>
                println(
                  s"Server: Got request pq message with message Id : $messageId and nonce : $nonce"
                )
                println("Server: Generating server nonce.")
                val serverNonce = 111L
                println(s"Server: Server nonce is $serverNonce.")
                socket.write(
                  bytes = Chunk.bytes(
                    UnencryptedMessage.toBytes(
                      UnencryptedMessage(message = ResPQ(nonce, serverNonce))
                    )
                  )
                )
              case UnencryptedMessage(
                  messageId,
                  ReqDHParams(nonce, serverNonce)
                  ) =>
                println(
                  s"Server: Got request dh params message with message Id : " +
                  s"$messageId and nonce : $nonce and serverNonce : $serverNonce"
                )
                socket.close
              case other =>
                println(s"Server: got nothing interesting. $other")
                socket.close
            }

        }
      }
      .parJoin(100)
      .compile
      .drain

}
