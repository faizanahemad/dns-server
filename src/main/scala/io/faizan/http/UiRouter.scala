package io.faizan.http

import java.io.{InputStream, InputStreamReader}
import java.nio.charset.StandardCharsets

import de.neuland.jade4j.{Jade4J, JadeConfiguration}
import io.faizan.{AppModule, Utils}
import org.http4s.{HttpService, Response, StaticFile}
import org.http4s.MediaType._
import org.http4s.dsl.{->, Root, _}
import org.http4s.headers.`Content-Type`
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization

import scala.collection.JavaConverters._
import scalaz.concurrent.Task

object UiRouter {
  val config = new JadeConfiguration()
  config.setPrettyPrint(true)
  val loader = new JadeTemplateLoader(StandardCharsets.UTF_8)
  config.setTemplateLoader(loader)
  val template = config.getTemplate("jade/app.jade")
  val html = config.renderTemplate(template, Map[String,AnyRef]().asJava)

  val router = HttpService{
                            case GET -> Root =>
                              implicit val formats = Serialization.formats(NoTypeHints)
                              Ok(html)
                              .putHeaders(`Content-Type`(`text/html`))
                          }

  val resources = HttpService {
                              case req @ GET -> "angular2" /: path =>
                                val pathString = path.toString
                                val staticFile = StaticFile.fromResource("/angular2"+pathString, Some(req))
                                staticFile.fold(NotFound())(Task.now)
                              }
  //                              val resource = Option(this.getClass.getClassLoader.getResource("angular2"+path))
  //                                             .flatMap(url=>StaticFile.fromURL(url, Some(req)))
}
