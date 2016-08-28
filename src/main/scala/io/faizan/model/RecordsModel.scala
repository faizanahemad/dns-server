package io.faizan.model

import java.util

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.mkroli.dns4s.dsl.{ARecord, RRName, ResourceRecordModifier}
import io.faizan.Utils._
import io.faizan.{AppModuleSupport, Utils}
import io.faizan.config.Config
import scaldi.{Injectable, Injector}


trait RecordsModel[PK,T<:IdentifiableRow[PK]] extends Injectable {
  def fetchAll:Map[PK,T]
  def findByPkIn(entries:Iterable[PK]):Map[PK,T]
  def write(entries:Map[PK,T]):Boolean
  def remove(entries:Iterable[PK]):Boolean
  def removeAll:Boolean

}

class DnsRecordsModelJson(implicit inj: Injector) extends RecordsModel[String,DnsRecord] {
  protected val conf = inject[Config]
  protected val objectMapper = inject[ObjectMapper](identified by AppModuleSupport.objectMapper)
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

  override def findByPkIn(entries: Iterable[String]): Map[String, DnsRecord] = {
    val entriesSet = entries.toSet
    fetchAll.filter(e=>entriesSet.contains(e._1))
  }
}

class RedirectRecordsModelJson extends RecordsModel[String,RedirectRecord] {
  override def fetchAll: Map[String, RedirectRecord] = ???

  override def findByPkIn(entries: Iterable[String]): Map[String, RedirectRecord] = ???

  override def write(entries: Map[String, RedirectRecord]): Boolean = ???

  override def remove(entries: Iterable[String]): Boolean = ???

  override def removeAll: Boolean = ???
}
