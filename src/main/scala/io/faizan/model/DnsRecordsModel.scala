package io.faizan.model

import java.util

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.mkroli.dns4s.dsl.{ARecord, RRName, ResourceRecordModifier}
import io.faizan.Utils._
import io.faizan.{AppModuleSupport, Utils}
import io.faizan.config.Config
import scaldi.{Injectable, Injector}

import scala.collection.JavaConverters._
abstract class DnsRecordsModel(implicit val inj: Injector) extends Injectable {
  protected val conf = inject[Config]
  protected val objectMapper = inject[ObjectMapper](identified by AppModuleSupport.objectMapper)

  def fetchAll:java.util.Map[String,String]
  def fetchAllasARecord: util.HashMap[String, ResourceRecordModifier] = {
    Utils.convertToDns4s(fetchAll.asScala.toMap)
  }
  def write(entries:Map[String,String]):Boolean
  def remove(entries:Iterable[String]):Boolean
  def removeAll:Boolean

}

class DnsRecordsModelDB(implicit inj: Injector) extends DnsRecordsModel with Injectable {
  protected val flatDnsRecords = inject[DnsRecords]
  override def fetchAll:java.util.Map[String,String] = {
    val dbDns = flatDnsRecords.fetchAll.map(f=>(urlToDomainName(f.domain),f.dns)).toMap
    val records = new util.HashMap[String,String](128,0.5f)
    records.putAll(dbDns.asJava)
    records
  }

  override def write(entries: Map[String, String]): Boolean = flatDnsRecords.insertOrUpdate(entries.map(e=>DnsRecord(e._1,e._2))) >0

  override def remove(entries: Iterable[String]): Boolean = flatDnsRecords.deleteByPk(entries)>0

  override def removeAll: Boolean = flatDnsRecords.deleteAll>0
}

class DnsRecordsModelJson(implicit inj: Injector) extends DnsRecordsModel with Injectable {
  private val appConf = conf.application
  private def getJsonFlatConfig: JsonNode = {
    val dnsConfigFile = conf.application.dnsJsonFile
    Utils.getJsonFileContents(dnsConfigFile)
  }
  def fetchAll:java.util.Map[String,String] = {
    val node = getJsonFlatConfig
    val parsedDnsRecords = objectMapper.convertValue(node, classOf[Map[String, String]])
                           .map(e => (urlToDomainName(e._1), e._2))
    val records = new util.HashMap[String,String](128,0.5f)
    records.putAll(parsedDnsRecords.asJava)
    records
  }
  override def write(entries: Map[String, String]): Boolean = {
    val finalMap = fetchAll.asScala ++ entries
    Utils.putJsonFileContents(appConf.dnsJsonFile,finalMap)
  }

  override def remove(entries: Iterable[String]): Boolean = {
    val finalMap = fetchAll.asScala
    entries.foreach(finalMap.remove)
    Utils.putJsonFileContents(appConf.dnsJsonFile,finalMap)
  }

  override def removeAll: Boolean = Utils.putJsonFileContents(appConf.dnsJsonFile,objectMapper.createObjectNode())
}

class DnsRecordsModelFlat(implicit inj: Injector) extends DnsRecordsModel with Injectable {
  val appConf = conf.application
  override def fetchAll:java.util.Map[String,String] = new util.HashMap[String,String](128,0.5f)

  override def write(entries: Map[String, String]): Boolean = true

  override def remove(entries: Iterable[String]): Boolean = true

  override def removeAll: Boolean = true
}
