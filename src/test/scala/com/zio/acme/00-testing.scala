
package com.zio.acme

import zio.test.TestAspect._
import zio.test.{assertTrue, _}
import zio.{ZIO, _}

/**
 * ASSERTIONS
 *
 * ZIO Test operates using assertions, which use macros to provide very
 * powerful error messages and reporting. Assertions produce values,
 * which compose using a variety of operators.
 */
object BasicAssertions extends ZIOSpecDefault {
  def spec = suite("BasicAssertions") {
    trait Building {
      def contents: String
    }
    object House extends Building {
      def contents = "bed, coffee pot, kitchen"
    }
    object Barn extends Building {
      def contents = "hay, goats, feed"
    }
    object Shed extends Building {
      def contents = "needle, broom"
    }

    val buildings = List(House, Barn, Shed)

    test("2 + 2 == 4") {

      /**
       * EXERCISE
       *
       * Using `assertTrue`, assert that 2 + 2 == 4.
       */
      assertTrue(2+2 == 4)
    } +
      test("sherlock misspelling") {
        /**
         * EXERCISE
         *
         * Examine the output of this failed test. Then fix the test so that it passes.
         */
        assertTrue("sherlock".contains("sherlock"))
      } +
      test("multiple assertions") {
        val string = "cannac"
        /**
         * EXERCISE
         *
         * Using the `&&` operator of `Assert`, verify the following properties
         * about `string`:
         *
         *  - The string is 6 letters in length
         *  - the string starts with "can"
         *  - the reverse of the string is equal to itself
         */
        assertTrue(string.length==6) && assertTrue(string.startsWith("can")) &&
            assertTrue(string.reverse == string)

      } +
      test("new test") { assertTrue(true) }
    /**
     * EXERCISE
     *
     * Using `+`, add another test to the suite, which you can create with
     * `test`, as above. This test should verify that the contents of one
     * of the buildings in `buildings` contains a `needle`.
     */
  }
}

/**
 * ZIO ASSERTIONS
 *
 * Most assertions in ZIO Test will be effectful, rather than pure. Using the
 * same syntax, ZIO lets you write effectful tests.
 */
object BasicAssertionsZIO extends ZIOSpecDefault {
  def spec = suite("BasicAssertionsZIO") {
    test("incrementing a ref") {
      /**
       * EXERCISE
       *
       * Using `assertTrue`, assert that incrementing a zero-valued ref by one
       * results in 1.
       */
      for {
        ref <- Ref.make(0)
        v   <- ref.updateAndGet(_ + 1)
      } yield assertTrue(v == 1)
    } +
      test("multiple assertions") {
        /**
         * EXERCISE
         *
         * Using the `&&` operator of `Assert`, verify the following properties
         * about `v`:
         *
         *  - It is an even number
         *  - It is greater than 0
         */
        for {
          ref  <- Ref.make(0)
          rand <- Random.nextIntBetween(1, 4)
          v    <- ref.updateAndGet(_ + 1).repeatN(rand * 2)
        } yield assertTrue(v > 0 && v % 2 > 0)
      }
  }
}

/**
 * TEST ASPECTS
 *
 * ZIO Test offers _test aspects_, which are values that allow modifying specs,
 * whether suites or individual tests. Test aspects are kind of like annotations,
 * except they are type-safe, non-magical, and first class values that can be
 * trasnformed and composed with other test aspects.
 *
 * Test aspects can add features like retrying tests, ignoring tests, running
 * tests only on a certain platform, and so forth.
 */
object BasicTestAspects extends ZIOSpecDefault {

  def spec = suite("BasicTestAspects") {
    test("ignore") {
      /**
       * EXERCISE
       *
       * Using `TestAspect.ignore`, add the `ignore` aspect to this test so that
       * the failure is ignored.
       */
      assertTrue(false)
    } @@ignore +
      test("flaky") {
        /**
         * EXERCISE
         *
         * Using `TestAspect.flaky`, mark this test as flaky so that it will pass so
         * long as it sometimes succeeds.
         */
        for {
          number <- Random.nextInt
        } yield assertTrue(number % 2 == 0)
      } @@flaky(4) +
      test("nonFlaky") {

        /**
         * EXERCISE
         *
         * Using `TestAspect.nonFlaky`, mark this test as non-flaky so that ZERO
         * failures are permitted.
         */
        for {
          number <- Random.nextIntBetween(0, 100)
        } yield assertTrue(number * 2 % 2 == 0)
      } @@nonFlaky(3) +
      /**
       * EXERCISE
       *
       * Add the `sequential` aspect to this suite and observe the change in
       * output to the console.
       */
      suite("sequential") {
        test("Test 1") {
          for {
            _ <- Live.live(ZIO.sleep(10.millis))
            _ <- Console.printLine("Test 1")
          } yield assertTrue(true)
        } @@sequential +
          test("Test 2") {
            for {
              _ <- Console.printLine("Test 2")
            } yield assertTrue(true)
          }
      }
  }
}

/**
 * TEST FIXTURES
 *
 * ZIO can execute arbitrary logic before, after, or before and after
 * tests individually, or all tests in a suite. This ability is sometimes
 * used for "test fixtures", which allow developers to perform custom
 * setup / tear down operations required for running tests.
 */
object TestFixtures extends ZIOSpecDefault {
  val beforeRef = new java.util.concurrent.atomic.AtomicInteger(0)
  val aroundRef = new java.util.concurrent.atomic.AtomicInteger(0)

  val incBeforeRef: UIO[Any] = ZIO.succeed(beforeRef.incrementAndGet())

  def spec = suite("TestFixtures") {
    /**
     * EXERCISE
     *
     * Using `TestAspect.before`, ensure the `incBeforeRef` effect is executed
     * prior to the start of the test.
     */
    test("before") {
      for {
        value <- ZIO.succeed(beforeRef.get)
      } yield assertTrue(value > 0)
    } @@ before {
      incBeforeRef.repeatN(1)
    } +
      /**
       * EXERCISE
       *
       * Using `TestAspect.after`, ensure the message `done with after` is printed
       * to the console using `ZIO.debug`.
       */
      test("after") {
        for {
          _ <- Console.printLine("after")
        } yield assertTrue(true)
      } @@ after {
        ZIO.debug("called after")
      } +
      /**
       * EXERCISE
       *
       * Using `TestAspect.around`, ensure the `aroundRef` is incremented before and
       * decremented after the test.
       */
      test("around") {
        for {
          value <- ZIO.succeed(aroundRef.get)
        } yield assertTrue(value == 1)
      } @@ around (ZIO.succeed(aroundRef.incrementAndGet()), ZIO.succeed(aroundRef.decrementAndGet() == 0))
  }
}

/**
 * TEST SERVICES
 *
 * By default, ZIO tests use test versions of all the standard services
 * baked into ZIO, including Random, Clock, System, and Console.
 * These allow you to programmatically control the services, such as
 * adjusting time, setting up fake environment variables, or inspecting
 * console output or providing console input.
 */
object TestServices extends ZIOSpecDefault {
  def spec =
    suite("TestServices") {
      /**
       * EXERCISE
       *
       * Using `TestClock.adjust`, ensure this test passes without timing out.
       */
      test("TestClock") {
        for {
          fiber <- Clock.sleep(1.second).as(42).fork
          _ <- TestClock.adjust(1.second)
          value <- fiber.join
        } yield assertTrue(value == 42)
      } +
        /**
         * EXERCISE
         *
         * Using `TestSystem.putEnv`, set an environment variable to make the
         * test pass.
         */
        test("TestSystem") {
          for {
            _ <- TestSystem.putEnv("name", "Sherlock Holmes")
            name <- System.env("name").some
          } yield assertTrue(name == "Sherlock Holmes")
        } +
        /**
         * EXERCISE
         *
         * Using `TestConsole.feedLines`, feed a name into the console such that
         * the following test passes.
         */
        test("TestConsole") {
          for {
            _    <- Console.printLine("What is your name?")
            _    <- TestConsole.feedLines("Sherlock Holmes")
            name <- Console.readLine
          } yield assertTrue(name == "Sherlock Holmes")
        } +
        /**
         * EXERCISE
         *
         * Using `TestRandom.feedInts`, feed the integer 5 into the Random
         * generator so the test will pass.
         */
        test("TestRandom") {
          for {
            _      <- TestRandom.feedInts(5)
            _      <- TestConsole.feedLines("5")
            number <- Random.nextInt
            _      <- Console.printLine("Guess a random number between 0 - 10: ")
            guess  <- Console.readLine
            result <- if (guess == number.toString) Console.printLine("Good job!").as(true)
            else Console.printLine("Try again!").as(false)
          } yield assertTrue(result)
        } +
        /**
         * EXERCISE
         *
         * Some times it is necessary to run code against a live standard
         * service, rather than one of the test services baked into ZIO Test.
         * A useful function for doing this is `Live.live`, which will ensure
         * the provided effect runs using the live services.
         */
        test("Live") {
          for {
            now <- Live.live(Clock.instant.map(_.getEpochSecond()))
          } yield assertTrue(now > 0)
        }
    }
}

/**
 * INTEGRATION/SYSTEM ASPECTS
 *
 * Some ZIO Test aspects are designed for more advanced integration and system
 * tests.
 */
object IntegrationSystem extends ZIOSpecDefault {
  /**
   * EXERCISE
   *
   * Explore jvmOnly, windows, linux, ifEnv, and other test aspects that
   * are useful for running platform-specific or integration / system tests.
   */
  def spec = suite("IntegrationSystem")(
  ) @@ jvmOnly
}

/**
 * CUSTOM LAYERS
 *
 * The code you are testing may use its own layers, to provide access to
 * other services required by your application. This is especially true
 * for business logic, which may be assembled from high-level layers
 * that allow expressing business logic in a direct style.
 *
 * ZIO Test allows you to provide custom layers in a variety of ways
 * to your tests.
 */
object CustomLayers extends ZIOSpecDefault {
  final case class User(id: String, name: String, age: Int)

  trait UserRepo {
    def getUserById(id: String): Task[Option[User]]
    def updateUser(user: User): Task[Unit]
  }
  object UserRepo {
    def getUserById(id: String): RIO[UserRepo, Option[User]] =
      ZIO.serviceWithZIO[UserRepo](_.getUserById(id))

    def updateUser(user: User): RIO[UserRepo, Unit] =
      ZIO.serviceWithZIO[UserRepo](_.updateUser(user))
  }

  final case class TestUserRepo(ref: Ref[Map[String, User]]) extends UserRepo {
    /**
     * EXERCISE
     *
     * Implement the following method of the user repo to operate on the
     * in-memory test data stored in the Ref.
     */
    def getUserById(id: String): Task[Option[User]] = ref.get.map(_.get(id))
    /**
     * EXERCISE
     *
     * Implement the following method of the user repo to operate on the
     * in-memory test data stored in the Ref.
     */
    def updateUser(user: User): Task[Unit] =
      ref.modify(m => (m, m.updated(user.id, user)))
  }

  /**
   * EXERCISE
   *
   * Create a test user repo layer and populate it with some test data.
   */
  lazy val testUserRepo: ULayer[UserRepo] = ZLayer {
    val u = User("sherlock@holmes.com", "sherlock", 42)
    Ref.make(Map[String, User](u.id -> u)).map(ref => TestUserRepo(ref))
  }

  def spec =
    suite("CustomLayers") {
      test("provideCustomLayer") {
        /**
         * EXERCISE
         *
         * In order to complete this exercise, you will have to make several
         * changes. First, use `UserRepo.getUserById` to retrieve the user
         * associated with the id. Then check the age is 42. To make the
         * test compile, you will have to `provideCustomLayer` on the test.
         * Finally, to make the test pass, you will have to create test
         * data matches your test expectations.
         */
         for {
           u <- UserRepo.getUserById("sherlock@holmes.com")
         } yield assertTrue(u.isDefined && u.get.age == 42)
      }.provideLayer(testUserRepo) +
        /**
         * EXERCISE
         *
         * Layers can be shared across all the tests in a suite.
         *
         * Use `provideCustomLayerShared` to provide a layer that is shared
         * across both of the following (sequentially executed) tests. Then
         * add a user in the first test that is then retrieved in the second.
         */
        suite("shared layer") {
          val user = User("id-0", "name", 42)
          test("adding a user") {
            for {
              _ <- UserRepo.updateUser(user)
              u <- UserRepo.getUserById(user.id)
            } yield assertTrue(u.isDefined && u.get.id == user.id)
          } +
          test("getting a user") {
            for {
              u <- UserRepo.getUserById(user.id)
            } yield assertTrue(u.isDefined)
          } @@ sequential
        }
    } provideLayerShared(testUserRepo)
}

/**
 * GRADUATION PROJECT
 *
 * To graduate from this section, you will choose and complete one of the
 * following projects under the assistance of the instructor:
 *
 * 1. Implement a custom `TestAspect` that provides features or functionality
 *    you might like to use in your own unit tests.
 *
 * 2. Design an `EmailService` for sending emails. Then create a test
 *    implementation that allows simulating failures and successes, and
 *    which captures sent emails for purposes of testing. Finally,
 *    create a layer for the test email service and use it in a test.
 *
 */
import zio.ZIO

object EmailService {
    trait EmailSender {
      def doSend(email : String, text : String) : Task[Unit]
    }

    def send(userEmail : String, content : String) :  ZIO[EmailSender, Throwable, Unit] =
        ZIO.serviceWithZIO[EmailSender](_.doSend(userEmail, content))

    final case class EmailSenderImpl() extends EmailSender {
      var failCounter = 0
      def doSend(email: String, text: String) : IO[Throwable, Unit] = {
        failCounter += 1  // simulating failures and successes
       // println(email, failCounter % 2 == 0)
       if (failCounter % 2 == 0) {
         ZIO.fail(new Exception(email))
       } else {
         ZIO.succeed(email)
       }
      }
    }

    lazy val live : ZLayer[Any, Nothing, EmailSender] = ZLayer {
      ZIO.succeed(EmailSenderImpl())
    }
}

object Graduation extends ZIOSpecDefault {
  def spec = suite("Graduation")(
    test("send email") {
      for {
        u <- ZIO.foreach((0 to 5).toList) { i =>
          val email = s"some-$i@email.com"
          EmailService.send(email, s"message $i")
            .as("SENT:" + email)    // success = email
            .catchAll(ex => ZIO.succeed("FAIL:"+ ex.getMessage))
        }
      } yield assertTrue(u.filter(s => s.startsWith("SENT")) ==
          List("SENT:some-0@email.com", "SENT:some-2@email.com", "SENT:some-4@email.com"))
    }.provideLayer(EmailService.live)
  )
}