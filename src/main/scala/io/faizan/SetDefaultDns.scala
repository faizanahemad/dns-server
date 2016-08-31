package io.faizan

import com.vnetpublishing.java.suapp.{SU, SuperUserApplication}
import io.faizan.OS.OS

import scala.sys.process._

object OS extends Enumeration {
  type OS = OS.Value
  val OSX = Value("mac")
  val WINDOWS = Value("windows")
  val LINUX = Value("linux")
}
class SetDefaultDns extends SuperUserApplication {
  override def run(args: Array[String]): Int = 0

  val osToChangerMap = Map[OS,()=>Unit](OS.OSX->setForMac)

  def setDnsToLocalHost() = {
    val os = OS.withName(SU.getOS)
    osToChangerMap.get(os).foreach(f=>f())
  }

  def setForMac(): Unit = {
    val nameServersArray = "cat /etc/resolv.conf".!!
                            .split(System.lineSeparator())
                            .filter(_.startsWith("nameserver"))
                            .map(str=>str.split(" ")(1))

    val networkInterfaces = "networksetup -listallnetworkservices".!!
                            .split(System.lineSeparator())

    val nameServersNew = "127.0.0.1"+: nameServersArray.toList

    val commands = networkInterfaces.map(i=>"networksetup -setdnsservers %s %s".format(i,nameServersNew.mkString(" ")))
    commands.map(_.!!)
  }
}
