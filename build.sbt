ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "zio_research",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.13",
      "dev.zio" %% "zio-http" % "0.0.5",
      "dev.zio" %% "zio-json" % "0.6.2",
      "dev.zio" %% "zio-logging" % "2.2.4",
      "dev.zio" %% "zio-test" % "2.0.13" % Test,
      "dev.zio" %% "zio-direct" % "1.0.0-RC7",
      "dev.zio" %% "zio-test-sbt" % "2.1.6" % Test
    )
  )
