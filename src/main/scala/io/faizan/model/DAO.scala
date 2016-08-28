package io.faizan.model

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import io.faizan.AppModule
import scaldi.Injectable
import slick.ast.{ScalaBaseType, ScalaOptionType}
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable
import slick.lifted.TableQuery
import slick.model.Column
import slick.lifted.{ProvenShape, TableQuery, Tag}
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag

object DBConstants {
  val stringComparator:(String,String)=>Boolean = (p1, p2) => p1.compareTo(p2) < 0
  val dateTimeComparator:(LocalDateTime,LocalDateTime)=>Boolean = (d1,d2)=>d1.isBefore(d2)
  val javaLocalDateTimeMapper = MappedColumnType.base[LocalDateTime, Timestamp](
    l => Timestamp.valueOf(l),
    t => t.toLocalDateTime
  )
}
trait IdentifiableRow[I] {
  def pk: I
  def createdAt: Option[LocalDateTime]
  def updatedAt: Option[LocalDateTime]
}
abstract class IdentifiableTable[I:ClassTag,ROW<:IdentifiableRow[I]](tag: Tag, tableName:String, pkName:String, implicit val f: (I, I) => Boolean) extends Table[ROW](tag, tableName) {
  implicit val ordering = Ordering.fromLessThan[I](f)
  implicit val dateOrdering = Ordering.fromLessThan[LocalDateTime](DBConstants.dateTimeComparator)
  implicit private val typeTT = new ScalaBaseType[I]
  implicit private val dateTT = new ScalaOptionType[LocalDateTime](new ScalaBaseType[LocalDateTime])
  def pk = column[I](pkName, O.PrimaryKey)
}


protected[model] trait DAO[PK,T <: IdentifiableRow[PK], U <: IdentifiableTable[PK,T]] extends RecordsModel[PK,T] {
  implicit private val injector = AppModule.getCurrentInjector
  lazy val db = inject[Database]

  def queryHelper: TableQuery[U]

  def findByPkQuery(pk:PK):Query[U, U#TableElementType, scala.Seq]

  def findByPkInQuery(keys:Iterable[PK]):Query[U, U#TableElementType, scala.Seq]

  def findByPk(pk:PK):T = {
    run(findByPkQuery(pk).result.head)
  }


  def fetchAll: Map[PK,T] = run(queryHelper.result).map(e=>(e.pk,e)).toMap

  def findByPkIn(keys:Iterable[PK]):Map[PK,T] = run(findByPkInQuery(keys).result).map(e=>(e.pk,e)).toMap

  def write(entries:Map[PK,T]):Boolean = insertOrUpdate(entries.values) >0

  def remove(entries:Iterable[PK]):Boolean = deleteByPk(entries) >0

  def removeAll:Boolean = deleteAll >0

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
