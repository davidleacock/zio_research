ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "zio_research",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.8",
      "dev.zio" %% "zio-kafka" % "2.8.2",
      "dev.zio" %% "zio-http" % "3.0.0-RC9",
      "dev.zio" %% "zio-json" % "0.6.2",
      "dev.zio" %% "zio-logging" % "2.3.1",
      "dev.zio" %% "zio-test" % "2.1.8" % Test,
      "dev.zio" %% "zio-direct" % "1.0.0-RC7",
      "dev.zio" %% "zio-test-sbt" % "2.1.6" % Test,
      "dev.zio" %% "zio-schema"          % "1.4.1",
      "dev.zio" %% "zio-sql-postgres" % "0.1.2",

      "io.getquill"          %% "quill-jdbc-zio" % "4.8.4",
      "org.postgresql" % "postgresql" % "42.7.3",
      "com.dimafeng" %% "testcontainers-scala-kafka" % "0.41.4" % Test
    )
  )
