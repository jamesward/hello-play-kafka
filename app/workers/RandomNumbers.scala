package workers

import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import play.api.{Configuration, Environment}
import play.api.inject.guice.GuiceApplicationBuilder
import services.Kafka

import scala.concurrent.duration._
import scala.util.Random

// Every 500 millis send a random number to Kafka
object RandomNumbers extends App {

  val config = Configuration.load(Environment.simple())
  val app = GuiceApplicationBuilder(configuration = config).build()

  val kafka = app.injector.instanceOf[Kafka]

  val tickSource = Source.tick(Duration.Zero, 500.milliseconds, Unit).map(_ => Random.nextInt().toString)

  tickSource
    .map(new ProducerRecord[String, String]("RandomNumbers", _))
    .to(kafka.sink)
    .run()(app.materializer)

}
