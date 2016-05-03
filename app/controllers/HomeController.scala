package controllers

import javax.inject._

import akka.stream.scaladsl.{Flow, Sink}
import play.api.mvc.{Action, Controller, WebSocket}
import services.Kafka

import scala.concurrent.Future
import scala.util.{Failure, Success}


@Singleton
class HomeController @Inject() (kafka: Kafka) extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index(routes.HomeController.ws().webSocketURL()))
  }

  def ws = WebSocket.acceptOrResult[Any, String] { _ =>
    kafka.source(Set("RandomNumbers")) match {
      case Failure(e) =>
        Future.successful(Left(InternalServerError("Could not connect to Kafka")))
      case Success(source) =>
        val flow = Flow.fromSinkAndSource(Sink.ignore, source.map(_.value))
        Future.successful(Right(flow))
    }
  }

}
