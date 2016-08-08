package io.faizan.model

import java.util.concurrent.TimeUnit

import io.faizan.AppModule
import scaldi.Injectable
import slick.ast.ScalaBaseType
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable
import slick.lifted.TableQuery
import slick.model.Column
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag

trait IdentifiableRow[I] {
  def pk: I
}
abstract class IdentifiableTable[I:ClassTag,ROW](tag: Tag, tableName:String, pkName:String, implicit val f: (I, I) => Boolean) extends Table[ROW](tag, tableName) {
  implicit val ordering = Ordering.fromLessThan[I](f)
  implicit private val typeTT = new ScalaBaseType[I]
  def pk = column[I](pkName, O.PrimaryKey)
}


protected[model] trait DAO[PK,T <: IdentifiableRow[PK], U <: IdentifiableTable[PK,T]] extends Injectable {
  implicit private val injector = AppModule.getCurrentInjector
  lazy val db = inject[Database]

  def queryHelper: TableQuery[U]

  def findByPkQuery(pk:PK):Query[U, U#TableElementType, scala.Seq]

  def findByPkInQuery(keys:Iterable[PK]):Query[U, U#TableElementType, scala.Seq]

  def findByPk(pk:PK):T = {
    run(findByPkQuery(pk).result.head)
  }


  def fetchAll: Iterable[T] = run(queryHelper.result).toList

  def insertOrUpdate(elements:Iterable[T]):Int = {
    elements.map(queryHelper.insertOrUpdate).map(run).sum
  }

  def deleteByPk(elements:Iterable[PK]):Int = {
    run(findByPkInQuery(elements).delete)
  }

  def delete(elements:Iterable[T]):Int = {
    deleteByPk(elements.map(_.pk))
  }

  def deleteAll:Int = {
    run(queryHelper.delete)
  }

  protected def run[R](a: DBIOAction[R, NoStream, Nothing]): R = {
    val waiter = db.run(a)
    Await.result(waiter, Duration(2000, TimeUnit.MILLISECONDS))
  }

  def createTableIfNotExists(tables: TableQuery[_ <: Table[_]]*): Future[Seq[Unit]] = {
    Future.sequence(
      tables map { table =>
        db.run(MTable.getTables(table.baseTableRow.tableName)).flatMap { result =>
          if (result.isEmpty) {
            db.run(table.schema.create)
          } else {
            Future.successful(())
          }
                                                                       }
      }
    )
  }
  Await.result(createTableIfNotExists(queryHelper), Duration.Inf)
}

trait MockDAO[PK,T <: IdentifiableRow[PK], U <: IdentifiableTable[PK,T]] extends DAO[PK,T,U] {
  override lazy val db = null
  override def queryHelper: TableQuery[U] = null

  override def fetchAll: List[T] = List()

  override def createTableIfNotExists(tables: TableQuery[_ <: Table[_]]*): Future[Seq[Unit]] = {
    Future(List())
  }

  override def insertOrUpdate(elements:Iterable[T]):Int = 0

  override def deleteByPk(elements:Iterable[PK]):Int = 0

  override def deleteAll:Int = 0

  override def delete(elements:Iterable[T]):Int = 0
}
