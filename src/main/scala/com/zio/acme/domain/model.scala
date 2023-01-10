package com.zio.acme.domain

import java.time.LocalDate
import java.util.UUID

final case class Order(id: UUID, customerId: UUID, orderDate: LocalDate)

final case class OrderLineItem(pos : Int, article : String, desc : String,
                     qty : Int, price : BigDecimal, total :  BigDecimal,
                     discountValue : BigDecimal = 0.0, refs : String = "")
                     

final case class Customers(id: UUID, firstName: String, lastName: String,
      verified: Boolean, dob: LocalDate)

final case class Product(id: UUID, name: String, description: String, imageUrl: String)

// TODO : move to repo ?
sealed trait DomainError extends Throwable
object DomainError {
  final case class RepositoryError(cause : Throwable) extends DomainError
}