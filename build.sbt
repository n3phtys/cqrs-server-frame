name := "cqrs-server-frame"

version := "1.0"

scalaVersion := "2.11.8"


val akkaV = "2.4.11"
val scalaTestV = "3.0.0"

val actualProtocolurl =  "https://github.com/n3phtys/protocol-vanilla-solar.git"
val genericProtocolurl =  "https://github.com/n3phtys/cqrs-dual-frame.git"
val openidhelperurl = "https://nephtys@bitbucket.org/nephtys/scala-openidconnect-authentication-helpers.git"

lazy val openidhelperProject = RootProject(uri(openidhelperurl))
lazy val genericProtocolProject = ProjectRef(uri(genericProtocolurl), "cqrsdualframeJVM")
lazy val actualProtocolProject = ProjectRef(uri(actualProtocolurl), "solarprotocolJVM")

lazy val root = Project("cqrs-server-frame", file("."))
  .settings(
    publish := {},
    publishLocal := {}
  )
  .dependsOn(genericProtocolProject, openidhelperProject, actualProtocolProject)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "upickle" % "0.4.3",
      "com.typesafe.akka" %% "akka-actor" % akkaV,
      "com.typesafe.akka" %% "akka-stream" % akkaV,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaV,
      "org.scalatest"     %% "scalatest" % scalaTestV % "test",
      "com.typesafe.akka" %% "akka-cluster-sharding"  % akkaV,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaV,
      "com.typesafe.akka" %% "akka-persistence-query-experimental" % "2.4.11",
      "com.typesafe.akka" %% "akka-persistence" % "2.4.11",
      "org.iq80.leveldb"            % "leveldb"          % "0.7",
      "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
      "com.typesafe" % "config" % "1.3.1",
      "com.github.cb372" %% "scalacache-caffeine" % "0.9.3",
      "com.lihaoyi" %% "upickle" % "0.4.3",
      "com.google.code.findbugs" % "jsr305" % "3.0.1" % "compile"
    ),
    scalacOptions ++= Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-target:jvm-1.8", "-unchecked",
      "-Ywarn-adapted-args", "-Ywarn-value-discard", "-Xlint"),
    javacOptions ++= Seq("-Xlint:deprecation", "-Xlint:unchecked", "-source", "1.8", "-target", "1.8", "-g:vars")
  )
