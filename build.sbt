name := "bot-mesr"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

scalaVersion := "2.12.8"

libraryDependencies += "com.bot4s" %% "telegram-core" % "4.0.0-RC2" withSources ()
libraryDependencies += "com.bot4s" %% "telegram-akka" % "4.0.0-RC2" withSources ()
libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.4.0"
libraryDependencies += "net.debasishg" %% "redisclient" % "3.4"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.19"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.19"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.19"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.5.19"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-sharding" % "2.5.19"

dockerBaseImage := "dockerproxy.bale.ai/openjdk:8"
packageName in Docker := "docker.bale.ai/hackathon/mesr"
version in Docker := (version in ThisBuild).value
dockerExposedPorts := Seq()
dockerUpdateLatest := true
logBuffered in Test := false