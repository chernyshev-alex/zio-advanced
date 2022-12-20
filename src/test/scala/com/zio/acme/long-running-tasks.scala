package com.zio.acme

import zio.test._
import zio.{Task, _}

import scala.language.postfixOps

object GraduationLayer extends ZIOSpecDefault {
  case class A(a :  Int)
  object A {
    val layer : ZLayer[Any, Nothing, A] = ZLayer.scoped {
      ZIO.acquireRelease(acquire = ZIO.debug("Init A") *>
        ZIO.succeed(A(5)))(release = _ => ZIO.debug("Release A"))
    }
  }

  def spec =
    suite("s1")(
      test("t1") {
          (for { a <- ZIO.serviceWith[A](_.a) } yield a * a).provide(A.layer)
            .map(v => assertTrue(v == 25))
      } +
        test("t2") {
          ZIO.serviceWith[A](_.a).map(a => a * a).provide(A.layer)
            .map(v => assertTrue(v == 25))
        }
    )
}

object Tests extends ZIOSpecDefault {
  def spec =
    suite("tests") {
      test("test1") {
        def task1(name : String, cnt : Int) : Task[Unit] = {
          var n = cnt
          ZIO.whileLoop(n > 0)(ZIO.unit) { _ =>
            n = n -1
            println(s"$name $n")
            ZIO.sleep(100 milliseconds)
          }
        }

        def task3(name : String, cnt : Int) : Task[Unit] = {
          var n = cnt
          ZIO.whileLoop(n > 0)(ZIO.unit) { _ =>
            ZIO.sleep(50 milliseconds)
            println(s"$name $n")
            n = n -1
          }
        }

        def task2(name : String, cnt : Int) = {
          ZIO.iterate(cnt)(_ >=0) { n =>
            println(s"$name $n")
            ZIO.succeed(n - 1)
          }
        }

        def runSomePeriodicJob = ZIO.attempt(println("running job..."))

        for {
          _ <- Console.printLine("started")
           fm <- runSomePeriodicJob.repeat(Schedule.once).fork //spaced(200.milliseconds)).fork
           f1 <- task1("task1", 100).fork
           f2 <- task2("task2", 100).fork
           f3 <- task3("task3", 100).fork
          _ <- TestClock.adjust(5.seconds)
          _ <- (f1 <*> f2 <*> f3).join
          _ <- fm.join
          _ <- Console.printLine("ended")
        } yield assertTrue(1 == 1)
      }
    }
}