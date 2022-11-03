package com.zio.acme

import zhttp.http._
import zhttp.service._
import zio._
import zio.json._
import scala.collection.mutable

  object WSApp extends ZIOAppDefault {
    def run :  ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
      Server.start(8080, http = UserApp())
        .provide(
          InMemoryUserRepo.layer    // add persistence layer
        )
  }

  //
  // Users HTTP Controller
  //
  object UserApp {
    def apply(): Http[UserRepo, Throwable, Request, Response] = Http.collectZIO[Request] {
        // POST /users -d '{"name": "John", "age": 35}'
        case req @ Method.POST -> !! / "users" =>
          for {
            u <- req.bodyAsString.map(_.fromJson[User])
            r <- u match {
              case Left(e) =>
                ZIO.debug(s"Failed to parse the input: $e").as(
                  Response.text(e).setStatus(Status.BadRequest))
              case Right(u) =>
                UserRepo.register(u).map(id => Response.text(id))
            }
          } yield r

        // GET /users/:id
        case Method.GET -> !! / "users" / id =>
        UserRepo.lookup(id).map {
            case Some(user) => Response.json(user.toJson)
            case None => Response.status(Status.NotFound)
          }
        // GET /users
        case Method.GET -> !! / "users" => UserRepo.users.map(user => Response.json(user.toJson))
      }
  }

  //
  // Domain
  //
  case class User(name: String, age: Int)
  object User {
    implicit val encoder : JsonEncoder[User] = DeriveJsonEncoder.gen[User]
    implicit val decoder : JsonDecoder[User] = DeriveJsonDecoder.gen[User]
  }

  //
  // Users Repository API
  //
  trait UserRepo {
    def register(user: User): Task[String]
    def lookup(id: String): Task[Option[User]]
    def users: Task[List[User]]
  }

  object UserRepo {
    def register(user: User): ZIO[UserRepo, Throwable, String] = ZIO.serviceWithZIO[UserRepo](_.register(user))
    def lookup(id: String): ZIO[UserRepo, Throwable, Option[User]] = ZIO.serviceWithZIO[UserRepo](_.lookup(id))
    def users: ZIO[UserRepo, Throwable, List[User]] =  ZIO.serviceWithZIO[UserRepo](_.users)
  }

  //
  // In memory Users Repository API impl
  //
  case class InMemoryUserRepo(map : Ref[mutable.Map[String, User]]) extends UserRepo {
    override def register(user: User): UIO[String] = for {
      id <- Random.nextUUID.map(_.toString)
      _ <- map.updateAndGet(_ addOne(id, user))
    } yield id

    override def lookup(id: String): UIO[Option[User]] = map.get.map(_.get(id))
    override def users: UIO[List[User]] = map.get.map(_.values.toList)
  }

  //
  // In memory Users Repository layer factory
  //
  object InMemoryUserRepo {
    def layer: ZLayer[Any, Nothing, InMemoryUserRepo] = ZLayer.fromZIO(
      Ref.make(mutable.Map.empty[String, User]).map(new InMemoryUserRepo(_))
    )
  }

