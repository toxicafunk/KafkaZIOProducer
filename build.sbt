name := "KafkaFS2Producer"

version := "0.1"

scalaVersion := "2.12.7"

fork in run := true
javaOptions += "-Xms4G"
javaOptions += "-Xmx16G"

resolvers += Resolver.bintrayRepo("cakesolutions", "maven")

libraryDependencies += "co.fs2" %% "fs2-core" % "1.0.1"
libraryDependencies += "co.fs2" %% "fs2-io" % "1.0.1"

libraryDependencies += "org.typelevel" %% "cats-effect" % "1.1.0"

libraryDependencies += "net.cakesolutions" %% "scala-kafka-client" % "2.0.0"

mainClass in assembly := Some("com.eniro.KafkaFS2Producer")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")