lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "hello-play-kafka"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.19",
  "com.heroku.sdk" % "env-keystore" % "1.0.2"
)
