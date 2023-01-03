package com.zio.acme.repo

import zio._
import zio.test._
import zio.test.TestAspect._
import java.time.LocalDate
import com.zio.acme.domain.Order
import zio.sql._
import java.util.UUID

object OrderRepoSpec extends  ZIOSpecDefault {

val testLayer = ZLayer.make[OrderRepository] (
    OrderRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec = 
    suite("order reporitory test") {
      ZIO.logLevel(LogLevel.Warning)
      test("find all") {
        for {
          repo <- ZIO.service[OrderRepository]
          cnt <- repo.findAll().runCount
        } yield assertTrue(cnt == 25)
      } +
      test("find by id") {
        val expected = Order(UUID.fromString("04912093-cc2e-46ac-b64c-1bd7bb7758c3"), 
              UUID.fromString("60b01fc9-c902-4468-8d49-3c0f989def37"),  LocalDate.of(2019, 3, 25))

        for {
          repo <- ZIO.service[OrderRepository]
          result <- repo.findById(expected.id)
        } yield assertTrue(result == expected)
      }
    }
    .provideLayerShared(testLayer.orDie) 
  }

