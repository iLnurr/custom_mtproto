package client

import java.net.InetSocketAddress

import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode, Sync}
import cats.syntax.all._
import fs2.{Chunk, Stream}
import fs2.io.tcp.{Socket, SocketGroup}

object Client {
  def run[F[_]: Concurrent: ContextShift](
    msgs: Stream[F, Array[Byte]]
  ): F[ExitCode] =
    Blocker[F]
      .use { blocker =>
        SocketGroup[F](blocker).use { socketGroup =>
          client[F](socketGroup, msgs)
        }
      }
      .as(ExitCode.Success)

  def client[F[_]: Concurrent: ContextShift](
    socketGroup: SocketGroup,
    stream:      Stream[F, Array[Byte]]
  ): F[Unit] =
    socketGroup.client(new InetSocketAddress("localhost", 5555)).use { socket =>
      stream.map(msg => fire(socket, msg)).compile.drain
    }

  def fire[F[_]: Concurrent](socket: Socket[F], msg: Array[Byte]) =
    socket.write(Chunk.bytes(msg)) >>
    socket.read(8192).flatMap { response =>
      Sync[F].delay {
        println(s"Response: $response")
        response
      }
    }
}
