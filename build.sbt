val akkaVersion = "2.6.8"

val `akka-sample-distributed-data-scala` = project
  .in(file("."))
  .enablePlugins(MultiJvmPlugin)
  .configs(MultiJvm)
  .settings(
    organization := "com.github.yoshiyoshifujii.akka.samples",
    version := "1.0",
    scalaVersion := "2.13.3",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m"),
    libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor-typed"           % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j"                 % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-typed"         % akkaVersion,
        "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
        "ch.qos.logback"     % "logback-classic"            % "1.2.3" excludeAll (
          ExclusionRule(organization = "org.slf4j")
        ),
        "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
        "com.typesafe.akka" %% "akka-multi-node-testkit"  % akkaVersion % Test,
        "org.scalatest"     %% "scalatest"                % "3.2.0"     % Test
      ),
    fork in run := true,
    Global / cancelable := false, // ctrl-c
    // disable parallel tests
    parallelExecution in Test := false,
    // show full stack traces and test case durations
    testOptions in Test += Tests.Argument("-oDF"),
    logBuffered in Test := false,
    licenses := Seq(("CC0", url("http://creativecommons.org/publicdomain/zero/1.0")))
  )
