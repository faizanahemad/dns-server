package io.faizan

import java.util.concurrent.Executors

import com.vnetpublishing.java.suapp.SuperUserApplication
import io.faizan.http.HttpServer
import org.http4s.Uri
import org.http4s.client._
import org.http4s.client.blaze.{defaultClient => client}
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.{Server, ServerApp}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import scalaz.concurrent.Task

object Main extends SuperUserApplication with ServerApp {
  import scala.concurrent.ExecutionContext.Implicits.global

  val sleepBase = 300L
  val splash = new SplashDisplay("DNS Server")
  splash.render("Checking Sudo/Root Permissions",10)
  val systemExit = (msg:String)=> {
    splash.render(msg,10)
    Thread.sleep(sleepBase*3)
    splash.stop()
    System.exit(1)
  }
  val sudoNeeded = !Try(System.getProperty("dev").toBoolean).getOrElse(false)
  var httpPort=8080
  val sudoAllowed = isSuperUser
  if (sudoNeeded && !sudoAllowed) {
    systemExit("Sudo/Root Perms Not present. Exiting")
  } else if(sudoAllowed) {
    httpPort = 80
    new SetDefaultDns().setDnsToLocalHost()
  }

  splash.render("Starting DNS Server",20)
  val httpServer = new HttpServer

  val dnsFuture = Future {
                           httpServer.startServer
                                    }

  override def server(args: List[String]): Task[Server] = {

    implicit val scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime.availableProcessors())



    Thread.sleep(sleepBase)
    splash.render("Starting Http Server",40)


    val reqRunningStatus = GET(Uri.fromString("http://localhost:%s/server/admin/status".format(httpPort)).toOption.get)
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
                            }.after(1000 millisecond)

    val startTask = BlazeBuilder.bindHttp(httpPort)
                    .mountService(httpServer.router).start.map(s => {
      dnsFuture.onComplete(v=>{
        splash.render("Pinging Server for Status",60)
        Try(afterRunCall.run).recover{
                                       case _=>systemExit("Unable to start. Terminating Program")
                                     }.map(f=>{
          splash.render("Go to http://localhost:%s/server/ui".format(httpPort),100)
          Thread.sleep(sleepBase*2)
          splash.stop()
        })
      })
      s
    })
    startTask
  }

  override def run(args: Array[String]): Int = 0
}
