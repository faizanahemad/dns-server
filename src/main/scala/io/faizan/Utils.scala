package io.faizan

import java.io.File
import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets
import java.util
import java.util.regex.Pattern
import java.util.{Timer, TimerTask}

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.github.mkroli.dns4s.{Message, MessageBuffer}
import com.github.mkroli.dns4s.dsl.{ARecord, Answers, RRName, ResourceRecordModifier, Response}
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.akka.Dns
import com.github.mkroli.dns4s.dsl.{ARecord, _}
import com.github.mkroli.dns4s.section.QuestionSection
import com.google.common.base.CharMatcher
import io.faizan.config.DBConfig
import io.faizan.model.{DnsRecord, RecordType}
import org.apache.commons.io.{FileUtils, IOUtils}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.Try

object Utils {
  private val mapper = AppModuleSupport.mapperIdentity

  private val actionTimeout = 4 second

  def createDb(dbConfig: DBConfig) = {
    val dbName = dbConfig.dBName
    using(Database.forURL(dbConfig.url, user = dbConfig.user, password = dbConfig.password,
                          driver = dbConfig.driver)) { conn =>
      Await.result(conn.run(sqlu"CREATE DATABASE IF NOT EXISTS #$dbName"), actionTimeout)
                                                     }
  }

  private def using[A <: {def close() : Unit}, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      Try {
            resource.close()
          }.failed.foreach(err => throw new Exception(s"failed to close $resource", err))
    }


  def dnsToSanitizedDns(dns:String):String = {
    dns.toLowerCase.trim
  }
  def urlToRedirectUrl(url:String):String = {
    val result = url.toLowerCase.trim
    redirectUrlPattern.matcher(result).matches() match {
      case true=>result
      case false=> "http://"+result
    }

  }
  def urlToDomainName(qn: String): String = {
    var qname = qn.toLowerCase.trim
    if (qname.endsWith(".")) {
      qname = qname.substring(0, qname.length - 1)
    }

    if (qname.startsWith("www.")) {
      qname = qname.substring(4)
    } else if (qname.startsWith("http://")) {
      qname = qname.substring(7)
    }
    else if (qname.startsWith("https://")) {
      qname = qname.substring(8)
    }
    else if (qname.startsWith("ftp://")) {
      qname = qname.substring(6)
    }
    else if (qname.startsWith(".")) {
      qname = qname.substring(1)
    }

    qname
  }

  def checkFileValidity(file: String): Boolean = {
    val fl = new File(file)
    checkFileValidity(fl)
  }

  def checkFileValidity(file:File):Boolean = if (file.canRead) {
    true
  }
  else {
    false
  }

  def checkUrlValidity(uri: String): Boolean = {
    if (uri != null && uri.nonEmpty) {
      val resource = Try(new URL(uri))
      if (resource.isSuccess) {
        val connect = resource.get.openConnection().asInstanceOf[HttpURLConnection]
        connect.setRequestMethod("HEAD")
        val code = connect.getResponseCode
        if (code == HttpURLConnection.HTTP_OK) {
          return true
        }
        else {
          false
        }
      }
      else {
        checkFileValidity(uri)
      }
    }
    else {
      false
    }
  }

  def getJsonFileContents(uri: String): JsonNode = {
    checkUrlValidity(uri) match {
      case true=>
        val dnsConfigFileUri = Try(new URL(uri))
                               .getOrElse(new File(uri).toURI.toURL)
        val stringFromSource = IOUtils.toString(dnsConfigFileUri, StandardCharsets.UTF_8)
        val filteredString = CharMatcher.JAVA_ISO_CONTROL.removeFrom(stringFromSource)
        val dnsString = CharMatcher.anyOf("\r\n\t").removeFrom(filteredString)
                        .replaceAll("[\u0000-\u001f]", "")
                        .replaceAll("\\p{Cntrl}", "")
        Try(mapper.readTree(dnsString)).getOrElse(mapper.createObjectNode())
      case false=>mapper.createObjectNode()
    }
  }

  def putJsonFileContents(uri: String, content: AnyRef):Boolean = {
    putJsonFileContents(new File(uri),content)
  }

  def putJsonFileContents(uri: File, content: AnyRef):Boolean = {
    val jsonString = mapper.writeValueAsString(content)
    Try(FileUtils.writeStringToFile(uri, jsonString, StandardCharsets.UTF_8))
    .map((u)=>true).getOrElse(false)
  }

  def toJson(value: Any): String = {
    mapper.writeValueAsString(value)
  }

  def fromJson[T: ClassTag](json: String)(implicit m: Manifest[T]): T = {
    mapper.readValue[T](json)
  }

  def convertToDns4s(entries: Map[String, String]) = {
    import scala.collection.JavaConverters._
    val records = new util.HashMap[String, ResourceRecordModifier](128, 0.5f)
    records.putAll(entries.map(e => (Utils.urlToDomainName(e._1), e._2))
                   .map(e => (e._1, RRName(e._1) ~ ARecord(e._2))).toMap.asJava)
    records
  }

  def convertToDns4s(entry: DnsRecord):ComposableMessage = {
    val q = entry.recordType match {
      case RecordType.A=>QuestionSection(entry.domain,1,1)
      case RecordType.AAAA=>QuestionSection(entry.domain,28,1)
      case RecordType.CNAME=>QuestionSection(entry.domain,5,1)
      case RecordType.DNAME=>QuestionSection(entry.domain,39,1)
    }
    val rr=RRName(entry.domain) ~ ARecord(entry.dns)
    Message(MessageBuffer()).copy(question = Seq(q)) ~ Answers(rr)
  }
  def delay[T](delay: Long)(block: => T): Future[T] = {
    val promise = Promise[T]()
    val t = new Timer()
    t.schedule(new TimerTask {
      override def run(): Unit = {
        promise.complete(Try(block))
      }
    }, delay)
    promise.future
  }

  lazy val dnsPattern = Pattern.compile("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$")
  lazy val domainPattern = Pattern.compile("^([a-zA-Z0-9](?:(?:[a-zA-Z0-9-]*|(?<!-)\\.(?![-.]))*[a-zA-Z0-9]+)?)$")
  lazy val redirectUrlPattern = Pattern.compile("^(ht|f)tp(s?)\\:\\/\\/[0-9a-zA-Z]([-.\\w]*[0-9a-zA-Z])*(:(0-9)*)*(\\/?)([a-zA-Z0-9\\-\\.\\?\\,\\'\\/\\\\\\+&amp;%\\$#_]*)?$")
}
