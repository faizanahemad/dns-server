package io.faizan.actor

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.agent.Agent
import akka.io.IO
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.github.mkroli.dns4s.Message
import com.github.mkroli.dns4s.akka.Dns
import com.github.mkroli.dns4s.dsl.{ARecord, _}
import com.github.mkroli.dns4s.section.QuestionSection
import com.github.mkroli.dns4s.section.resource.AResource
import io.faizan.model.DnsRecordsStorage
import io.faizan.{AppModuleSupport, ServerStatus, Utils}
import scaldi.{Injectable, Injector}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global

case class DnsAnswer(record: Message, rcode: Int, cached:Boolean)

class DnsHandlerActor(implicit inj: Injector) extends Actor with Injectable {
  val dnsDetails = inject[DnsRecordsStorage]
  val serverStatus = inject[Agent[ServerStatus]]
  val dnsResolverAddress = inject[InetSocketAddress](
    identified by AppModuleSupport.dnsResolverAddress)
  val dnsResolverAddressSecondStage = inject[InetSocketAddress](
    identified by AppModuleSupport.dnsResolverAddressSecondLevel)

  override def receive = {
    case x: Message =>
      val question = x.question.head
      val header = x.header
      val qname = question.qname
      val ans = fetchDNSAnswer(qname,question)
      val addToCache = dnsDetails.addToCache _
      val res = ans map(answer=>{
        val rd=answer.record.copy(header = header.copy(rcode = answer.rcode, qr = true, ra = true)) ~ Answers()
        if (!answer.cached && answer.rcode==0) {
          addToCache(qname,rd)
        }
        rd
      })
      res pipeTo sender
    case Dns.Bound =>
      serverStatus send ServerStatus(ServerStatus.RUNNING)
    case Dns.Unbound =>
      serverStatus send ServerStatus(ServerStatus.STOPPED)
  }


  private def fetchDNSAnswer(qname: String, question: QuestionSection): Future[DnsAnswer] = {
    val dnsAnswer = dnsDetails.getIfPresent(qname)
    dnsAnswer match {
      case Some(a)=>Future(DnsAnswer(a, 0, true))
      case None=>fetchFromDNSResolver(question,dnsResolverAddress,true)
    }
  }

  private def fetchFromDNSResolver(question: QuestionSection,address: InetSocketAddress, resolveRecursive:Boolean): Future[DnsAnswer] = {
    implicit val timeout = Timeout(500 millisecond)
    implicit val system = context.system
    val dnsRequest = Dns.DnsPacket(Query ~ Questions(question), address)
    val dnsResponseFuture = IO(Dns) ? dnsRequest
    dnsResponseFuture.flatMap(r=>{
      val resp = r.asInstanceOf[Message]
      if (resp.header.rcode != 0 && resp.header.rcode != 1 && resolveRecursive) {
        fetchFromDNSResolver(question,dnsResolverAddressSecondStage,false)
      } else {
        Future( DnsAnswer(resp, resp.header.rcode, false))
      }
    })
  }


}
