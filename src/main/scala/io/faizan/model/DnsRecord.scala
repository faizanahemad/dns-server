package io.faizan.model

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.regex.Pattern

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import io.faizan.Utils
import io.faizan.model.RecordType.RecordType
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}
import slick.sql.SqlProfile.ColumnOption.SqlType

import scala.util.Try


object RecordType extends Enumeration {
  type RecordType = Value
  val A = Value("A")
  val AAAA = Value("AAAA")
  val CNAME = Value("CNAME")
  val DNAME = Value("DNAME")
}

class RecordTypeType extends TypeReference[RecordType.type]

case class DnsRecord(domain: String,
                     dns: String,
                     @JsonScalaEnumeration(
                       classOf[RecordTypeType]) recordType: RecordType = RecordType.A,
                     createdAt: Option[LocalDateTime] = Option.empty,
                     updatedAt: Option[LocalDateTime] = Option
                                                        .empty) extends IdentifiableRow[String] {

  require(Utils.domainPattern.matcher(domain).matches())
  require(Utils.dnsPattern.matcher(dns).matches())
  def pk = domain
}

class DnsRecordTable(tag: Tag) extends IdentifiableTable[String, DnsRecord](tag, "flat_dns_records",
                                                                            "domain", (p1, p2) => p1
                                                                                                  .compareTo(
                                                                                                    p2) < 0) {

  implicit val javaLocalDateTimeMapper = MappedColumnType.base[LocalDateTime, Timestamp](
    l => Timestamp.valueOf(l),
    t => t.toLocalDateTime
  )

  implicit val recordMapper = MappedColumnType.base[RecordType, String](
    e => e.toString,
    s => RecordType.withName(s)
  )

  def dns = column[String]("dns", SqlType("varchar(254) not null"))

  def record_type = column[RecordType]("record_type", O.Default(RecordType.A))


  def created_at = column[Option[LocalDateTime]]("created_at", SqlType(
    "timestamp not null default CURRENT_TIMESTAMP"))

  def updated_at = column[Option[LocalDateTime]]("updated_at", SqlType(
    "timestamp not null default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP"))

  override def * : ProvenShape[DnsRecord] = (pk, dns, record_type, created_at, updated_at) <> (DnsRecord
                                                                                               .tupled, DnsRecord
                                                                                                        .unapply)

}

class DnsRecords extends DAO[String, DnsRecord, DnsRecordTable] {
  override def findByPkQuery(pk: String) = queryHelper.filter(_.pk === pk)

  override def queryHelper = TableQuery[DnsRecordTable]

  override def findByPkInQuery(pk: Iterable[String]) = queryHelper.filter(_.pk inSet pk)

  override def insertOrUpdate(elements: Iterable[DnsRecord]): Int = {
    val dbRecords = findByPkIn(elements.map(_.domain))
    val dbRecordsMap = dbRecords.map(r => (r.domain, r)).toMap
    val recordsToInsert = elements.map(el => {
      val dbr = dbRecordsMap.getOrElse(el.domain.trim, el)
      dbr.copy(dns = el.dns.trim).copy(domain = dbr.domain.trim).copy(updatedAt = Option(LocalDateTime.now()))
    })
    super.insertOrUpdate(recordsToInsert)
  }
}
