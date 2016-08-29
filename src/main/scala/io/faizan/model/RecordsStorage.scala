package io.faizan.model

import com.github.mkroli.dns4s.dsl.{ComposableMessage, ResourceRecordModifier}
import com.google.common.cache.{Cache, CacheBuilder}
import io.faizan.Utils
import io.faizan.config.Config
import scaldi.{Injectable, Injector}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag


abstract class RecordsStorage[PK:ClassTag,T<:IdentifiableRow[PK],V](implicit inj: Injector) extends Injectable {
  private val conf = inject[Config]
  protected lazy val storageMap:scala.collection.mutable.Map[PK,T] = scala.collection.mutable.Map[PK,T]()
  protected lazy val cache:Cache[PK,V] = CacheBuilder.newBuilder()
              .initialCapacity(512)
              .concurrencyLevel(Runtime.getRuntime.availableProcessors())
              .maximumSize(conf.dnsConf.maxEntries)
              .expireAfterWrite(conf.dnsConf.entryExpiryTime, conf.dnsConf.timeUnit)
              .build().asInstanceOf[Cache[PK,V]]

  private def sanitizeInput(input:(PK,T)):(PK,T) = sanitizeInputKey(input._1)->sanitizeInputValue(input._2)

  protected def recordsModel:RecordsModel[PK,T]

  protected def sanitizeInputKey(key:PK):PK

  protected def sanitizeInputValue(v:T):T

  protected def convertStoredEntry(entry:T):V

  protected def searchComparator(searchKey:PK,storedMapKey:PK):Boolean

  def getIfPresent(key: PK): Option[V] = {
    val sanitizedInput = sanitizeInputKey(key)
    val answer = Option(cache.getIfPresent(sanitizedInput))
    answer match {
      case Some(x) => Some(x)
      case None =>
        val storedValue = storageMap.get(sanitizedInput).map(convertStoredEntry)
        storedValue.foreach(s => cache.put(sanitizedInput, s))
        storedValue
    }
  }

  def addEntries(entries: Map[PK, T]) = {
    val entriesSanitized = entries.map(sanitizeInput)
    cache.invalidateAll(entriesSanitized.keys.asJava)
    val writeResults = recordsModel.write(entriesSanitized)
    val fetchedEntries = recordsModel.findByPkIn(entriesSanitized.keys)
    storageMap ++= fetchedEntries
    cache.putAll(fetchedEntries.map(e => (e._1, convertStoredEntry(e._2))).asJava)
    writeResults
  }

  def addToCache(key:PK,v:V) = {
    cache.put(sanitizeInputKey(key),v)
  }

  def removeEntries(entries: Array[PK]) = {
    cache.invalidateAll(entries.toList.asJava)
    storageMap -- entries
    recordsModel.remove(entries)
  }

  private def clearCached() = {
    cache.invalidateAll()
    storageMap.clear()
  }

  def removeAllEntries() = {
    clearCached()
    recordsModel.removeAll
  }

  def getStorageMap = storageMap

  def getCachedMap = cache

  def searchMap(key:PK) = {
    storageMap.filter(e=> searchComparator(key, e._1))
  }

  storageMap ++= recordsModel.fetchAll
}

class DnsRecordsStorage(implicit inj: Injector) extends RecordsStorage[String,DnsRecord,ComposableMessage] {

  override protected def recordsModel: RecordsModel[String, DnsRecord] = inject[RecordsModel[String,DnsRecord]]


  override protected def convertStoredEntry(entry: DnsRecord): ComposableMessage = Utils.convertToDns4s(sanitizeInputValue(entry))

  override protected def searchComparator(searchKey: String,
                                storedMapKey: String): Boolean = storedMapKey.contains(searchKey)

  override protected def sanitizeInputKey(key: String): String = Utils.urlToDomainName(key)

  override protected def sanitizeInputValue(v: DnsRecord): DnsRecord = v.copy(domain = sanitizeInputKey(v.domain),dns = Utils.dnsToSanitizedDns(v.dns))
}

class RedirectRecordsStorage(implicit inj: Injector) extends RecordsStorage[String,RedirectRecord,String] {

  override protected def recordsModel: RecordsModel[String, RedirectRecord] = inject[RecordsModel[String,RedirectRecord]]


  override protected def convertStoredEntry(entry: RedirectRecord): String = Utils.urlToRedirectUrl(entry.redirectUrl)

  override protected def searchComparator(searchKey: String,
                                storedMapKey: String): Boolean = storedMapKey.contains(searchKey)

  override protected def sanitizeInputKey(key: String): String = key.trim

  override protected def sanitizeInputValue(v: RedirectRecord): RedirectRecord = v.copy(redirectUrl = Utils.urlToRedirectUrl(v.redirectUrl))


  override def getIfPresent(key: String): Option[String] = {
    val sanitizedInput = sanitizeInputKey(key)
    storageMap.get(sanitizedInput).map(convertStoredEntry)
  }
}
