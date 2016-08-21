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
  val JSON,MYSQLDB = Value
}
class StorageMediumType extends TypeReference[StorageMedium.type]
@JsonIgnoreProperties(Array("properties","driver","numThreads"))
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
                                      "numThreads"->numThreads.toString,
                                      "useSSL"->"false",
                                      "rewriteBatchedStatements"->"true")
}
@JsonIgnoreProperties(Array("timeUnit","port","dnsPort"))
case class DNSConfig(dnsResolver:String, maxEntries:Int, entryExpiryTime:Int, var port:Int) {
  private val dnsPort = System.getProperty("dns.port")
  if (dnsPort!=null) {
    port = Try(dnsPort.trim.toInt).getOrElse(port)
  }
  val timeUnit  = TimeUnit.MINUTES
}
@JsonIgnoreProperties(Array("defaultConfDir"))
case class ApplicationConfig(var dnsJsonFile:String,@JsonScalaEnumeration(classOf[StorageMediumType]) storageMedium:StorageMedium.Value) {
  val defaultConfDir = Config.defaultConfDir
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
case class Config(application: ApplicationConfig, dbConf:DBConfig, dnsConf:DNSConfig, config:com.typesafe.config.Config) {
  def this(appConf: ApplicationConfig,dbConf:DBConfig, dnsConfig:DNSConfig) {
    this(appConf, dbConf, dnsConfig, Config.readConfig)
  }
}
object Config {
  val defaultConfDir = Paths.get(System.getProperty("user.home") + File.separator + ".dnsserver")
  val configFile = Paths.get(defaultConfDir.toString,"application.json").toFile
  var config =Option.empty[Config]
  protected def readConfig = {
    ConfigFactory.invalidateCaches()
    val default:com.typesafe.config.Config=ConfigFactory.load()
    val fileConfig = Try(ConfigFactory.parseFile(configFile)).getOrElse(ConfigFactory.load())
    val cfg = fileConfig.withFallback(default)
    cfg
  }
  protected def saveConfig(conf: Config) = {
    Utils.putJsonFileContents(configFile,conf)
  }
  def getConfig = {
    if (config.isEmpty) {
      getNewConfig
    }
    config.get
  }
  def getNewConfig = {
    val cfg = readConfig
    val dbConf :DBConfig=cfg.get[DBConfig]("dbConf").value
    val dnsConf = cfg.get[DNSConfig]("dnsConf").value
    val applicationConfig = cfg.get[ApplicationConfig]("application").value
    config = Option(Config(applicationConfig,dbConf,dnsConf,cfg))
    config.get
  }
  def setConfig(conf: Config) = {
    saveConfig(conf)
    config = Option(getNewConfig)
  }
}
