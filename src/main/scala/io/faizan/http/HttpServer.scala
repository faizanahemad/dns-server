package io.faizan.http

import io.faizan.config.{Config, ConfigDto}
import io.faizan.model.{DnsRecord, RedirectRecord}
import io.faizan.{AppModule, DnsServer, ServerStatus, Utils}
import org.http4s.MediaType._
import org.http4s.dsl._
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.server.Router
import org.http4s.{HttpService, _}
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.read

import scala.concurrent.Future
import scalaz.concurrent.Task

class HttpServer{

  private var serverOptional = Option.empty[DnsServer]

  def okJson(response:Any) = {
    Ok(Utils.toJson(response))
    .putHeaders(
      `Content-Type`(`application/json`))
  }

  def okJson = {
    Ok().putHeaders(
      `Content-Type`(`application/json`))
  }

  def startServer = if (serverOptional.isEmpty) {
    val server = new DnsServer(AppModule.getConfig)
    serverOptional = Option(server)
    serverOptional.foreach(_.start)
    serverOptional.foreach(s=>s.dnsRecordsStore.addEntries(Map("l"->DnsRecord("l","127.0.0.1"),
                                                               "f"->DnsRecord("f","127.0.0.1"))))
    okJson(ServerStatus(ServerStatus.STARTING))
  }
  else {
    MethodNotAllowed()
  }

  def stopServer = {
    serverOptional.foreach(_.shutdown)
    serverOptional = Option.empty[DnsServer]
    okJson(ServerStatus(ServerStatus.STOPPING))
  }

  def restartServer = {
    stopServer
    startServer
  }

  val configService = HttpService {
                                    case GET -> Root =>
                                      implicit val formats = Serialization.formats(NoTypeHints)
                                      okJson(AppModule.getConfig)

                                    case req@POST -> Root =>
                                      implicit val formats = Serialization.formats(NoTypeHints)
                                      req.as[String].map(body => {
                                        val newCon = Utils.fromJson[ConfigDto](body)
                                        AppModule.setConfig(newCon)
                                        if (serverOptional.isDefined) {
                                          restartServer
                                        }
                                        okJson
                                      }).or(Task(BadRequest())).run
                                  }
  val adminService = HttpService {
                                   case GET -> Root / "status" =>
                                     implicit val formats = Serialization.formats(NoTypeHints)
                                     serverOptional.map(s => okJson(s.status.get()))
                                     .getOrElse(okJson(ServerStatus(ServerStatus.STOPPED)))

                                   case req@PUT -> Root / "start" =>
                                     startServer
                                   case req@PUT -> Root / "restart" =>
                                     restartServer
                                   case req@PUT -> Root / "exit" =>
                                     import scala.concurrent.ExecutionContext.Implicits.global
                                     Future {
                                              stopServer
                                              Thread.sleep(2000)
                                            } onComplete ((u) => System.exit(0))
                                     okJson(ServerStatus(ServerStatus.STOPPING))
                                 }

  val dnsListingService = HttpService {
                                     case req @ GET -> Root =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       val params = req.params.get _

                                       serverOptional.map(server => {
                                         val dnsMap = params("domain") match {
                                           case Some(dom) => Option(dom).filter(_.length>3).map(domain=>server.dnsRecordsStore.searchMap(domain).values.toArray)
                                                             .getOrElse(Array[DnsRecord]())
                                           case None => server.dnsRecordsStore.getStorageMap.values.toArray
                                         }

                                         val sortedMap = params("sortby") match {
                                           case Some(dom) =>
                                             val order = params("order").getOrElse("asc")
                                             val sortedEntries = dnsMap.sortWith((e1,e2)=> dom match {
                                               case "domain"=>e1.domain.compareTo(e2.domain) < 0
                                               case "created_at"=>e1.createdAt.get.compareTo(e2.createdAt.get) <0
                                               case "updated_at"=>e1.updatedAt.get.compareTo(e2.updatedAt.get) <0
                                               case _=>e1.domain.compareTo(e2.domain) < 0
                                             })

                                             if (order=="asc")
                                               sortedEntries
                                             else
                                               sortedEntries.reverse

                                           case _ => dnsMap
                                         }
                                         val sortedPaginatedMap = params("pageNumber") match {
                                           case Some(page) =>
                                             val pageSize = params("pageSize").getOrElse("10").toInt
                                             sortedMap.slice((page.toInt -1)*pageSize,page.toInt*pageSize)
                                           case _ => sortedMap
                                         }
                                         okJson(sortedPaginatedMap)
                                       })
                                       .getOrElse(BadRequest())

                                     case GET -> Root / "count" =>
                                       serverOptional.map(server => {
                                         val response = Map("count"->server.dnsRecordsStore.getStorageMap.size)
                                         okJson(response)
                                       })
                                       .getOrElse(BadRequest())

                                     case req@PUT -> Root =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       req.as[String].map(body => {
                                         val newCon = read[Map[String, DnsRecord]](body)
                                         serverOptional
                                         .map(server => {
                                           server.dnsRecordsStore.addEntries(newCon) match {
                                             case true=>okJson(server.dnsRecordsStore.getStorageMap)
                                             case false=>BadRequest()
                                           }
                                         })
                                         .getOrElse(BadRequest())
                                       }).or(Task(BadRequest())).run

                                     case req@DELETE -> Root / domain =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       serverOptional
                                       .map(server => {
                                         if (domain.length > 0) {
                                           server.dnsRecordsStore.removeEntries(Array(domain)) match {
                                             case true=>okJson(server.dnsRecordsStore.getStorageMap)
                                             case false=>BadRequest()
                                           }
                                         }
                                         else {
                                           server.dnsRecordsStore.removeAllEntries() match {
                                             case true=>okJson(server.dnsRecordsStore.getStorageMap)
                                             case false=>BadRequest()
                                           }
                                         }
                                       })
                                       .getOrElse(BadRequest())

                                   }

  val redirectListingService = HttpService {
                                     case req @ GET -> Root =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       val params = req.params.get _

                                       serverOptional.map(server => {
                                         val redirectMap = params("domain") match {
                                           case Some(dom) => Option(dom).filter(_.length>3).map(domain=>server.redirectRecordsStore.searchMap(domain).values.toArray)
                                                             .getOrElse(Array[RedirectRecord]())
                                           case None => server.redirectRecordsStore.getStorageMap.values.toArray
                                         }

                                         val sortedMap = params("sortby") match {
                                           case Some(dom) =>
                                             val order = params("order").getOrElse("asc")
                                             val sortedEntries = redirectMap.sortWith((e1,e2)=> dom match {
                                               case "request_url"=>e1.requestUrl.compareTo(e2.requestUrl) < 0
                                               case "redirect_url"=>e1.redirectUrl.compareTo(e2.redirectUrl) < 0
                                               case "created_at"=>e1.createdAt.get.compareTo(e2.createdAt.get) <0
                                               case "updated_at"=>e1.updatedAt.get.compareTo(e2.updatedAt.get) <0
                                               case _=>e1.requestUrl.compareTo(e2.requestUrl) < 0
                                             })

                                             if (order=="asc")
                                               sortedEntries
                                             else
                                               sortedEntries.reverse

                                           case _ => redirectMap
                                         }
                                         val sortedPaginatedMap = params("pageNumber") match {
                                           case Some(page) =>
                                             val pageSize = params("pageSize").getOrElse("10").toInt
                                             sortedMap.slice((page.toInt -1)*pageSize,page.toInt*pageSize)
                                           case _ => sortedMap
                                         }
                                         okJson(sortedPaginatedMap)
                                       })
                                       .getOrElse(BadRequest())

                                     case GET -> Root / "count" =>
                                       serverOptional.map(server => {
                                         val response = Map("count"->server.redirectRecordsStore.getStorageMap.size)
                                         okJson(response)
                                       })
                                       .getOrElse(BadRequest())

                                     case req@PUT -> Root =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       req.as[String].map(body => {
                                         val newCon = read[Map[String, RedirectRecord]](body)
                                         serverOptional
                                         .map(server => {
                                           server.redirectRecordsStore.addEntries(newCon) match {
                                             case true=>okJson(server.redirectRecordsStore.getStorageMap)
                                             case false=>BadRequest()
                                           }
                                         })
                                         .getOrElse(BadRequest())
                                       }).or(Task(BadRequest())).run

                                     case req@DELETE -> Root / requestUrl =>
                                       implicit val formats = Serialization.formats(NoTypeHints)
                                       serverOptional
                                       .map(server => {
                                         if (requestUrl.length > 0) {
                                           server.redirectRecordsStore.removeEntries(Array(requestUrl)) match {
                                             case true=>okJson(server.redirectRecordsStore.getStorageMap)
                                             case false=>BadRequest()
                                           }
                                         }
                                         else {
                                           server.redirectRecordsStore.removeAllEntries() match {
                                             case true=>okJson(server.redirectRecordsStore.getStorageMap)
                                             case false=>BadRequest()
                                           }
                                         }
                                       })
                                       .getOrElse(BadRequest())

                                   }

  val redirectService = {
    def getRedirectUrl(path:String) = {
      serverOptional
      .map(server => {
        server.redirectRecordsStore.getIfPresent(path).flatMap(rr=>Uri.fromString(rr).toOption) match {
          case Some(rr)=>PermanentRedirect(uri("")).putHeaders(
            Location(rr)
          )
          case None=>NotFound()
        }
      })
      .getOrElse(BadRequest())
    }
    HttpService {
                  case GET -> Root =>
                    PermanentRedirect(uri("server/ui"))
                  case GET -> Root / path =>
                    getRedirectUrl(path)

                  case POST -> Root / path =>
                    getRedirectUrl(path)

                  case PUT -> Root / path =>
                    getRedirectUrl(path)
                }
  }

  def router: HttpService = {
    val serverRouter = Router(
      "/config" -> configService,
      "/admin" -> adminService,
      "/list/dns" -> dnsListingService,
      "/list/redirect" -> redirectListingService,
      "" -> UiRouter.resources,
      "/ui" -> UiRouter.router,
      "/dns" -> UiRouter.router,
      "/help" -> UiRouter.router,
      "/settings" -> UiRouter.router,
      "/redirect" -> UiRouter.router
    )
    Router(
      "/server" -> serverRouter,
      "/" -> redirectService
    )
  }
}
