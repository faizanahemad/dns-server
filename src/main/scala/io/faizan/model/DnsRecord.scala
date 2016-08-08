package io.faizan.model

import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

case class DnsRecord(pk: String, dns: String) extends IdentifiableRow[String] {
  def domain = pk
}

class DnsRecordTable(tag: Tag) extends IdentifiableTable[String,DnsRecord](tag, "flat_dns_records", "domain", (p1, p2)=> p1.compareTo(p2) <0) {

  def dns = column[String]("dns")

  override def * : ProvenShape[DnsRecord] = (pk, dns) <> (DnsRecord.tupled, DnsRecord.unapply)
}

class DnsRecords extends DAO[String, DnsRecord, DnsRecordTable] {
  override def findByPkQuery(pk: String) = queryHelper.filter(_.pk === pk)

  override def queryHelper = TableQuery[DnsRecordTable]

  override def findByPkInQuery(pk: Iterable[String]) = queryHelper.filter(_.pk inSet pk)
}
