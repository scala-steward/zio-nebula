package nebula4scala.zio

import scala.util.Try

import com.vesoft.nebula.client.graph.{ NebulaPoolConfig => _ }
import com.vesoft.nebula.client.graph.net.{ NebulaPool => Pool }

import _root_.zio._
import nebula4scala.Effect
import nebula4scala.api._
import nebula4scala.data._
import nebula4scala.impl.NebulaClientDefault
import nebula4scala.zio.syntax._

object NebulaClient {

  private final class Impl(underlying: NebulaClient[Try]) extends NebulaClient[Task] {

    def init(): Task[Boolean] =
      implicitly[Effect[Task]].fromTry(underlying.init())

    def close(): Task[Unit] = implicitly[Effect[Task]].fromTry(underlying.close())

    def getSession(useSpace: Boolean = false): Task[NebulaSession[Task]] =
      implicitly[Effect[Task]]
        .fromTry(underlying.getSession(useSpace))
        .map(s => new NebulaSessionImpl(s))

    def activeConnNum: Task[Int] = implicitly[Effect[Task]].fromTry(underlying.activeConnNum)

    def idleConnNum: Task[Int] = implicitly[Effect[Task]].fromTry(underlying.idleConnNum)

    def waitersNum: Task[Int] = implicitly[Effect[Task]].fromTry(underlying.waitersNum)

  }

  val layer: ZLayer[Scope & NebulaClientConfig, Throwable, NebulaClient[Task]] =
    ZLayer.fromZIO(
      for {
        config <- ZIO.service[NebulaClientConfig]
        cb <- ZIO.acquireRelease(ZIO.attempt(new Impl(new NebulaClientDefault(config, new Pool))))(p =>
          p.close().onError(e => ZIO.logErrorCause(e)).ignoreLogged
        )
      } yield cb
    )
}
