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

case class DnsAnswer(record: Option[ResourceRecordModifier], rcode: Int, cached:Boolean)

class DnsHandlerActor(implicit inj: Injector) extends Actor with Injectable {
  val dnsDetails = inject[DnsRecordsStorage]
  val serverStatus = inject[Agent[ServerStatus]]
  val dnsResolverAddress = inject[InetSocketAddress](
    identified by AppModuleSupport.dnsResolverAddress)

  override def receive = {
    case x: Message =>
      val question = x.question.head
      val header = x.header
      val qname = Utils.urlToDomainName(question.qname)
      val ans = fetchDNSAnswer(qname,question)
      val cache = dnsDetails.cache
      val res = ans map(answer=>{
        if (!answer.cached && answer.record.isDefined) {
          cache.put(qname, answer.record.get)
        }
        val records = answer.record.toList
        val resp = x.copy(header = header.copy(rcode = answer.rcode))
        val answers = Answers(records: _*)
        Response(resp) ~ answers
      })
      res pipeTo sender
    case Dns.Bound =>
      serverStatus send ServerStatus(ServerStatus.RUNNING)
    case Dns.Unbound =>
      serverStatus send ServerStatus(ServerStatus.STOPPED)
  }


  private def fetchDNSAnswer(qname: String, question: QuestionSection): Future[DnsAnswer] = {
    val dnsAnswer = dnsDetails.getIfPresent(qname)
    if (dnsAnswer.isEmpty) {
      fetchFromDNSResolver(question)
    }
    else {
      Future(DnsAnswer(dnsAnswer, 0, true))
    }
  }

  private def fetchFromDNSResolver(question: QuestionSection): Future[DnsAnswer] = {
    implicit val timeout = Timeout(500 millisecond)
    implicit val system = context.system
    val dnsRequest = Dns.DnsPacket(Query ~ Questions(question), dnsResolverAddress)
    val dnsResponseFuture = IO(Dns) ? dnsRequest
    dnsResponseFuture.map(r => DnsHandlerActor.getDnsAnswerFromResponse(r.asInstanceOf[Message]))
  }


}

object DnsHandlerActor {
  def getDnsAnswerFromResponse(dnsResponse: Message): DnsAnswer = {
    var result: Option[ResourceRecordModifier] = Option.empty
    if (dnsResponse.header.rcode == 0) {
      result = Option(RRName(dnsResponse.question.head.qname) ~ ARecord(
        dnsResponse.answer.head.rdata.asInstanceOf[AResource].address))
    }
    DnsAnswer(result, dnsResponse.header.rcode, false)
  }
}
