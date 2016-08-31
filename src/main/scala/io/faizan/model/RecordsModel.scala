package io.faizan.model

import java.util

import com.fasterxml.jackson.core.`type`.TypeReference
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

abstract class RecordsModelJson[PK,T<:IdentifiableRow[PK]](implicit inj: Injector) extends RecordsModel[PK,T] with Injectable {
  protected val objectMapper = inject[ObjectMapper](identified by AppModuleSupport.objectMapper)
  def jsonFile:String
  override def write(entries: Map[PK,T]): Boolean = {
    val finalMap = fetchAll ++ entries
    Utils.putJsonFileContents(jsonFile,finalMap)
  }

  override def remove(entries: Iterable[PK]): Boolean = {
    val finalMap = fetchAll -- entries
    Utils.putJsonFileContents(jsonFile,finalMap)
  }

  override def removeAll: Boolean = Utils.putJsonFileContents(jsonFile,objectMapper.createObjectNode())

  override def findByPkIn(entries: Iterable[PK]): Map[PK,T] = {
    val entriesSet = entries.toSet
    fetchAll.filter(e=>entriesSet.contains(e._1))
  }
}

class DnsRecordsModelJson(implicit inj: Injector) extends RecordsModel[String,DnsRecord] {
  private val conf = inject[Config]
  private val objectMapper = inject[ObjectMapper](identified by AppModuleSupport.objectMapper)
  private val appConf = conf.application
  override def fetchAll:Map[String,DnsRecord] = {
    val node = Utils.getJsonFileContents(conf.application.dnsJsonFile)
    val typereference = new TypeReference[Map[String, DnsRecord]] {}
    objectMapper.convertValue(node, typereference)
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

class RedirectRecordsModelJson(implicit inj: Injector) extends RecordsModelJson[String,RedirectRecord] {
  private val conf = inject[Config]
  private val appConf = conf.application
  override def fetchAll: Map[String, RedirectRecord] = {
    val node = Utils.getJsonFileContents(jsonFile)
    val typereference = new TypeReference[Map[String, RedirectRecord]] {}
    objectMapper.convertValue(node, typereference)
  }
  override def jsonFile: String = appConf.urlShortnerJsonFile
}
