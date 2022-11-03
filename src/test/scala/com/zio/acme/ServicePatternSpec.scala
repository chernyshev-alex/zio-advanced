package com.zio.acme

import zio._
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}

object ServicePatternSpec extends  ZIOSpecDefault {
  final case class Book(isbn: String, title: String, authorId: Int, issued: Int)

  trait BookStore {
    def getBookByISBN(isbn: String): Task[Option[Book]]
  }

  object BookStore {
    def getBookByISBN(isbn: String): RIO[BookStore, Option[Book]] =
      ZIO.serviceWithZIO[BookStore](_.getBookByISBN(isbn))
  }

  // service impl
  final case class MemBookStore(ref: Ref[Map[String, Book]]) extends BookStore {
    override def getBookByISBN(isbn: String): ZIO[Any, Throwable, Option[Book]] =
      ref.get.map(_.get(isbn))
  }

  object BookStoreLayerFactory {
    def build(books : Seq[Book]) = ZLayer {
        for {
          ref <- Ref make books.map(b => b.isbn -> b).toMap
        } yield MemBookStore(ref)
      }
    }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("Books") {
      test("Book not found") {
        for {
          u <- BookStore.getBookByISBN("11")
        } yield assertTrue(u.isEmpty)
      }.provideLayer(BookStoreLayerFactory.build(Seq.empty[Book]))  +
        test("Book exists") {
          for {
            u <- BookStore.getBookByISBN("1")
          } yield assertTrue(u.isDefined && u.get.isbn == "1")
        }.provideLayer {
          val books = Seq(Book("1", "B1", 1000, 2000), Book("2", "B2", 2000, 2001))
          BookStoreLayerFactory.build(books)
        }
    }
  }
}
