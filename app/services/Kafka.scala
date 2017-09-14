package services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Sink, Source}
import com.github.jkutner.EnvKeyStore
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.Configuration

import scala.util.{Failure, Success, Try}

trait Kafka {
  val maybePrefix: Option[String]
  def sink: Try[Sink[ProducerRecord[String, String], _]]
  def source(topic: String): Try[Source[ConsumerRecord[String, String], _]]
}

@Singleton
class KafkaImpl @Inject() (configuration: Configuration) extends Kafka {

  import akka.kafka.ProducerSettings
  import org.apache.kafka.common.serialization.StringSerializer

  lazy val envTrustStore = EnvKeyStore.createWithRandomPassword("KAFKA_TRUSTED_CERT")
  lazy val envKeyStore = EnvKeyStore.createWithRandomPassword("KAFKA_CLIENT_CERT_KEY", "KAFKA_CLIENT_CERT")

  lazy val maybePrefix = configuration.getString("kafka.prefix")

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

  def consumerSettings: Try[ConsumerSettings[String, String]] = {
    maybeKafkaUrl { kafkaUrl =>
      val deserializer = new StringDeserializer()
      val config = configuration.getConfig("akka.kafka.consumer").getOrElse(Configuration.empty) ++ sslConfig
      ConsumerSettings(config.underlying, deserializer, deserializer)
        .withBootstrapServers(kafkaUrl)
        .withGroupId(UUID.randomUUID().toString)
    }
  }

  def sink: Try[Sink[ProducerRecord[String, String], _]] = {
    producerSettings.map(Producer.plainSink(_))
  }

  def source(topic: String): Try[Source[ConsumerRecord[String, String], _]] = {
    val topicWithPrefix = maybePrefix.getOrElse("") + topic
    val subscriptions = Subscriptions.topics(topic)
    consumerSettings.map(Consumer.plainSource(_, subscriptions))
  }

}
