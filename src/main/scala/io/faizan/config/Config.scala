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
@JsonIgnoreProperties(Array("properties","driver","numThreads","connectionPool","keepAliveConnection"))
case class DBConfig(user:String
                    , password:String
                    , url:String
                    , dBName:String
                    , driver:String
                    , connectionPool:String
                    , keepAliveConnection:Boolean
                    , numThreads:Int) {
  require(Utils.alphaNumericUnderscoreRegex.matcher(dBName).matches())
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
@JsonIgnoreProperties(Array("timeUnit"))
case class DNSConfig(dnsResolver:String,dnsResolverSecondLevel:String, maxEntries:Int, entryExpiryTime:Int) {
  require(Utils.dnsPattern.matcher(dnsResolver).matches())
  require(Utils.dnsPattern.matcher(dnsResolverSecondLevel).matches())
  val timeUnit  = TimeUnit.MINUTES
}
@JsonIgnoreProperties(Array("defaultConfDir","devMode"))
case class ApplicationConfig(var dnsJsonFile:String,var urlShortnerJsonFile:String,@JsonScalaEnumeration(classOf[StorageMediumType]) storageMedium:StorageMedium.Value) {
  require(Utils.fileRegexPattern.matcher(dnsJsonFile).matches())
  require(Utils.fileRegexPattern.matcher(urlShortnerJsonFile).matches())
  dnsJsonFile = dnsJsonFile.replaceFirst("^~",System.getProperty("user.home"))
  urlShortnerJsonFile = urlShortnerJsonFile.replaceFirst("^~",System.getProperty("user.home"))
  val defaultConfDir = Config.defaultConfDir
  val devMode = Try(System.getProperty("dev").toBoolean).getOrElse(false)
  Files.createDirectories(defaultConfDir)
  def createIfNotExistsDataFile(filename:String, defaultFileName:String): String = {
    if(!Utils.checkUrlValidity(filename)) {
      val configuredFilePath = Paths.get(filename)
      Try(Files.createFile(configuredFilePath).toString).recover{
                                                         case e:Exception=>
                                                           val dnsFilePath = Paths.get(defaultConfDir.toString,defaultFileName)
                                                           val flatFile = dnsFilePath.toFile
                                                           if (!flatFile.exists()) {
                                                             val createdFile = Files.createFile(dnsFilePath)
                                                             createdFile.toString
                                                           } else {
                                                             flatFile.toString
                                                           }
                                                       }.get
    } else {
      filename
    }
  }
  dnsJsonFile = createIfNotExistsDataFile(dnsJsonFile,"dns.json")
  urlShortnerJsonFile = createIfNotExistsDataFile(urlShortnerJsonFile,"shortner.json")
}
@JsonIgnoreProperties(Array("config"))
case class Config(application: ApplicationConfig, dbConf:DBConfig, dnsConf:DNSConfig, firstStart:Boolean, config:com.typesafe.config.Config) {
  def this(appConf: ApplicationConfig,dbConf:DBConfig, dnsConfig:DNSConfig,firstStart:Boolean) {
    this(appConf, dbConf, dnsConfig,firstStart, Config.readConfig)
  }
}
object Config {
  val defaultConfDir = Paths.get(System.getProperty("user.home") + File.separator + ".dnsserver")
  val configFilePath = Paths.get(defaultConfDir.toString,"application.json")
  val configFile = configFilePath.toFile
  var config =Option.empty[Config]

  private def initialiseConfigDir = {
    val default:com.typesafe.config.Config=ConfigFactory.load()
    val firstTime = !configFile.exists()
    if (firstTime) {
      Try(Files.createFile(configFilePath))
      Utils.putJsonFileContents(configFile,getConfigFromTypeSafeConfig(default).copy(firstStart = true))
    }
  }
  protected def readConfig = {
    ConfigFactory.invalidateCaches()
    val default:com.typesafe.config.Config=ConfigFactory.load()
    val fileConfig = Try(ConfigFactory.parseFile(configFile)).getOrElse(ConfigFactory.load())
    val cfg = fileConfig.withFallback(default)
    cfg
  }
  private def saveConfig(conf: Config) = {
    Utils.putJsonFileContents(configFile,conf)
  }
  def getConfig = {
    config.getOrElse(getNewConfig)
  }
  private def getNewConfig = {
    val cfg = readConfig
    config = Option(getConfigFromTypeSafeConfig(cfg))
    config.get
  }

  def getDefaultConfig:Config = {
    val default:com.typesafe.config.Config=ConfigFactory.load()
    getConfigFromTypeSafeConfig(default)
  }

  private def getConfigFromTypeSafeConfig(cfg:com.typesafe.config.Config):Config = {
    val dbConf :DBConfig=cfg.get[DBConfig]("dbConf").value
    val dnsConf = cfg.get[DNSConfig]("dnsConf").value
    val applicationConfig = cfg.get[ApplicationConfig]("application").value
    val firstStart = cfg.get[Boolean]("firstStart").value
    Config(applicationConfig,dbConf,dnsConf,firstStart,cfg)
  }
  def setConfig(nconf: ConfigDto) = {
    val currentConfig = config.get
    val configToSave = copyIgnoreNullConfig(currentConfig,nconf)
    saveConfig(configToSave)
    config = Option(getNewConfig)
  }

  private def copyIgnoreNullConfig(oldConf: Config, newConf: ConfigDto):Config = {
    val napp =  newConf.application
    val ndb = newConf.dbConf
    val ndns = newConf.dnsConf
    oldConf.copy(application = oldConf.application.copy(napp.dnsJsonFile,napp.urlShortnerJsonFile,napp.storageMedium))
    .copy(dbConf = oldConf.dbConf.copy(ndb.user,ndb.password,ndb.url,ndb.dBName))
    .copy(dnsConf = oldConf.dnsConf.copy(ndns.dnsResolver,ndns.dnsResolverSecondLevel,ndns.maxEntries,ndns.entryExpiryTime))
    .copy(firstStart = newConf.firstStart)
  }

  initialiseConfigDir
}
