package com.zio.acme
import zio._
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

final case class ServiceA(b : ServiceB, c : ServiceC, log : Logger) {
  def run : Task[Unit] = {
    b.run *> c.run *> log.warn("In Service A")
  }
}
final case class ServiceB(cache : CacheService) {
  def run : Task[Unit] = {
    Console.printLine("In Service B") <* cache.run
  }
}
final case class ServiceC(cache : CacheService) {
  def run : Task[Unit] = Console.printLine("In Service C") <* cache.run
}
final case class CacheService(log : Logger) {
  def run : Task[Unit] = {
    log.warn("called CacheService") <*
      log.err(new Exception("some exception"))
  }
}
final case class Logger() {
  def warn(msg : String) : Task[Unit] = Console.printLine(msg)
  def err(ex : Exception) : Task[Unit] = Console.printLine(ex.getMessage)
}

object ServiceA {
  def run() : ZIO[ServiceA, Throwable, Unit] = ZIO.serviceWithZIO[ServiceA](_.run)
  val live = ZLayer {
    for {
        b <- ZIO.service[ServiceB]
        c <- ZIO.service[ServiceC]
        log <- ZIO.service[Logger]
    } yield ServiceA(b, c, log)
  }
}
object ServiceB {
  def run() : ZIO[ServiceB, Throwable, Unit] = ZIO.serviceWithZIO[ServiceB](_.run)
  val live = ZLayer { ZIO.service[CacheService].map(ServiceB(_)) }
}
object ServiceC {
  def run() : ZIO[ServiceC, Throwable, Unit] = ZIO.serviceWithZIO[ServiceC](_.run)
  val live = ZLayer { ZIO.service[CacheService].map(ServiceC(_)) }
}
object CacheService {
  def run() : ZIO[CacheService, Throwable, Unit] = ZIO.serviceWithZIO[CacheService](_.run)
  val live = ZLayer { ZIO.service[Logger].map(CacheService(_))  }
}
object Logger {
  val live = ZLayer { ZIO.succeed(new Logger()) }
}

object BasicAppSpec extends  ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = {
    test("Launch") {
      for {
        _ <- ServiceA.run()
      } yield
        assertTrue(true)
    }.provide(
      ServiceA.live,
      ServiceB.live,
      ServiceC.live,
      CacheService.live,
      Logger.live
    )
  }
}
