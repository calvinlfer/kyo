package kyo

import kyo._
import kyo.ios._
import kyo.sums._
import kyo.envs._
import kyo.tries._
import kyo.aborts._
import kyo.server._
import kyo.concurrent.fibers._
import kyo.concurrent.timers._

import sttp.tapir._
import sttp.tapir.server.ServerEndpoint
import kyo.App.Effects
import kyo.internal.KyoSttpMonad
import kyo.internal.KyoSttpMonad._

object routes {

  type Route[+T] = ServerEndpoint[Any, KyoSttpMonad.M]

  type Routes = Sums[List[Route[Any]]] with Fibers with IOs

  object Routes {

    private val sums = Sums[List[Route[Any]]]

    def run[T, S](v: Unit > (Routes with S)): NettyKyoServerBinding > (Fibers with IOs with S) =
      run[T, S](NettyKyoServer())(v)

    def run[T, S](server: NettyKyoServer)(v: Unit > (Routes with S))
        : NettyKyoServerBinding > (Fibers with IOs with S) =
      sums.run[NettyKyoServerBinding, Fibers with IOs with S] {
        v.andThen(sums.get.map(server.addEndpoints(_)).map(_.start()))
      }

    def add[T, U, E, S](e: Endpoint[Unit, T, Unit, U, Unit])(
        f: T => U > (Fibers with IOs)
    ): Unit > Routes =
      sums.add(List(
          e.serverLogic[KyoSttpMonad.M](f(_).map(Right(_))).asInstanceOf[Route[Any]]
      )).unit

    def add[T, U, S](
        e: PublicEndpoint[Unit, Unit, Unit, Any] => Endpoint[Unit, T, Unit, U, Any]
    )(
        f: T => U > (Fibers with IOs)
    ): Unit > Routes =
      add(e(endpoint))(f)
  }
}