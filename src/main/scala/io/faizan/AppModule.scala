package io.faizan

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.agent.Agent
import akka.routing.FromConfig
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import io.faizan.actor.DnsHandlerActor
import io.faizan.config.Config
import io.faizan.model.DnsRecordsStorage
import scaldi.{Module, MutableInjectorAggregation}

import scala.concurrent.ExecutionContext.Implicits.global


class AppModule(config: Config) extends Module {

  import AppModuleSupport._

  bind[Config] to config
  val system = ActorSystem("DnsServer", config.config)
  val serverStatus = Agent(ServerStatus(ServerStatus.NOT_STARTED))
  bind[Agent[ServerStatus]] to serverStatus
  bind[ActorSystem] to system destroyWith (_.terminate())
  bind[ObjectMapper] identifiedBy objectMapper to mapperIdentity
  bind[ActorRef] identifiedBy dnsHandlerActor to system.actorOf(Props(new DnsHandlerActor)
                                                                .withRouter(FromConfig()),
                                                                "configBasedRouter")
  bind[DnsRecordsStorage] to new DnsRecordsStorage
  bind[InetSocketAddress] identifiedBy dnsResolverAddress to new InetSocketAddress(
    config.dnsConfig.dnsResolver, 53)
}

object AppModule {
  private var injector = Option.empty[MutableInjectorAggregation]

  def getConfig = Config.getConfig

  def setConfig(config: Config) = Config.setConfig(config)

  def getCurrentInjector: MutableInjectorAggregation = {
    injector.get
  }

  def getNewInjector(config: Config): MutableInjectorAggregation = {
    injector = Option(new AppModule(getConfig) :: new DBModule(getConfig))
    injector.get
  }
}

object AppModuleSupport {

  val mapperIdentity = new ObjectMapper() with ScalaObjectMapper
  mapperIdentity.registerModule(DefaultScalaModule)
  mapperIdentity.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
  mapperIdentity.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
  mapperIdentity.enable(SerializationFeature.INDENT_OUTPUT)

  val dnsHandlerActor = "'DnsHandler"
  val dnsMapName = "'DnsMap"
  val objectMapper = "'CAMEL_OBJECT_MAPPER"
  val dnsResolverAddress = "'DnsResolver"
}
