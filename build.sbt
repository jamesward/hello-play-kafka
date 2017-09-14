lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "hello-play-kafka"

scalaVersion := "2.12.3"

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.akka" %% "akka-stream-kafka" % "0.17",
  "com.heroku.sdk" % "env-keystore" % "1.0.0"
)

cancelable in Global := true
