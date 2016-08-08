package io.faizan.model

import java.util

import com.github.mkroli.dns4s.dsl.ResourceRecordModifier
import com.github.mkroli.dns4s.section.resource.AResource
import com.google.common.cache.{Cache, CacheBuilder}
import io.faizan.Utils
import io.faizan.config.Config
import scaldi.{Injectable, Injector}

import scala.collection.JavaConverters._


class DnsRecordsStorage(implicit inj: Injector) extends Injectable {
  private val conf = inject[Config]
  private val dnsRecordsModel = inject[DnsRecordsModel]
  val dnsMap = new util.HashMap[String, ResourceRecordModifier](512)
  val cache = CacheBuilder.newBuilder()
              .initialCapacity(512)
              .concurrencyLevel(Runtime.getRuntime.availableProcessors())
              .maximumSize(conf.dnsConf.maxEntries)
              .expireAfterWrite(conf.dnsConf.entryExpiryTime, conf.dnsConf.timeUnit)
              .build().asInstanceOf[Cache[String, ResourceRecordModifier]]

  def addEntries(entries: Map[String, String]) = {
    cache.invalidateAll(entries.keys.asJava)
    val convertedEntries = Utils.convertToDns4s(entries)
    dnsMap.putAll(convertedEntries)
    cache.putAll(convertedEntries)
    dnsRecordsModel.write(entries)
  }

  def removeEntries(entries: Array[String]) = {
    cache.invalidateAll(entries.toList.asJava)
    entries.foreach(dnsMap.remove)
    dnsRecordsModel.remove(entries)
  }

  private def clearCached() = {
    cache.invalidateAll()
    dnsMap.clear()
  }

  def removeAllEntries() = {
    clearCached()
    dnsRecordsModel.removeAll
  }

  def deleteAndCreateNew(entries: Map[String, String]) = {
    removeAllEntries()
    addEntries(entries)
  }

  def getDnsMap = {
    dnsMap.asScala.map(e => (e._1, e._2.rdata.asInstanceOf[AResource].address))
  }

  def refresh = {
    clearCached()
    dnsMap.putAll(dnsRecordsModel.fetchAllasARecord)
  }

  refresh
}
