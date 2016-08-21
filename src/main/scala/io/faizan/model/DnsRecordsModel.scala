package io.faizan.model

import java.util

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.mkroli.dns4s.dsl.{ARecord, RRName, ResourceRecordModifier}
import io.faizan.Utils._
import io.faizan.{AppModuleSupport, Utils}
import io.faizan.config.Config
import scaldi.{Injectable, Injector}


abstract class DnsRecordsModel(implicit val inj: Injector) extends Injectable {
  protected val conf = inject[Config]
  protected val objectMapper = inject[ObjectMapper](identified by AppModuleSupport.objectMapper)

  def fetchAll:Map[String,DnsRecord]
  def fetchByDomain(entries:Iterable[String]):Map[String,DnsRecord]
  def write(entries:Map[String,DnsRecord]):Boolean
  def remove(entries:Iterable[String]):Boolean
  def removeAll:Boolean

}

class DnsRecordsModelDB(implicit inj: Injector) extends DnsRecordsModel with Injectable {
  protected val flatDnsRecords = inject[DnsRecords]
  override def fetchAll:Map[String,DnsRecord] = {
    flatDnsRecords.fetchAll.map(f=>(urlToDomainName(f.domain),f)).toMap
  }

  override def write(entries: Map[String, DnsRecord]): Boolean = flatDnsRecords.insertOrUpdate(entries.values) >0

  override def remove(entries: Iterable[String]): Boolean = flatDnsRecords.deleteByPk(entries)>0

  override def removeAll: Boolean = flatDnsRecords.deleteAll>0

  override def fetchByDomain(entries: Iterable[String]): Map[String, DnsRecord] = flatDnsRecords.findByPkIn(entries).map(f=>(urlToDomainName(f.domain),f)).toMap
}

class DnsRecordsModelJson(implicit inj: Injector) extends DnsRecordsModel with Injectable {
  private val appConf = conf.application
  private def getJsonFlatConfig: JsonNode = {
    val dnsConfigFile = conf.application.dnsJsonFile
    Utils.getJsonFileContents(dnsConfigFile)
  }
  def fetchAll:Map[String,DnsRecord] = {
    val node = getJsonFlatConfig
    val parsedDnsRecords = objectMapper.convertValue(node, classOf[Map[String, String]])
                           .map(e => (urlToDomainName(e._1), e._2)).map(e=>(e._1,DnsRecord(e._1,e._2)))
    parsedDnsRecords
  }
  override def write(entries: Map[String, DnsRecord]): Boolean = {
    val finalMap = fetchAll ++ entries
    Utils.putJsonFileContents(appConf.dnsJsonFile,finalMap)
  }

  override def remove(entries: Iterable[String]): Boolean = {
    val finalMap = fetchAll -- entries
    Utils.putJsonFileContents(appConf.dnsJsonFile,finalMap)
  }

  override def removeAll: Boolean = Utils.putJsonFileContents(appConf.dnsJsonFile,objectMapper.createObjectNode())

  override def fetchByDomain(entries: Iterable[String]): Map[String, DnsRecord] = {
    val entriesSet = entries.toSet
    fetchAll.filter(e=>entriesSet.contains(e._1))
  }
}
