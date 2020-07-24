package client

import java.net.InetSocketAddress

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode}
import cats.syntax.all._
import fs2.io.tcp.{Socket, SocketGroup}
import fs2.{Chunk, Stream}
import models.UnencryptedMessage

class Client[F[_]: Concurrent: ContextShift](msgs: List[UnencryptedMessage]) {
  def run: F[ExitCode] =
    Blocker[F]
      .use(SocketGroup[F](_).use(client(_).compile.drain))
      .as(ExitCode.Success)

  def client(
    socketGroup: SocketGroup
  ): Stream[F, UnencryptedMessage] =
    Stream
      .resource(socketGroup.client(new InetSocketAddress("localhost", 5555)))
      .flatMap(socketTest)

  def decode(response: Chunk[Byte]): UnencryptedMessage =
    UnencryptedMessage.codec.decode(response.toBitVector).require.value
  private def socketTest(socket: Socket[F]): Stream[F, UnencryptedMessage] =
    Stream
      .emits(msgs)
      .flatMap(toStream)
      .through(socket.writes())
      .drain
      .onFinalize(socket.endOfOutput) ++ socket.reads(8092).chunks.map(decode)

  private def toStream(msg: UnencryptedMessage) =
    Stream.chunk(Chunk.bytes(UnencryptedMessage.toBytes(msg)))
}
