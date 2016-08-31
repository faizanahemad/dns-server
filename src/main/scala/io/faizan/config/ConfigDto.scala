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
  require(Utils.alphaNumericUnderscoreRegex.matcher(dBName).matches())
}

case class DNSConfigDto(dnsResolver:String,dnsResolverSecondLevel:String, maxEntries:Int, entryExpiryTime:Int) {
  require(Utils.dnsPattern.matcher(dnsResolver).matches())
  require(Utils.dnsPattern.matcher(dnsResolverSecondLevel).matches())
}

case class ApplicationConfigDto(var dnsJsonFile:String,var urlShortnerJsonFile:String,@JsonScalaEnumeration(classOf[StorageMediumType]) storageMedium:StorageMedium.Value) {
  require(Utils.fileRegexPattern.matcher(dnsJsonFile).matches())
  require(Utils.fileRegexPattern.matcher(urlShortnerJsonFile).matches())
}

case class ConfigDto(application: ApplicationConfigDto, dbConf:DBConfigDto, dnsConf:DNSConfigDto, firstStart:Boolean)
