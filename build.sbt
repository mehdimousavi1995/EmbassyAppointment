
name := "bot-mesr"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

scalaVersion := "2.12.8"


libraryDependencies ++= Seq(
  "com.bot4s" %% "telegram-core" % "4.0.0-RC2" withSources(),
  "com.bot4s" %% "telegram-akka" % "4.0.0-RC2" withSources(),
  "ch.megard" %% "akka-http-cors" % "0.4.0",
  "net.debasishg" %% "redisclient" % "3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.19",
  "com.typesafe.akka" %% "akka-actor" % "2.5.19",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.19",
  "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.19",
  "org.flywaydb" % "flyway-core" % "4.2.0",
  "com.typesafe.slick" %% "slick" % "3.2.3",
  "com.github.tminglei" %% "slick-pg" % "0.16.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
  "org.postgresql" % "postgresql" % "42.2.1",
  "tyrex" % "tyrex" % "1.0.1",
  "org.typelevel" %% "cats-core" % "1.4.0",
  "org.typelevel" %% "cats-free" % "1.4.0",
  "com.github.kxbmap" %% "configs" % "0.4.4",
  "io.spray" %% "spray-json" % "1.3.5"

)

dockerBaseImage := "dockerproxy.bale.ai/openjdk:8"
packageName in Docker := "docker.bale.ai/hackathon/mesr"
version in Docker := (version in ThisBuild).value
dockerExposedPorts := Seq()
dockerUpdateLatest := true
logBuffered in Test := false


fork in run := false