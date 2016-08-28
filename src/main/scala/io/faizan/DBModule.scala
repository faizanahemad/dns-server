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
      bind[RecordsModel[String,DnsRecord]] to new DnsRecords
      bind[RecordsModel[String,RedirectRecord]] to new RedirectRecords
    case StorageMedium.JSON=>
      bind[RecordsModel[String,DnsRecord]] to new DnsRecordsModelJson
      bind[RecordsModel[String,RedirectRecord]] to new RedirectRecordsModelJson
  }
  bind[DnsRecordsStorage] to new DnsRecordsStorage
  bind[RedirectRecordsStorage] to new RedirectRecordsStorage
}
