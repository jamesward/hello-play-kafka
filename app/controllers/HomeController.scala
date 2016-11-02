package controllers

import javax.inject._

import akka.stream.scaladsl.{Flow, Sink}
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{Action, Controller, WebSocket}
import services.Kafka


@Singleton
class HomeController @Inject() (kafka: Kafka) extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index(routes.HomeController.ws().webSocketURL()))
  }

  def ws = WebSocket.accept { request =>
    val maybeOffset = request.getQueryString("offset").map(_.toLong)
    val source = kafka.source("RandomNumbers", maybeOffset).map { consumerRecord =>
      Json.obj(consumerRecord.offset().toString -> consumerRecord.value())
    }
    Flow.fromSinkAndSource(Sink.ignore, source)
  }

}
