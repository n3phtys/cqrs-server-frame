name := "cqrs-server-frame"

version := "1.0"

scalaVersion := "2.11.8"



val genericProtocolurl =  "https://github.com/n3phtys/cqrs-dual-frame.git"


lazy val genericProtocolProject = ProjectRef(uri(genericProtocolurl), "cqrsdualframeJVM")

lazy val root = Project("cqrs-server-frame", file("."))
  .settings(
    publish := {},
    publishLocal := {}
  )
  .dependsOn(genericProtocolProject)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % "0.4.3",
      "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11"
    ),
    scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.8", "-unchecked",
      "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint")
  )
