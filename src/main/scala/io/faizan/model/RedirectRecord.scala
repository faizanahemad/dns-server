package io.faizan.model

import java.time.LocalDateTime

import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}
import slick.sql.SqlProfile.ColumnOption.SqlType

case class RedirectRecord(requestUrl:String,
                          redirectUrl:String,
                          createdAt: Option[LocalDateTime] = Option.empty,
                          updatedAt: Option[LocalDateTime] = Option.empty) extends IdentifiableRow[String] {
  def pk = requestUrl
  require(requestUrl.length>0)
  require(redirectUrl.length>0)
}

class RedirectRecordTable(tag: Tag) extends IdentifiableTable[String, RedirectRecord](tag, "redirect_records",
                                                                                      "request_url", DBConstants.stringComparator) {

  implicit val javaLocalDateTimeMapper = DBConstants.javaLocalDateTimeMapper

  def redirectUrl = column[String]("redirect_url", SqlType("varchar(254) not null"))

  def created_at = column[Option[LocalDateTime]]("created_at", SqlType(
    "timestamp not null default CURRENT_TIMESTAMP"))

  def updated_at = column[Option[LocalDateTime]]("updated_at", SqlType(
    "timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP"))

  override def * : ProvenShape[RedirectRecord] = (pk, redirectUrl, created_at, updated_at) <> (RedirectRecord.tupled, RedirectRecord.unapply)
}

class RedirectRecords extends DAO[String, RedirectRecord, RedirectRecordTable] {
  override def findByPkQuery(pk: String) = queryHelper.filter(_.pk === pk)

  override def queryHelper = TableQuery[RedirectRecordTable]

  override def findByPkInQuery(pk: Iterable[String]) = queryHelper.filter(_.pk inSet pk)

  override def insertOrUpdate(elements: Iterable[RedirectRecord]): Int = {
    val dbRecordsMap = findByPkIn(elements.map(_.requestUrl))
    val recordsToInsert = elements.map(el => {
      val dbr = dbRecordsMap.getOrElse(el.requestUrl, el)
      dbr.copy(redirectUrl = el.redirectUrl,updatedAt = Option(LocalDateTime.now()))
    })
    super.insertOrUpdate(recordsToInsert)
  }
}
