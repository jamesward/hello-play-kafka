name := "hello-play-kafka"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).disablePlugins(PlayLogback)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.kafka" %% "kafka" % "0.10.0.1",
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.13",
  "com.github.jkutner" % "env-keystore" % "0.1.2"
  //"org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.4.1"
)

excludeDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic"
)

cancelable in Global := true

val startKafka = TaskKey[Unit]("start-kafka")

startKafka := {
  (runMain in Compile).toTask(" KafkaServer").value
}
