package com.zio.acme

import com.zio.acme.RuntimeSpec.unsafe
import zio._
import zio.test.{ZIOSpecDefault, assertTrue}

import scala.language.postfixOps

object ZServices extends ZIOSpecDefault {
  // raw external service
  class ExtService(name : String, sleep : Long = 0, _cnt : Int = 0) {
    var cnt  = 0
    def call(url: String) = {
      Thread.sleep(sleep)
      cnt += 1
      if (cnt < _cnt) {
        println(s"IOException on $name ${Thread.currentThread().getName}")
        throw new java.io.IOException("didn't get response")
      }
      name
    }
  }

  // ZIO wrappers
  object ExtService {
    val live : ZLayer[Any, Throwable, ExtService] = ZLayer.succeed(new ExtService("", 0, 0))
  }

  final class ZWorkflow(fast : ExtService, slow : ExtService, someFail : ExtService) {
    val policy = Schedule.exponential(10.milliseconds) >>> Schedule.recurs(10)

    def pool() : Task[String] =
      for {
        a <- ZIO.attemptBlocking(fast.call("s1")).retry(policy).fork
        b <- ZIO.attemptBlocking(slow.call("s2")).retry(policy).fork
        c <- ZIO.attemptBlocking(someFail.call("s3")).retry(policy).fork
        a <- a.join
        b <- b.join
        c <- c.join
      } yield a + b + c
  }

  object ZWorkflow {
    def pool(): ZIO[ZWorkflow, Throwable, String] = ZIO.serviceWithZIO[ZWorkflow](_.pool())
    val live: ZLayer[ExtService, Throwable, ZWorkflow] =
      ZLayer {
        for {
          fast <- ZIO.serviceWith[ExtService](_ => new ExtService( "01", 100, 10))
          slow <- ZIO.serviceWith[ExtService](_ => new ExtService( "02", 200, 10))
          someFail <- ZIO.serviceWith[ExtService](_ => new ExtService( "03", 50, 10))
        } yield new ZWorkflow(fast, slow, someFail)
      }
  }

  val appLayer : ZLayer[Any,Throwable, ZWorkflow] = ZLayer.make[ZWorkflow](ZWorkflow.live, ExtService.live)

  def spec = suite("DI") {
    test("Service Pattern") {
      val app : ZIO[Any, Throwable, String] = {
        for {
          resp <- ZWorkflow.pool()
        } yield resp
      }.provide(appLayer)

      val result = runtime.unsafe.run(app).getOrThrow()
      assertTrue("010203" == result)
    }
  }
}

object ZActor extends ZIOSpecDefault {
  // acts like Akka actor
  def actor[In](receive: In => UIO[Unit]) : ZIO[Scope, Nothing, In => UIO[Boolean]] = {
    ZIO.acquireRelease {
      for {
        q <- Queue.unbounded[In]
        fbr <- q.take.flatMap(receive).forever.fork
      } yield ((m: In) => q.offer(m), fbr)
    } (_._2.join).map(_._1)
  }

  def spec = suite("Actor") {
    test("Send/Receive") {
      val app = ZIO.scoped {
        for {
          receive <- ZIO.succeed((n : Int) => ZIO.debug(s"received $n").delay(1.milliseconds))
          sendTo <- actor[Int](receive)
          _ <- ZIO.foreachParDiscard(1 to 100)(sendTo(_))
          _ <- ZIO.debug("All messages were sent")
        } yield ()
      }
      runtime.unsafe.run(app).getOrThrow()
      assertTrue(false)
    }
  }
}
