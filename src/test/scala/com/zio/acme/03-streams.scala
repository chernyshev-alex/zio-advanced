/**
 * STREAMS
 *
 * ZIO Streams is an optional module of ZIO that provides support for async,
 * concurrent, high-performance streams that are built upon and tightly
 * integrated with ZIO, including embracing typed errors and environment.
 *
 * Although FS2 and other streaming libraries can be used with ZIO, ZIO
 * Streams offers a compelling package in a small surface area and is
 * considered extremely vital to the future of the ZIO ecosystem.
 */
package com.zio.acme

import zio._
import zio.stream._
import zio.test._

import java.io.{FileReader, IOException}
import java.nio.file.{FileSystems, Path, Paths}
import scala.language.postfixOps

/**
 * ZIO Streams can be constructed from a huge number of other data types,
 * including iterables, ZIO effects, queues, hubs, input streams, and
 * much, much more.
 *
 * In this section, you'll explore a few of the common constructors.
 */
object SimpleConstructors extends ZIOSpecDefault {
  def spec =
    suite("SimpleConstructors") {
      /**
       * EXERCISE
       *
       * Use the `ZStream.apply` constructor to make a stream from literal
       * integer values to make the unit test succeed.
       */
      test("apply") {
        for {
          ref    <- Ref.make(0)
          stream = ZStream(5,10)
          _      <- stream.foreach(value => ref.update(_ + value))
          v      <- ref.get
        } yield assertTrue(v == 15)
      }  +
        /**
         * EXERCISE
         *
         * Use the `ZStream.fromIterable` constructor to make a stream from the
         * `iterable` value to make the unit test succeed.
         */
        test("fromIterable") {
          val iterable: Iterable[Int] = List(1, 2, 3, 4, 5)
          for {
            ref    <- Ref.make(0)
            stream = ZStream.fromIterable(iterable)
            _      <- stream.foreach(value => ref.update(_ + value))
            v      <- ref.get
          } yield assertTrue(v == 15)
        }  +
        /**
         * EXERCISE
         *
         * Use the `ZStream.fromQueue` constructor to make a stream from the
         * `queue` to make the unit test succeed.
         */
        test("fromQueue") {
          for {
            ref    <- Ref.make(0)
            queue  <- Queue.bounded[Int](100)
            _      <- (ZIO.foreach(0 to 100)(queue.offer(_)) *>
                            queue.size.repeatUntil(_ == 0) *> queue.shutdown).forkDaemon
            stream = ZStream.fromQueue(queue)
            _      <- stream.foreach(value => ref.update(_ + value))
            v      <- ref.get
          } yield assertTrue(v == 5050)
        }  +
        /**
         * EXERCISE
         *
         * Use the `ZStream.fromZIO` constructor to convert the provided effect
         * into a singleton stream.
         */
        test("fromZIO") {
          val effect = Console.readLine
          for {
            ref    <- Ref.make("")
            _      <- TestConsole.feedLines("a", "b", "c")
            stream = ZStream.fromZIO(effect)
            _      <- stream.foreach(value => ref.update(_ + value))
            v      <- ref.get
          } yield assertTrue(v == "a")
        } +
        /**
         * EXERCISE
         *
         * Use the `ZStream.fromFile` method to read the "build.sbt" file.
         * Ignore the machinery that has NOT been introduced yet, such as
         * transduce.
         */
        test("fromFile") {
          lazy val path   = FileSystems.getDefault().getPath("build.sbt")
          lazy val decode = ZPipeline.utf8Decode >>> ZPipeline.splitLines
          for {
            _     <- (ZStream.fromPath(path) >>> decode).foreach(Console.printLine(_))
            lines <- TestConsole.output
          } yield assertTrue(lines.exists(_.contains("zio-streams")))
        }
    }
}

object SimpleOperators extends ZIOSpecDefault {
  def spec =
    suite("SimpleOperators") {
      /**
       * EXERCISE
       *
       * Use `runCollect` on the provided stream to extract out all the values of the stream
       * and collect them into a `Chunk`.
       */
      test("runCollect") {
        val stream = ZStream(1, 2, 3, 4, 5)
        for {
          values <- stream.runCollect
        } yield assertTrue(values == Chunk(1, 2, 3, 4, 5))
      }  +
        /**
         * EXERCISE
         *
         * Insert a `take(2)` at the right place to take the first two elements
         * of the stream.
         */
        test("take") {
          for {
            values <- ZStream(1, 2, 3, 4).runCollect
          } yield assertTrue(values.take(2) == Chunk(1, 2))
        }  +
        /**
         * EXERCISE
         *
         * Insert a `takeWhile(_ < 3)` at the right place to take the first two
         * elements of the stream.
         */
        test("takeWhile") {
          for {
            values <- ZStream(1, 2, 3, 4).runCollect
          } yield assertTrue(values.takeWhile(_ < 3) == Chunk(1, 2))
        }  +
        /**
         * EXERCISE
         *
         * Insert a `drop(2)` at the right place to drop the first two elements
         * of the stream.
         */
        test("drop") {
          for {
            values <- ZStream(1, 2, 3, 4).runCollect
          } yield assertTrue(values.drop(2) == Chunk(3, 4))
        }  +
        /**
         * EXERCISE
         *
         * Insert a `dropWhile(_ < 3)` at the right place to drop the first
         * two elements of the stream.
         */
        test("dropWhile") {
          for {
            values <- ZStream(1, 2, 3, 4).runCollect
          } yield assertTrue(values.dropWhile(_ < 3) == Chunk(3, 4))
        }  +
        /**
         * EXERCISE
         *
         * Insert a `map(_ * 2)` at the right place.
         */
        test("map") {
          for {
            values <- ZStream(1, 2, 3).runCollect
          } yield assertTrue(values.map(_ * 2) == Chunk(2, 4, 6))
        }  +
        /**
         * EXERCISE
         *
         * Insert a `filter(_ % 2 == 0)` at the right place to filter out
         * all the odd numbers.
         */
        test("filter") {
          for {
            values <- ZStream(1, 2, 3, 4, 5, 6).runCollect
          } yield assertTrue(values.filter(_ % 2 == 0) == Chunk(2, 4, 6))
        } +
        /**
         * EXERCISE
         *
         * Insert a `.forever` call at the right place.
         */
        test("forever") {
          for {
            values <- ZStream(1).forever.take(5).runCollect
          } yield assertTrue(values == Chunk(1, 1, 1, 1, 1))
        }  +
        /**
         * EXERCISE
         *
         * Use `++` to concatenate the two streams together.
         */
        test("++") {
          val stream1 = ZStream(1, 2, 3)
          val stream2 = ZStream(4, 5, 6)
          for {
            values <- (stream1 ++ stream2).runCollect
          } yield assertTrue(values == Chunk(1, 2, 3, 4, 5, 6))
        }
    }
}

object RunningStreams extends ZIOSpecDefault {
  def spec =
    suite("RunningStreams") {
      /**
       * EXERCISE
       *
       * Use `.runHead` to pull out the head element of an infinite stream.
       */
      test("runHead") {
        val stream = ZStream("All work and no play makes Jack a dull boy").forever
        for {
          headOption <- stream.runHead
        } yield assertTrue(headOption == Some("All work and no play makes Jack a dull boy"))
      }  +
        /**
         * EXERCISE
         *
         * Use `.runDrain` to run a stream by draining all of its elements and
         * throwing them away (change the `.runHead`).
         */
        test("runDrain") {
          val stream = ZStream.fromIterable(0 to 1000)
          for {
            drained   <- Ref.make(false)
            _         <- (stream ++ ZStream.fromZIO(drained.set(true)).drain).runDrain  // runHead
            isDrained <- drained.get
          } yield assertTrue(isDrained)
        }  +
        /**
         * EXERCISE
         *
         * Use `.runCount` to drain the stream and count how many things were
         * emitted by the stream (change the `.runHead`).
         */
        test("runCount") {
          val stream = ZStream.fromIterable(0 to 100)
          for {
            count <- stream.runCount
          } yield assertTrue(count == 101)
        }  +
        /**
         * EXERCISE
         *
         * Use `.run` with the provided "sink" to count the number of things
         * that were emitted by the stream (change the `.runHead`).
         */
        test("run") {
          val stream = ZStream.fromIterable(0 to 100)
          for {
            count <- stream.run(ZSink.count)
          } yield assertTrue(count == 101)
        }  +
        /**
         * EXERCISE
         *
         * Use `.fold` to fold over the stream, summing all the elements.
         */
        test("fold") {
          val stream = ZStream.fromIterable(0 to 100)
          for {
            sum <- stream.runFold(0)((a, v) => a + v)
          } yield assertTrue(sum == 5050)
        }  +
        /**
         * EXERCISE
         *
         * Use `.foldZIO` to fold over a stream of questions, asking responses,
         * and aggregating them into a map.
         */
        test("foldZIO") {
          val expected = Map("What is your name?" -> "Sherlock Holmes", "What is your age?" -> "42")
          val questions = ZStream.fromIterable(expected.keys)
          for {
            _ <- TestConsole.feedLines(expected.values.toVector: _*)
            map <- questions.runFoldZIO(Map.empty[String, String])((m, question) =>
              Console.readLine(question).map(answer => m + (question -> answer)))
          } yield assertTrue(map == expected)
        }
    }
}

object AdvancedConstructors extends ZIOSpecDefault {
  def spec =
    suite("AdvancedConstructors") {
      /**
       * EXERCISE
       *
       * Use the ZStream.unfold constructor that can be used for statefully
       * "unfolding" a finite or infinite stream from an initial value.
       */
      test("unfold") {
        val fibs: ZStream[Any, Nothing, Int] = ZStream.unfold((0, 1)) { case (a, b) => Some((a, (b, a + b))) }
        for {
          values <- fibs.take(5).runCollect
        } yield assertTrue(values == Chunk(0, 1, 1, 2, 3))
      }  +
        /**
         * EXERCISE
         *
         * Use `ZStream.repeatZIO` to construct a stream whose elements are
         * constructed by repeatedly executing the `Console.readLine` effect.
         */
        test("repeatZIO") {
          val stream = ZStream.repeatZIO(Console.readLine)
          for {
            _      <- TestConsole.feedLines("Hello", "World")
            values <- stream.take(2).runCollect
          } yield assertTrue(values == Chunk("Hello", "World"))
        }  +
        /**
         * EXERCISE
         *
         * Use `ZStream.repeatZIOOption` to repeat `Console.readLine` until
         * the line "John" is read from the console.
         */
        test("repeatZIOOption") {
          val readUntilJohn =
            for {
              line <- Console.readLine.mapError(Some(_))
              _    <- ZIO.fail(None).when(line == "John")
            } yield line

          val stream = ZStream.repeatZIOOption(readUntilJohn)  // ZStream[String]()
          for {
            _      <- TestConsole.feedLines("Sherlock", "Holmes", "John", "Watson")
            values <- stream.runCollect
          } yield assertTrue(values == Chunk("Sherlock", "Holmes"))
        }
    }
}

object AdvancedOperators extends ZIOSpecDefault {
  def spec =
    suite("AdvancedOperators") {
      /**
       * EXERCISE
       *
       * Using `flatMap`, turn the provided stream into one where every element
       * is replicated 3 times.
       */
      test("flatMap") {
        val stream = ZStream(1, 2, 3)
        for {
          values <- stream.flatMap(e => ZStream(e,e,e)).runCollect
        } yield assertTrue(values == Chunk(1, 1, 1, 2, 2, 2, 3, 3, 3))
      } +
        /**
         * EXERCISE
         *
         * Insert a `.mapZIO` to print out each question using Console.printLine
         * and ask for a response using Console.readLine.
         */
        test("mapZIO") {
          val questions = ZStream("What is your name?", "What is your age?")
          for {
            _      <- TestConsole.feedLines("Sherlock Holmes", "42")
            values <- questions.mapZIO(q => Console.printLine(q) *> Console.readLine).runCollect
            lines  <- TestConsole.output
          } yield
            assertTrue(values == Chunk("Sherlock Holmes", "42")) &&
              assertTrue(lines == Vector("What is your name?\n", "What is your age?\n"))
        } +
        /**
         * EXERCISE
         *
         * Use `mapAccum` to keep track of word counts, emitting pairs of
         * words and their current running counts. Hint: Use a `Map[String, Int]`
         * as the state type for your `mapAccum`.
         */
        test("mapAccum") {
          val stream = ZStream("blue", "red", "blue", "red")

          def aggregate(stream: Stream[Nothing, String]): Stream[Nothing, (String, Int)] =
            stream.mapAccum(Map.empty[String, Int])((m, w) => {
               val t  = w -> (m.getOrElse(w, 0) + 1)
               (m + t, t) })

          for {
            tuple <- aggregate(stream).runLast.some
          } yield assertTrue(tuple == ("red", 2))
        }
    }
}

object BasicError extends ZIOSpecDefault {
  def spec =
    suite("BasicError") {
      /**
       * EXERCISE
       *
       * Use `ZStream.fail` to construct a stream that fails with the string
       * "Uh oh!".
       */
      test("fail") {
        for {
          value <- (ZStream.fail("Uh oh!")).runCollect.either
        } yield assertTrue(value == Left("Uh oh!"))
      }  +
        /**
         * EXERCISE
         *
         * Use `.catchAll` to catch the error and turn it into a singleton stream.
         */
        test("catchAll") {
          for {
            value <- ZStream.fail("Uh oh!").catchAll(err => ZStream(err)).runCollect
          } yield assertTrue(value == Chunk("Uh oh!"))
        }
    }
}

object TemporalStreams extends ZIOSpecDefault {
  def spec =
    suite("TemporalStreams") {
      /**
       * EXERCISE
       *
       * Use `ZStream.fromSchedule` to convert a schedule to a stream.
       */
      test("fromSchedule") {
        val schedule = Schedule.recurs(100)
        for {
          values <- ZStream.fromSchedule(schedule).runCollect
        } yield assertTrue(values.length == 100)
      }  +
        /**
         * EXERCISE
         *
         * Use `ZStream.repeatZIOWithSchedule` to repeat the provided effect
         * according to the provided schedule.
         */
        test("repeatZIOWithSchedule") {
          val effect   = Console.printLine("All work and no play makes Jack a dull boy")
          for {
            _     <- ZStream.repeatZIOWithSchedule(effect, Schedule.recurs(100)).runDrain
            lines <- TestConsole.output
          } yield assertTrue(lines.length == 101)
        }
    }
}

object ChunkedStreams extends ZIOSpecDefault {
  def spec =
    suite("ChunkedStreams") {
      /**
       * EXERCISE
       *
       * Use `.foreachChunk` in order to iterate through all the chunks which
       * are backing the ZStream.
       */
      test("foreachChunk") {
        val stream = ZStream.fromIterable(1 to 100)
        for {
          chunkCount <- Ref.make(0)
          _     <- stream.runForeachChunk(_ => chunkCount.update(_ + 1))
          v          <- chunkCount.get
        } yield assertTrue(v == 1)
      } +
        /**
         * EXERCISE
         *
         * Map over the chunks backing the provided stream with `mapChunks`,
         * reversing each of them.
         */
        test("mapChunks") {
          val stream = ZStream.fromChunks(Chunk(1), Chunk(2), Chunk(3, 4), Chunk(5, 6, 7, 8, 9))
          for {
            values <- stream.mapChunks(_.reverse).runCollect
          } yield assertTrue(values == Chunk(1, 2, 4, 3, 9, 8, 7, 6, 5))
        }  +
        /**
         * EXERCISE
         *
         * Provide a correct implementation of `chunked` that exposes the chunks
         * underlying a stream.
         */
        test("chunked") {
          val stream = ZStream.fromChunks(Chunk(1), Chunk(2), Chunk(3, 4), Chunk(5, 6, 7, 8, 9))

          def chunked[R, E, A](stream: ZStream[R, E, A]): ZStream[R, E, Chunk[A]] =
            stream.mapChunks(c => Chunk(c))

          for {
            chunks <- chunked(stream).runCollect
          } yield assertTrue(chunks.length == 4)
        } +
        /**
         * EXERCISE
         *
         * Implement a correct version of `unchunked` that hides the chunks
         * into the stream.
         */
        test("unchunked") {
          val stream = ZStream(Chunk(1), Chunk(2), Chunk(3, 4), Chunk(5, 6, 7, 8, 9))

          def unchunked[R, E, A](stream: ZStream[R, E, Chunk[A]]): ZStream[R, E, A] = stream.flattenChunks

          for {
            values1 <- unchunked(stream).runCollect
            values2 <- stream.flattenChunks.runCollect
          } yield assertTrue(values1 == values2)
        }
    }
}

/**
 * GRADUATION
 *
 * To graduate from this section, you will implement a command-line application
 * that uses ZIO Streams to perform "word counting" on a provided file.
 */
object Graduation03 extends zio.ZIOAppDefault {

  def splitFilePipeline = ZPipeline.utf8Decode >>> ZPipeline.splitOn(" ")

  def run = {
    for {
      file <- getArgs.map(_.headOption).someOrFail("Specify a file")
      map <- (ZStream.fromFileName(file) >>> splitFilePipeline).runFold(Map.empty[String, Int]) {
                      case (m, word) => m + (word -> (m.getOrElse(word, 0) + 1))
                  }.refineToOrDie[IOException]
      _   <- Console.printLine(map.mkString("", "\n", ""))
    } yield()
  }
}