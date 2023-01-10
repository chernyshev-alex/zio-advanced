package com.zio.acme.repo

import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase
import zio._
import java.sql.SQLException
import com.zio.acme.domain._

class CustomerRepository(quill : Quill.Postgres[SnakeCase]) {
    import quill._
    def getCustomers : ZIO[Any, SQLException, List[Customers]] = run(query[Customers])
}

object CustomerRepository {
    def getCustomers : ZIO[CustomerRepository, SQLException, List[Customers]] = 
        ZIO.serviceWithZIO[CustomerRepository](_.getCustomers)
    
    val live = ZLayer.fromFunction(new CustomerRepository(_))
}