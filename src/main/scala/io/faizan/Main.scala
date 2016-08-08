package io.faizan

import io.faizan.http.HttpServer
import org.http4s.client._
import org.http4s.client.blaze.{defaultClient => client}
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.{Server, ServerApp}

import scala.concurrent.duration._
import scalaz.concurrent.Task

object Main extends ServerApp {

  val httpServer = new HttpServer

  override def server(args: List[String]): Task[Server] = {
    val splash = new SplashDisplay("DNS Server")
    splash.render("Starting Http Server")
    val reqRunningStatus = GET(uri("http://localhost:8080/admin/status"))
    val afterRunCall = client.expect[String](reqRunningStatus)
                       .map((resp) => Utils.fromJson[ServerStatus](resp))
                       .map {
                              case e: ServerStatus if e == ServerStatus(ServerStatus.RUNNING) => e
                              case f: ServerStatus =>
                                System.exit(1)
                                f
                            }.after(10000 millisecond)
    val req = PUT(uri("http://localhost:8080/admin/start"))
    val startCall = client.expect[String](req).map((resp) => Utils.fromJson[ServerStatus](resp))
                    .map {
                           case e: ServerStatus if e == ServerStatus(ServerStatus.STARTING) => e
                           case f: ServerStatus =>
                             System.exit(1)
                             f
                         }.map((s)=>{
      splash.render("Checking for Server Run")
      afterRunCall.run
      splash.render("Go to http://localhost:8080/")
      Thread.sleep(1000)
      splash.stop()
    })

    val startTask = BlazeBuilder.bindHttp(8080)
                    .mountService(httpServer.router).start.map(s => {
      splash.render("Starting DNS Server")
      startCall.run
      s
    })
    startTask
  }
}
