package io.faizan

import com.vnetpublishing.java.suapp.SuperUserApplication
import io.faizan.http.HttpServer
import org.http4s.client._
import org.http4s.client.blaze.{defaultClient => client}
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.{Server, ServerApp}

import scala.concurrent.duration._
import scala.util.Try
import scalaz.concurrent.Task

object Main extends SuperUserApplication with ServerApp {

  val httpServer = new HttpServer

  override def server(args: List[String]): Task[Server] = {
    val sudoNeeded = Option(System.getProperty("sudo")).forall(s=> Try(s.toBoolean).getOrElse(true))
    val splash = new SplashDisplay("DNS Server")
    splash.render("Checking Sudo/Root Permissions",10)
    Thread.sleep(500)
    if (sudoNeeded && !isSuperUser) {
      splash.render("Sudo/Root Perms Not present. Exiting",10)
      Thread.sleep(1000)
      System.exit(1)
    }
    splash.render("Starting Http Server",20)
    val reqRunningStatus = GET(uri("http://localhost:8080/admin/status"))
    val afterRunCall = client.expect[String](reqRunningStatus)
                       .map((resp) => Utils.fromJson[ServerStatus](resp))
                       .map {
                              case e: ServerStatus if e == ServerStatus(ServerStatus.RUNNING) =>
                                splash.render("Server "+e.status,80)
                                Thread.sleep(500)
                                e
                              case f: ServerStatus =>
                                splash.render("Unable to start. Terminating Program",0)
                                Thread.sleep(1000)
                                System.exit(1)
                                f
                            }.after(8000 millisecond)
    val req = PUT(uri("http://localhost:8080/admin/start"))
    val startCall = client.expect[String](req).map((resp) => Utils.fromJson[ServerStatus](resp))
                    .map {
                           case e: ServerStatus if e == ServerStatus(ServerStatus.STARTING) => e
                           case f: ServerStatus =>
                             System.exit(1)
                             f
                         }.map((s)=>{
      splash.render("Pinging Server for Status",60)
      afterRunCall.run
      splash.render("Go to http://localhost:8080/",100)
      Thread.sleep(1500)
      splash.stop()
    })

    val startTask = BlazeBuilder.bindHttp(8080)
                    .mountService(httpServer.router).start.map(s => {
      splash.render("Starting DNS Server",40)
      startCall.run
      s
    })
    startTask
  }

  override def run(args: Array[String]): Int = 0
}
