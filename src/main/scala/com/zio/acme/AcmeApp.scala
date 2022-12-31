package com.zio.acme

import com.zio.acme.repo.{OrderRepository, OrderRepositoryImpl}
import zhttp.http.{Http, Request, Response}
import zhttp.service.Server
import zio.sql.{ConnectionPool, ConnectionPoolConfig}
import zio.{ULayer, ZIOAppDefault, ZLayer}

class AcmeHttpApp extends ZIOAppDefault {
  val connPoolConfigLive : ULayer[ConnectionPoolConfig] =
      ZLayer.succeed(ConnectionPoolConfig(url="https://posgtgresql/ins1", new java.util.Properties()))

  def run =  Server.start(8080, http = AcmeApp())
    .provide(
      connPoolConfigLive,
      ConnectionPool.live,
      OrderRepositoryImpl.live)
}

object AcmeApp {
  def apply(): Http[OrderRepository, Throwable, Request, Response] = Http.empty
}