name := "kafkaZIOProducer"

version := "0.1"

scalaVersion := "2.12.7"

fork in run := true
javaOptions += "-Xms4G"
javaOptions += "-Xmx16G"
//javaOptions += "-agentpath:/usr/share/visualvm/profiler/lib/deployed/jdk16/linux-amd64/libprofilerinterface.so=/usr/share/visualvm/profiler/lib,5140"

resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

val zioVersion = "0.5.3"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.25",
  "org.scalaz" %% "scalaz-zio" % zioVersion,
  "org.scalaz" %% "scalaz-zio-interop" % "0.5.0"
)

libraryDependencies += "net.cakesolutions" %% "scala-kafka-client" % "2.0.0"

/*
 * sbt assembly
 * /opt/graalvm/bin/native-image --allow-incomplete-classpath -jar kafkaZIOProducer-assembly-0.1.jar 
 */
mainClass in assembly := Some("com.eniro.KafkaZIOProducer")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")