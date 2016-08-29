package io.faizan.config

import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import io.faizan.Utils

import scala.util.Try


case class DBConfigDto(user:String
                    , password:String
                    , url:String
                    , dBName:String) {

}

case class DNSConfigDto(dnsResolver:String,dnsResolverSecondLevel:String, maxEntries:Int, entryExpiryTime:Int)

case class ApplicationConfigDto(var dnsJsonFile:String,var urlShortnerJsonFile:String,@JsonScalaEnumeration(classOf[StorageMediumType]) storageMedium:StorageMedium.Value)

case class ConfigDto(application: ApplicationConfigDto, dbConf:DBConfigDto, dnsConf:DNSConfigDto, firstStart:Boolean)
