package io.faizan

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.agent.Agent
import akka.io.IO
import akka.util.Timeout
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.github.mkroli.dns4s.akka.Dns
import io.faizan.config.{Config, StorageMedium, StorageMediumType}
import io.faizan.model.{DnsRecordsStorage, RedirectRecordsStorage}
import scaldi.Injectable

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.Try

class ServerStatusType extends TypeReference[ServerStatus.type]
object ServerStatus extends Enumeration {
  val NOT_STARTED, STARTING, RUNNING, STOPPING, STOPPED = Value
}
case class ServerStatus(@JsonScalaEnumeration(classOf[ServerStatusType]) status: ServerStatus.Value)

class DnsServer(config: Config) extends Injectable {

  import scala.concurrent.ExecutionContext.Implicits.global

  private implicit val injector = AppModule.getNewInjector(config)
  private implicit val timeout = Timeout(1000 millisecond)
  private implicit val system = inject[ActorSystem]
  private val dnsHandlerActor = inject[ActorRef](identified by AppModuleSupport.dnsHandlerActor)
  private val manager = IO(Dns)
  var status = inject[Agent[ServerStatus]]
  val dnsRecordsStore = inject[DnsRecordsStorage]
  val redirectRecordsStore = inject[RedirectRecordsStorage]
  val dnsPort = if (Try(System.getProperty("dev").toBoolean).getOrElse(false)) 5354 else 53

  def start = {
    val statusFuture = status.future().flatMap {
                                                 case ServerStatus(ServerStatus.NOT_STARTED) | ServerStatus(ServerStatus.STOPPED) =>
                                                   manager ! Dns.Bind(dnsHandlerActor, dnsPort)
                                                   status alter ServerStatus(ServerStatus.STARTING)
                                               }

    Await.result(statusFuture, Duration(6000, TimeUnit.MILLISECONDS))
  }

  def shutdown = {
    val statusFuture = status.future().flatMap {
                                                 case ServerStatus(ServerStatus.RUNNING) =>
                                                   manager ! Dns.Unbind
                                                   status alter ServerStatus(ServerStatus.STOPPING)
                                               }

    Await.result(statusFuture, Duration(2000, TimeUnit.MILLISECONDS))
    statusFuture.onComplete(r => injector.destroy())
  }
}
