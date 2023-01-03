package com.zio.acme.repo

import com.zio.acme.domain.Order
import zio.sql.postgresql.PostgresJdbcModule
import zio.stream._
import zio._
import zio.schema.DeriveSchema

trait PqTablesDescription extends PostgresJdbcModule {

    implicit val schema = DeriveSchema.gen[Order]
    val orders = defineTableSmart[Order]

    val (id, customerId, orderDate) = orders.columns

}