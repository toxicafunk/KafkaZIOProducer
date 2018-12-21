name := "kafkaZIOProducer"

version := "0.1"

scalaVersion := "2.12.7"

fork in run := true
javaOptions += "-Xms4G"
javaOptions += "-Xmx16G"

resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "1.0.1",
  "co.fs2" %% "fs2-io" % "1.1.0"
)

libraryDependencies += "org.typelevel" %% "cats-effect" % "1.1.0"

libraryDependencies += "net.cakesolutions" %% "scala-kafka-client" % "2.0.0"

mainClass in assembly := Some("com.eniro.KafkaProducer")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")