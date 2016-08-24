package io.faizan.http

import io.faizan.config.Config
import io.faizan.model.DnsRecord
import io.faizan.{AppModule, DnsServer, ServerStatus, Utils}
import org.http4s.EntityEncoder._
import org.http4s.MediaType._
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.http4s.server.blaze._
import org.http4s.server.{Router, Server, ServerApp}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import scala.concurrent.Future
import scalaz.concurrent.Task

class HttpServer{

  private var serverOptional = Option.empty[DnsServer]

  def startServer = if (serverOptional.isEmpty) {
    val server = new DnsServer(AppModule.getConfig)
    serverOptional = Option(server)
    serverOptional.foreach(_.start)
    Ok(Utils.toJson(ServerStatus(ServerStatus.STARTING))).putHeaders(`Content-Type`(`application/json`))
  }
  else {
    MethodNotAllowed()
  }

  def stopServer = {
    serverOptional.foreach(_.shutdown)
    serverOptional = Option.empty[DnsServer]
    Ok(Utils.toJson(ServerStatus(ServerStatus.STOPPING))).putHeaders(`Content-Type`(`application/json`))
  }

  def restartServer = {
    stopServer
    startServer
  }

  def refreshCache = {
    serverOptional
    .map(server => {
      server.dnsRecordsStore.refresh
      Ok().putHeaders(`Content-Type`(`application/json`))
    })
    .getOrElse(BadRequest())
  }

  val configService = HttpService {
                                    case GET -> Root =>
                                      implicit val formats = Serialization.formats(NoTypeHints)
                                      Ok(Utils.toJson(AppModule.getConfig))
                                      .putHeaders(`Content-Type`(`application/json`))
                                    case req@POST -> Root =>
                                      implicit val formats = Serialization.formats(NoTypeHints)
                                      req.as[String].map(body => {
                                        val newCon = Utils.fromJson[Config](body)
                                        AppModule.setConfig(newCon)
                                        if (serverOptional.isDefined) {
                                          restartServer
                                        }
                                        Ok().putHeaders(`Content-Type`(`application/json`))
                                      }).or(Task(BadRequest())).run
                                  }
  val adminService = HttpService {
                                   case GET -> Root / "status" =>
                                     implicit val formats = Serialization.formats(NoTypeHints)
                                     serverOptional
                                     .map(s => {
                                       Ok(Utils.toJson(s.status.get()))
                                       .putHeaders(`Content-Type`(`application/json`))
                                     })
                                     .getOrElse(Ok(Utils.toJson(ServerStatus(ServerStatus.STOPPED)))
                                                .putHeaders(`Content-Type`(`application/json`)))

                                   case GET -> Root / "stats" =>
                                     implicit val formats = Serialization.formats(NoTypeHints)
                                     serverOptional
                                     .map(s => {
                                       val status = "status" -> s.status.get().status.toString
                                       val cacheEntries = "cache" -> s.dnsDetails.getCachedMap.size()
                                       val storedEntires = "store" -> s.dnsDetails.getDnsMap.size
                                       val response = Map(status,cacheEntries,storedEntires)
                                       Ok(Utils.toJson(response))
                                       .putHeaders(`Content-Type`(`application/json`))
                                     })
                                     .getOrElse(Ok(Utils.toJson(ServerStatus(ServerStatus.STOPPED)))
                                                .putHeaders(`Content-Type`(`application/json`)))
                                   case req@PUT -> Root / "start" =>
                                     startServer
                                   case req@PUT -> Root / "stop" =>
                                     stopServer
                                   case req@PUT -> Root / "restart" =>
                                     restartServer
                                   case req@PUT -> Root / "refresh" =>
                                     refreshCache
                                   case req@PUT -> Root / "exit" =>
                                     import scala.concurrent.ExecutionContext.Implicits.global
                                     Future {
                                              stopServer
                                              Thread.sleep(2000)
                                            } onComplete ((u) => System.exit(0))
                                     Ok(Utils.toJson(ServerStatus(ServerStatus.STOPPING)))
                                     .putHeaders(`Content-Type`(`application/json`))
                                 }
  val listingService = HttpService {
                                     case GET -> Root =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       serverOptional
                                       .map(server => Ok(Utils.toJson(server.dnsDetails.getDnsMap))
                                                      .putHeaders(
                                                        `Content-Type`(`application/json`)))
                                       .getOrElse(BadRequest())

                                     case req @ GET -> Root / "search" =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       req.params.get("domain").filter(_.length>3).map(domain=> {
                                         serverOptional
                                         .map(server => Ok(Utils.toJson(server.dnsDetails.searchDnsMap(domain)))
                                                        .putHeaders(
                                                          `Content-Type`(`application/json`)))
                                         .getOrElse(BadRequest())
                                       }).getOrElse(BadRequest())

                                     case req@PUT -> Root =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       req.as[String].map(body => {
                                         val newCon = read[Map[String, DnsRecord]](body)
                                         serverOptional
                                         .map(server => {
                                           if (server.dnsRecordsStore.addEntries(newCon)) {
                                             Ok(Utils.toJson(server.dnsDetails.getDnsMap))
                                             .putHeaders(`Content-Type`(`application/json`))
                                           }
                                           else {
                                             BadRequest()
                                           }
                                         })
                                         .getOrElse(BadRequest())
                                       }).or(Task(BadRequest())).run

                                     case req@DELETE -> Root =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       req.as[String].map(body => {
                                         val newCon = read[Array[String]](body)
                                         serverOptional
                                         .map(server => {
                                           if (newCon.length > 0) {
                                             if (server.dnsRecordsStore.removeEntries(newCon)) {
                                               Ok(Utils.toJson(server.dnsDetails.getDnsMap))
                                               .putHeaders(`Content-Type`(`application/json`))
                                             }
                                             else {
                                               BadRequest()
                                             }
                                           }
                                           else {
                                             if (server.dnsRecordsStore.removeAllEntries()) {
                                               Ok(Utils.toJson(server.dnsDetails.getDnsMap))
                                               .putHeaders(`Content-Type`(`application/json`))
                                             }
                                             else {
                                               BadRequest()
                                             }
                                           }
                                         })
                                         .getOrElse(BadRequest())
                                       }).or(Task(BadRequest())).run
                                   }

  def router: HttpService = Router(
    "/config" -> configService,
    "/admin" -> adminService,
    "/list" -> listingService,
    "" -> UiRouter.router
  )
}
