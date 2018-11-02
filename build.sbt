name := "kafkaZIOProducer"

version := "0.1"

scalaVersion := "2.12.7"

resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.25",
  "org.scalaz" %% "scalaz-zio" % "0.2.7",
  "org.scalaz" %% "scalaz-zio-interop" % "0.2.7"
  //"org.scalaz" %% "testz-core"         % "0.0.5",
  //"org.scalaz" %% "testz-stdlib"       % "0.0.5"
)

libraryDependencies += "net.cakesolutions" %% "scala-kafka-client" % "2.0.0"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")