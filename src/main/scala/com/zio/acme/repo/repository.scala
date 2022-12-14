package com.zio.acme.repo

import com.zio.acme.domain.DomainError._
import com.zio.acme.domain._
import zio._
import zio.prelude.EqualOps
import zio.sql.ConnectionPool
import zio.stream.{ZStream, _}

import java.time.LocalDate
import java.util.UUID

trait OrderRepository {
  def findById(id : UUID) : IO[RepositoryError, Order]
  def findAll() : ZStream[Any, Exception, Order]
  def add(order : Order) : IO[RepositoryError, Int]
  def add(order : Seq[Order]) : IO[RepositoryError, Int]
}

object OrderRepository {
  def findById(id : UUID) : ZIO[OrderRepository, RepositoryError, Order] =
    ZIO.serviceWithZIO[OrderRepository](_.findById(id))

  def findAll() : ZStream[OrderRepository, Exception, Order] =
    ZStream.serviceWithStream[OrderRepository](_.findAll())

  def add(order : Order) : ZIO[OrderRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[OrderRepository](_.add(order))
}

// Impl ==================

final case class OrderRepositoryImpl(connectionPool : ConnectionPool)
  extends OrderRepository with PqTablesDescription { 

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(connectionPool))

  override def findById(_id: UUID): IO[RepositoryError, Order] = {
    val qOrdersById = select(id, customerId, orderDate)
      .from(orders)
      .where(id === _id)

    val stream: ZStream[SqlDriver, Exception, Order] = execute[Order](qOrdersById.to((Order.apply _).tupled))
    stream.runHead.some
      .tap(s => ZIO.logInfo(s"${s} SQL : ${renderRead(qOrdersById)}"))
      .tapError {
        case None => ZIO.unit
        case Some(e) => ZIO.logError(e.getMessage())
      }.mapError {
        case None => RepositoryError(new RuntimeException("Order does not exists"))
        case Some(e) => RepositoryError(e.getCause())
      }.provideLayer(driverLayer)
  }

  override def findAll(): ZStream[Any, Exception, Order] = {
    val sql = select(id, customerId, orderDate)
      .from(orders)

    execute[Order](sql.to((Order.apply _).tupled))
      .provideLayer(driverLayer)
  }

  override def add(order: Order): IO[RepositoryError, Int] = {
    val sql = insertInto(orders)(id, customerId, orderDate).values(order)
    
    ZIO.logInfo(s"Insert order query is ${renderInsert(sql)}") *> 
      execute[Order](sql)
        .tapError(e => ZIO.logError(e.getMessage()))
        .mapError (RepositoryError(_))
        .provide(driverLayer)
  }

  override def add(seqOrders : Seq[Order]) : IO[RepositoryError, Int] = {
    val xs = seqOrders.map(o => (o.id, o.customerId, o.orderDate))
    val sql = insertInto(orders)(id, customerId, orderDate).values(xs)
    execute(sql)
      .tapError(e => ZIO.logError(e.getMessage()))
      .mapError(RepositoryError(_))
      .provide(driverLayer)
  }
}


object OrderRepositoryImpl {
  val live: ZLayer[ConnectionPool, Nothing, OrderRepository] =
    ZLayer.fromFunction(new OrderRepositoryImpl(_))
}
