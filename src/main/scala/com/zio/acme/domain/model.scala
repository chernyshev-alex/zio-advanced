package com.zio.acme.domain

import java.time.LocalDate
import java.util.UUID

final case class Order(id: UUID, customerId: UUID, orderDate: LocalDate)

final case class OrderLineItem(pos : Int, article : String, desc : String,
                     qty : Int, price : BigDecimal, total :  BigDecimal,
                     discountValue : BigDecimal = 0.0, refs : String = "")

// TODO : move to repo ?
sealed trait DomainError extends Throwable
object DomainError {
  final case class RepositoryError(ex: Throwable) extends DomainError
}