name := "PactStubber"

version := "0.1"

scalaVersion := "2.12.4"

val scalaPactVersion = "2.2.3-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.itv" %% "scalapact-scalatest" % scalaPactVersion ,
  "com.itv" %% "scalapact-core" % scalaPactVersion ,
  "com.itv" %% "scalapact-shared" % scalaPactVersion,
  "com.itv" %% "scalapact-http4s-0-17-0" % scalaPactVersion ,
  "com.itv" %% "scalapact-argonaut-6-2" % scalaPactVersion
)