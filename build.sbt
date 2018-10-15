name := "kafkaZIOProducer"

version := "0.1"

scalaVersion := "2.12.7"

val camelVersion = "2.22.1"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.2.25",
  "org.scalaz" %% "scalaz-zio" % "0.2.7",
  "org.scalaz" %% "scalaz-zio-interop" % "0.2.7"
  //"org.scalaz" %% "testz-core"         % "0.0.5",
  //"org.scalaz" %% "testz-stdlib"       % "0.0.5"
)

libraryDependencies ++= Seq(
  "org.apache.camel" % "camel-core" % camelVersion,
  "org.apache.camel" % "camel-kafka" % camelVersion,
)

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % "1.0.0",
  "co.fs2" %% "fs2-io" % "1.0.0"
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")