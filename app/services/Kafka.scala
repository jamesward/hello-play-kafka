package services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.kafka.ConsumerSettings
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Sink, Source}
import com.github.jkutner.EnvKeyStore
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.Configuration

import scala.util.{Failure, Success, Try}

trait Kafka {
  def sink: Try[Sink[ProducerRecord[String, String], NotUsed]]
  def source(topics: Set[String]): Try[Source[ConsumerRecord[String, String], Consumer.Control]]
}

@Singleton
class KafkaImpl @Inject() (configuration: Configuration) extends Kafka {

  import akka.kafka.ProducerSettings
  import org.apache.kafka.common.serialization.StringSerializer

  lazy val envTrustStore = EnvKeyStore.createWithRandomPassword("KAFKA_TRUSTED_CERT")
  lazy val envKeyStore = EnvKeyStore.createWithRandomPassword("KAFKA_CLIENT_CERT_KEY", "KAFKA_CLIENT_CERT")

  lazy val trustStore = envTrustStore.storeTemp()
  lazy val keyStore = envKeyStore.storeTemp()

  lazy val sslConfig = {
    Configuration(
      "kafka-clients" -> Map(
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG -> envTrustStore.`type`(),
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG -> trustStore.getAbsolutePath,
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG -> envTrustStore.password(),
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG -> envKeyStore.`type`(),
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG -> keyStore.getAbsolutePath,
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG -> envKeyStore.password
      )
    )
  }

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
      val config = configuration.getConfig("akka.kafka.producer").getOrElse(Configuration.empty) ++ sslConfig
      ProducerSettings[String, String](config.underlying, serializer, serializer).withBootstrapServers(kafkaUrl)
    }
  }

  def consumerSettings(topics: Set[String]): Try[ConsumerSettings[String, String]] = {
    maybeKafkaUrl { kafkaUrl =>
      val deserializer = new StringDeserializer()
      val config = configuration.getConfig("akka.kafka.consumer").getOrElse(Configuration.empty) ++ sslConfig
      ConsumerSettings[String, String](config.underlying, deserializer, deserializer, topics)
        .withBootstrapServers(kafkaUrl)
        .withGroupId(UUID.randomUUID().toString)
    }
  }

  def sink: Try[Sink[ProducerRecord[String, String], NotUsed]] = {
    producerSettings.map(Producer.plainSink)
  }

  def source(topics: Set[String]): Try[Source[ConsumerRecord[String, String], Consumer.Control]] = {
    consumerSettings(topics).map(Consumer.plainSource)
  }

}
