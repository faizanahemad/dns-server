package io.faizan

import javax.naming.spi.Resolver

import io.faizan.config.{Config, StorageMedium}
import io.faizan.model._
import scaldi.Module
import slick.jdbc.MySQLProfile.api._

class DBModule(config: Config) extends Module {
  config.application.storageMedium match {
    case StorageMedium.MYSQLDB=>
      Utils.createDb(config.dbConf)
      lazy val db = Database.forURL(config.dbConf.url, config.dbConf.properties)
      bind[Database] to db
      bind[DnsRecords] to new DnsRecords
      bind[DnsRecordsModel] to new DnsRecordsModelDB
    case StorageMedium.JSON=>
      bind[DnsRecords] to new DnsRecords with MockDAO[String,DnsRecord, DnsRecordTable]
      bind[DnsRecordsModel] to new DnsRecordsModelJson
  }
}
