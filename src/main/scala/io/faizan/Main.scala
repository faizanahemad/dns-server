package io.faizan

import java.util.concurrent.Executors

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

    implicit val scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors())
    val sleepBase = 40L
    val splash = new SplashDisplay("DNS Server")
    splash.render("Checking Sudo/Root Permissions",10)
    val systemExit = (msg:String)=> {
      splash.render(msg,10)
      Thread.sleep(sleepBase*2)
      splash.stop()
      System.exit(1)
    }
    val sudoNeeded = Option(System.getProperty("sudo")).forall(s=> Try(s.toBoolean).getOrElse(true))
    if (sudoNeeded && !isSuperUser) {
      systemExit("Sudo/Root Perms Not present. Exiting")
    }


    Thread.sleep(sleepBase)
    splash.render("Starting Http Server",20)


    val reqRunningStatus = GET(uri("http://localhost:8080/admin/status"))
    val afterRunCall = client.expect[String](reqRunningStatus)
                       .map((resp) => Utils.fromJson[ServerStatus](resp))
                       .map {
                              case e: ServerStatus if e == ServerStatus(ServerStatus.RUNNING) =>
                                splash.render("Server "+e.status,80)
                                Thread.sleep(sleepBase)
                                e
                              case f: ServerStatus =>
                                systemExit("Unable to start. Terminating Program")
                                f
                            }.after(6000 millisecond)


    val reqStartServer = PUT(uri("http://localhost:8080/admin/start"))
    val startCall = client.expect[String](reqStartServer)
                    .timed(6000)
                    .map((resp) => Utils.fromJson[ServerStatus](resp))
                    .map {
                           case e: ServerStatus if e == ServerStatus(ServerStatus.STARTING) => e
                           case f: ServerStatus =>
                             systemExit("Unable to start. Terminating Program")
                             f
                         }.map((s)=>{
      splash.render("Pinging Server for Status",60)
      afterRunCall.run
      splash.render("Go to http://localhost:8080/",100)
      Thread.sleep(sleepBase*3)
      splash.stop()
    }).timed(15000)

    val startTask = BlazeBuilder.bindHttp(8080)
                    .mountService(httpServer.router).start.map(s => {
      splash.render("Starting DNS Server",40)
      Try(startCall.run).recover{
                                  case _=>systemExit("Unable to start. Terminating Program")
                                }
      s
    })
    startTask
  }

  override def run(args: Array[String]): Int = 0
}
