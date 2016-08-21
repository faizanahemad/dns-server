package io.faizan.http

import java.io.{InputStream, InputStreamReader, Reader}
import java.nio.charset.Charset

import de.neuland.jade4j.template.TemplateLoader

class JadeTemplateLoader(charset: Charset) extends TemplateLoader{

  override def getLastModified(name: String): Long = 0

  override def getReader(name: String): Reader = {
    val CLDR = this.getClass.getClassLoader
    val stream = CLDR.getResourceAsStream(name)
    new InputStreamReader(stream, charset)
  }
}
