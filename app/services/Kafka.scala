package services

import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, Subscription, Subscriptions}
import akka.stream.scaladsl.{Sink, Source}
import com.github.jkutner.EnvKeyStore
import com.typesafe.config.Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.Configuration


trait Kafka {
  def sink: Sink[ProducerRecord[String, String], _]
  def source(topic: String, maybeOffset: Option[Long]): Source[ConsumerRecord[String, String], _]
}

@Singleton
class KafkaImpl @Inject() (configuration: Configuration) extends Kafka {

  import akka.kafka.ProducerSettings
  import org.apache.kafka.common.serialization.StringSerializer

  lazy val sslConfig = {
    val envTrustStore = EnvKeyStore.createWithRandomPassword("KAFKA_TRUSTED_CERT")
    val envKeyStore = EnvKeyStore.createWithRandomPassword("KAFKA_CLIENT_CERT_KEY", "KAFKA_CLIENT_CERT")

    val trustStore = envTrustStore.storeTemp()
    val keyStore = envKeyStore.storeTemp()

    Configuration(
      "kafka-clients" -> Map(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG -> "SSL",
        SslConfigs.SSL_CLIENT_AUTH_CONFIG -> "required",
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG -> envTrustStore.`type`(),
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG -> trustStore.getAbsolutePath,
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG -> envTrustStore.password(),
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG -> envKeyStore.`type`(),
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG -> keyStore.getAbsolutePath,
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG -> envKeyStore.password
      )
    )
  }

  lazy val maybeKafkaUrl: Option[String] = {
    configuration.getString("kafka.url").map { kafkaUrl =>
      import java.net.URI

      val kafkaUrls = kafkaUrl.split(",").map { urlString =>
        val uri = new URI(urlString)
        Seq(uri.getHost, uri.getPort).mkString(":")
      }

      kafkaUrls.mkString(",")
    }
  }

  def kafkaUrl: String = maybeKafkaUrl.getOrElse("localhost:9092")

  def kafkaConfig(configKey: String): Config = {
    val baseConfig = configuration.getConfig(configKey).getOrElse(Configuration.empty)
    val config = maybeKafkaUrl.fold(baseConfig) { _ => baseConfig ++ sslConfig }
    config.underlying
  }

  def producerSettings: ProducerSettings[String, String] = {
    val serializer = new StringSerializer()
    ProducerSettings[String, String](kafkaConfig("akka.kafka.producer"), serializer, serializer).withBootstrapServers(kafkaUrl)
  }

  def consumerSettings: ConsumerSettings[String, String] = {
    val deserializer = new StringDeserializer()
    ConsumerSettings(kafkaConfig("akka.kafka.consumer"), deserializer, deserializer)
      .withBootstrapServers(kafkaUrl)
      .withGroupId(UUID.randomUUID().toString)
  }

  def sink: Sink[ProducerRecord[String, String], _] = {
    Producer.plainSink(producerSettings)
  }

  def source(topic: String, maybeOffset: Option[Long]): Source[ConsumerRecord[String, String], _] = {
    val subscriptions = maybeOffset.fold[Subscription](Subscriptions.topics(topic)) { offset =>
      Subscriptions.assignmentWithOffset(new TopicPartition(topic, 0), offset)
    }
    Consumer.plainSource(consumerSettings, subscriptions)
  }

}
