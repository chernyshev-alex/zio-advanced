package com.zio.acme

import zio._
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object UserRepoSpec extends  ZIOSpecDefault  {
  final case class User(id: String, name: String, age: Int)

  // service definition
  trait UserRepository {
    def getUserById(id: String): ZIO[Any, Throwable, Option[User]]
  }

  // service dependencies
  trait UserStorage {
    def getUserById(id: String): ZIO[Any, Throwable, Option[User]]
  }

  // service impl
  final case class UserRepositoryImpl(storage : UserStorage) extends UserRepository {
    override def getUserById(id: String): ZIO[Any, Throwable, Option[User]] =
      storage.getUserById(id)
  }

  // ZLayer constructor
  object UserRepositoryImpl {
    val layer : ZLayer[UserStorage, Nothing, UserRepository] = ZLayer {
        for {
          storage <- ZIO.service[UserStorage]
        } yield UserRepositoryImpl(storage)
    }
  }

  // Accessor Methods
  object UserRepository {
    def getUserById(id: String): ZIO[UserRepository, Throwable, Option[User]] =
      ZIO.serviceWithZIO[UserRepository](_.getUserById(id))
  }

  final case class MapUserStorage(db : Map[String, User]) extends UserStorage {
    override def getUserById(id: String): ZIO[Any, Throwable, Option[User]] =
      ZIO.succeed(db.get(id))
  }

  // storage impl
  object InMemoryBlobStorage {
    val layer = ZLayer {
      ZIO.succeed(MapUserStorage(Map.empty[String, User]))
     }
  }

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("User Repo")(
      test("getUserById") {
        for {
          u <- UserRepository.getUserById("11")
        } yield assertTrue(u.isEmpty)
      }.provide(UserRepositoryImpl.layer, InMemoryBlobStorage.layer)
    )

  // https://zio.dev/reference/service-pattern/
  // https://softwaremill.com/structuring-zio-2-applications/#example-zio-2-application
  // https://github.com/jdegoes/advanced-zio/blob/main/src/main/scala/net/zio/00-testing.scala
  // https://softwaremill.com/zio-environment-episode-3/
  // https://github.com/adamw/zioenv/tree/zio2/core/src/main/scala/zioenv
  // https://github.com/softwaremill/zio2-structure/blob/master/core/src/main/scala/Main.scala

}
