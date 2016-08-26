package io.faizan.model

import com.github.mkroli.dns4s.dsl.ResourceRecordModifier
import com.google.common.cache.{Cache, CacheBuilder}
import io.faizan.Utils
import io.faizan.config.Config
import scaldi.{Injectable, Injector}

import scala.collection.JavaConverters._


class DnsRecordsStorage(implicit inj: Injector) extends Injectable {
  private val conf = inject[Config]
  private val dnsRecordsModel = inject[DnsRecordsModel]
  val dnsMap = scala.collection.mutable.Map[String, DnsRecord]()
  val cache = CacheBuilder.newBuilder()
              .initialCapacity(512)
              .concurrencyLevel(Runtime.getRuntime.availableProcessors())
              .maximumSize(conf.dnsConf.maxEntries)
              .expireAfterWrite(conf.dnsConf.entryExpiryTime, conf.dnsConf.timeUnit)
              .build().asInstanceOf[Cache[String, ResourceRecordModifier]]

  def getIfPresent(domain: String): Option[ResourceRecordModifier] = {
    val dmn = domain.toLowerCase
    val dnsAnswer = Option(cache.getIfPresent(dmn))
    dnsAnswer match {
      case Some(x) => Some(x)
      case None =>
        val storedValue = dnsMap.get(dmn).map(Utils.convertToDns4s)
        storedValue.foreach(s => cache.put(dmn, s))
        storedValue
    }
  }

  def addEntries(entries: Map[String, DnsRecord]) = {
    val entriesIgnoreCase = entries.map(e=> e._1.toLowerCase->e._2.copy(domain=e._2.domain.toLowerCase))
    cache.invalidateAll(entriesIgnoreCase.keys.asJava)
    val writeResults = dnsRecordsModel.write(entriesIgnoreCase)
    val fetchedEntries = dnsRecordsModel.fetchByDomain(entriesIgnoreCase.keys)
    dnsMap ++= fetchedEntries
    cache.putAll(fetchedEntries.map(e => (e._1, Utils.convertToDns4s(e._2))).asJava)
    writeResults
  }

  def removeEntries(entries: Array[String]) = {
    cache.invalidateAll(entries.toList.asJava)
    dnsMap -- entries
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

  def getDnsMap = dnsMap

  def getCachedMap = cache

  def searchDnsMap(domain:String) = {
    dnsMap.filter(_._1.contains(domain))
  }

  def refresh = {
    clearCached()
    dnsMap ++= dnsRecordsModel.fetchAll
  }

  refresh
}
