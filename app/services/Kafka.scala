package services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ConsumerSettings
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.Configuration

import scala.util.{Failure, Success, Try}

trait Kafka {
  def sink(topic: String): Try[Sink[ProducerRecord[String, String], NotUsed]]
  def source(topics: Set[String]): Try[Source[ConsumerRecord[String, String], Consumer.Control]]
}

@Singleton
class KafkaImpl @Inject() (actorSystem: ActorSystem, configuration: Configuration) extends Kafka {

  import akka.kafka.ProducerSettings
  import org.apache.kafka.common.serialization.StringSerializer

  def maybeKafkaUrl[K](f: String => K): Try[K] = {
    configuration.getString("kafka.url").fold[Try[K]] {
      Failure(new Error("kafka.url was not set"))
    } { kafkaUrl =>
      import java.net.URI

      val kafkaUrls = kafkaUrl.split(",").map { urlString =>
        val uri = new URI(urlString)
        Seq(uri.getHost, uri.getPort).mkString(":")
      }

      Success(f(kafkaUrls.mkString(",")))
    }
  }

  def producerSettings: Try[ProducerSettings[String, String]] = {
    maybeKafkaUrl { kafkaUrl =>
      val serializer = new StringSerializer()
      ProducerSettings[String, String](actorSystem, serializer, serializer).withBootstrapServers(kafkaUrl)
    }
  }

  def consumerSettings(topics: Set[String]): Try[ConsumerSettings[String, String]] = {
    maybeKafkaUrl { kafkaUrl =>
      val deserializer = new StringDeserializer()
      ConsumerSettings[String, String](actorSystem, deserializer, deserializer, topics)
        .withBootstrapServers(kafkaUrl)
        .withGroupId(UUID.randomUUID().toString)
    }
  }

  def sink(topic: String): Try[Sink[ProducerRecord[String, String], NotUsed]] = {
    producerSettings.map(Producer.plainSink)
  }

  def source(topics: Set[String]): Try[Source[ConsumerRecord[String, String], Consumer.Control]] = {
    consumerSettings(topics).map(Consumer.plainSource)
  }

}