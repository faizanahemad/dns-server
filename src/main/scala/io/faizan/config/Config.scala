package io.faizan.config
import java.io.File
import java.nio.file.{Files, Paths}
import java.nio.file.attribute.FileAttribute
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.typesafe.config.ConfigFactory
import configs.Configs
import configs.syntax._
import io.faizan.{AppModuleSupport, Utils}

import scala.util.Try

object StorageMedium extends Enumeration {
  val JSON,MYSQLDB,FLAT_FILE = Value
}
class StorageMediumType extends TypeReference[StorageMedium.type]
@JsonIgnoreProperties(Array("properties"))
case class DBConfig(user:String
                    , password:String
                    , url:String
                    , dBName:String
                    , driver:String
                    , connectionPool:String
                    , keepAliveConnection:Boolean
                    , numThreads:Int) {
  val properties = Map[String,String]("user"->user,
                                      "password"->password,
                                      "url"->url,
                                      "DBNAME"-> dBName,
                                      "driver"->driver,
                                      "connectionPool"->connectionPool,
                                      "keepAliveConnection"->keepAliveConnection.toString,
                                      "numThreads"->numThreads.toString)
}
@JsonIgnoreProperties(Array("timeUnit"))
case class DNSConfig(dnsResolver:String, maxEntries:Int, entryExpiryTime:Int, var port:Int) {
  private val dnsPort = System.getProperty("dns.port")
  if (dnsPort!=null) {
    port = Try(dnsPort.trim.toInt).getOrElse(port)
  }
  val timeUnit  = TimeUnit.MINUTES
}
case class ApplicationConfig(var dnsJsonFile:String,@JsonScalaEnumeration(classOf[StorageMediumType]) storageMedium:StorageMedium.Value) {
  val defaultConfDir = Paths.get(System.getProperty("user.home") + File.separator + ".dnsserver")
  Files.createDirectories(defaultConfDir)
  if(!Utils.checkUrlValidity(dnsJsonFile)) {
    val configuredFilePath = Paths.get(dnsJsonFile)
    Try(Files.createFile(configuredFilePath)).recover{
                                                       case e:Exception=>
                                                         val dnsFilePath = Paths.get(defaultConfDir.toString,"dnsJsonFile.json")
                                                         val flatFile = dnsFilePath.toFile
                                                         if (!flatFile.exists()) {
                                                           val createdFile = Files.createFile(dnsFilePath)
                                                           dnsJsonFile = createdFile.toString
                                                           createdFile
                                                           } else {
                                                           dnsJsonFile = flatFile.toString
                                                           dnsFilePath
                                                         }
                                                     }
  }
}
@JsonIgnoreProperties(Array("config"))
case class Config(appConf: ApplicationConfig,dbConf:DBConfig, dnsConfig:DNSConfig,config:com.typesafe.config.Config) {
  def this(appConf: ApplicationConfig,dbConf:DBConfig, dnsConfig:DNSConfig) {
    this(appConf, dbConf, dnsConfig, ConfigFactory.load())
  }
  def persist(): Unit = {
    persist(this)
  }
  private def persist(config: Config): Unit = {

  }
}
object Config {
  var config =Option.empty[Config]
  def getConfig = {
    if (config.isEmpty) {
      ConfigFactory.invalidateCaches()
      val cfg:com.typesafe.config.Config=ConfigFactory.load()
      val dbConf :DBConfig=cfg.get[DBConfig]("dbConf").value
      val dnsConf = cfg.get[DNSConfig]("dnsConf").value
      val applicationConfig = cfg.get[ApplicationConfig]("application").value
      config = Option(Config(applicationConfig,dbConf,dnsConf,cfg))
    }
    config.get
  }
  def setConfig(conf: Config) = {
    config = Option(conf)
    getConfig.persist()
    ConfigFactory.invalidateCaches()
    config = Option(getConfig.copy())
  }
}
