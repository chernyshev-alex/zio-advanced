package com.zio.acme.repo

import zio._
import zio.test._
import zio.test.TestAspect._
import java.time.LocalDate
import com.zio.acme.domain._
import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase
import org.testcontainers.utility.DockerImageName
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Ports.Binding
import com.github.dockerjava.api.model.ExposedPort
import io.getquill.util.LoadConfig
import io.getquill

object CustomerRepoSpec extends  ZIOSpecDefault {

  def startDbContainer : ZIO[Any, Throwable, PostgreSQLContainer] = {
    ZIO.attemptBlocking { 
      val conf = getquill.util.LoadConfig("db-config2").getConfig("dataSource")
      var c = PostgreSQLContainer(
          dockerImageNameOverride = DockerImageName.parse("postgres:alpine"), 
          databaseName = conf.getString("databaseName"), 
          username = conf.getString("user"),
          password = conf.getString("password") 
      ).configure { a => 
          a.withInitScript("init.sql")
     } 
      c.start()
      c
    }
  }

  def stopDbContainer(container : PostgreSQLContainer) = 
      ZIO.attemptBlocking(container.stop()).orDie

  val testLayer = ZLayer.make[CustomerRepository] (
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("db-config2"),
      CustomerRepository.live)

  override def spec = 
    suite("customer reporitory test") {
      test("quill get customers") {
        for {
          ls <- CustomerRepository.getCustomers
        } yield assertTrue(ls.length == 5)
      }.provideLayerShared(testLayer) @@ aroundAllWith(startDbContainer)(stopDbContainer _)
  } 
}