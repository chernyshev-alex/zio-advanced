package com.zio.acme.repo

import zio._
import zio.test._
import zio.test.TestAspect._
import java.time.LocalDate
import com.zio.acme.domain.Order
import zio.sql._
import java.util.UUID
import com.zio.acme.domain.DomainError

object OrderRepoSpec extends  ZIOSpecDefault {

val testLayer = ZLayer.make[OrderRepository] (
    OrderRepositoryImpl.live,
    PostgresContainer.connectionPoolConfigLayer,
    ConnectionPool.live,
    PostgresContainer.createContainer
  )

  override def spec = 
    suite("order repository test") {
      
      test(" all") {
        for {
          cnt <- OrderRepository.findAll().runCount
        } yield assertTrue(cnt == 25)
      } +
      test("find by id when exists") {
        val expected = Order(UUID.fromString("04912093-cc2e-46ac-b64c-1bd7bb7758c3"), 
              UUID.fromString("60b01fc9-c902-4468-8d49-3c0f989def37"),  LocalDate.of(2019, 3, 25))
        for {
          result <- OrderRepository.findById(expected.id)
        } yield assertTrue(result == expected)
      } +
      test("find by id when not exists") {
        for {
         result <- OrderRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000000")).either
        } yield assertTrue(result.fold(_.isInstanceOf[DomainError.RepositoryError], _ => false))
      } + 
      test("add one order") {
        val expected = Order(UUID.fromString("00000000-0000-0000-0000-000000000001"), 
              UUID.fromString("00000000-0000-0000-0000-000000000001"),  LocalDate.of(2019, 3, 25))
        for {
          res <- OrderRepository.add(expected)
        } yield assertTrue(res == 1)
      }

   }.provideLayerShared(testLayer.orDie) 
  }

// TODO  doobie impl
// https://medium.com/@wiemzin/zio-with-http4s-and-doobie-952fba51d089
// https://medium.com/@wiemzin/zio-with-http4s-and-doobie-952fba51d