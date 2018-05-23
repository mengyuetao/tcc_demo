import scala.language.reflectiveCalls
import scoverage.ScoverageKeys

concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)

val releaseVersion = "18.1.0"

lazy val buildSettings = Seq(
  version := releaseVersion,
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.11", "2.12.4"),
  scalaModuleInfo := scalaModuleInfo.value.map(_.withOverrideScalaVersion(true)),
  fork in Test := true,
  javaOptions in Test ++= travisTestJavaOptions
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  // sbt-pgp's publishSigned task needs this defined even though it is not publishing.
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

def travisTestJavaOptions: Seq[String] = {
  // When building on travis-ci, we want to suppress logging to error level only.
  val travisBuild = sys.env.getOrElse("TRAVIS", "false").toBoolean
  if (travisBuild) {
    Seq(
      "-DSKIP_FLAKY=true",
      "-Dsbt.log.noformat=true",
      "-Dorg.slf4j.simpleLogger.defaultLogLevel=error",
      "-Dcom.twitter.inject.test.logging.disabled",
      // Needed to avoid cryptic EOFException crashes in forked tests
      // in Travis with `sudo: false`.
      // See https://github.com/sbt/sbt/issues/653
      // and https://github.com/travis-ci/travis-ci/issues/3775
      "-Xmx3G")
  } else {
    Seq(
      "-DSKIP_FLAKY=true")
  }
}

lazy val versions = new {
  // When building on travis-ci, querying for the branch name via git commands
  // will return "HEAD", because travis-ci checks out a specific sha.
  val travisBranch = sys.env.getOrElse("TRAVIS_BRANCH", "")

  // All Twitter library releases are date versioned as YY.MM.patch
  val twLibVersion = releaseVersion

  val commonsCodec = "1.9"
  val commonsFileupload = "1.3.1"
  val commonsIo = "2.4"
  val commonsLang = "2.6"
  val guava = "19.0"
  val guice = "4.0"
  val jackson = "2.8.4"
  val jodaConvert = "1.2"
  val jodaTime = "2.5"
  val junit = "4.12"
  val libThrift = "0.10.0"
  val logback = "1.1.7"
  val mockito = "1.9.5"
  val mustache = "0.8.18"
  val nscalaTime = "2.14.0"
  val scalaCheck = "1.13.4"
  val scalaGuice = "4.1.0"
  val scalaTest = "3.0.0"
  val servletApi = "2.5"
  val slf4j = "1.7.21"
  val snakeyaml = "1.12"
  val specs2 = "2.4.17"

  val finatra="18.4.0"
  val slick="3.2.3"
  val h2="1.4.185"
}

lazy val scalaCompilerOptions = scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint",
  "-Ywarn-unused-import"
)

lazy val baseSettings = Seq(
  libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" %  versions.mockito % "test",
    "org.scalacheck" %% "scalacheck" % versions.scalaCheck % "test",
    "org.scalatest" %% "scalatest" %  versions.scalaTest % "test",
    "org.specs2" %% "specs2-core" % versions.specs2 % "test",
    "org.specs2" %% "specs2-junit" % versions.specs2 % "test",
    "org.specs2" %% "specs2-mock" % versions.specs2 % "test"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  scalaCompilerOptions,
  javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  javacOptions in doc ++= Seq("-source", "1.8"),
  // -a: print stack traces for failing asserts
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a"),
  // broken in 2.12 due to: https://issues.scala-lang.org/browse/SI-10134
  scalacOptions in (Compile, doc) ++= {
    if (scalaVersion.value.startsWith("2.12")) Seq("-no-java-comments")
    else Nil
  }
)


lazy val slf4jSimpleTestDependency = Seq(
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-simple" % versions.slf4j % "test"
  )
)


lazy val baseServerSettings =  Seq(
  organization := "com.iyoumee",
  publishArtifact := false,
  publishLocal := {},
  publish := {},
  assemblyMergeStrategy in assembly := {
    case "BUILD" => MergeStrategy.discard
    case "META-INF/io.netty.versions.properties" => MergeStrategy.last
    case other => MergeStrategy.defaultMergeStrategy(other)
  }
)




def aggregatedProjects = Seq[sbt.ProjectReference](coServer)

def mappingContainsAnyPath(mapping: (File, String), paths: Seq[String]): Boolean = {
  paths.foldLeft(false)(_ || mapping._1.getPath.contains(_))
}

lazy val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(baseSettings)
  .settings(buildSettings)
  .settings(noPublishSettings)
  .settings(
    name := "sgw2",
    Compile / name := "sgw2",
    organization := "com.twitter",
    moduleName := "finatra-root",
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject
      //-- inProjects(benchmarks)
      // START EXAMPLES
      //-- inProjects(benchmarkServer, exampleHttpJavaServer, exampleInjectJavaServer,
      //exampleWebDashboard, helloWorld,
      //streamingExample, thriftExampleIdl, thriftExampleServer,
      //thriftJavaExampleIdl, thriftJavaExampleServer, twitterClone)
    // END EXAMPLES
  ).aggregate(aggregatedProjects: _*)



lazy val coServer = (project in file("sgw2"))
  .settings(baseServerSettings)
  .settings(baseSettings)
  .settings(buildSettings)
  .settings(noPublishSettings)
  .settings(
    name := "co-server",
    moduleName := "co-server",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finatra-http" % versions.finatra,
      "com.twitter" %% "finatra-http" % versions.finatra % "test",
      "com.twitter" %% "finatra-httpclient" % versions.finatra,
      "com.twitter" %% "finatra-httpclient" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "finatra-http" % versions.finatra % "test"  classifier "tests",
      "com.twitter" %% "inject-core" % versions.finatra % "test"  ,
      "com.twitter" %% "inject-core" % versions.finatra % "test" classifier "tests",
      "com.twitter" %% "inject-server" % versions.finatra % "test"  ,
      "com.twitter" %% "inject-server" % versions.finatra % "test"  classifier "tests",
      "com.twitter" %% "inject-app" % versions.finatra % "test"  ,
      "com.twitter" %% "inject-app" % versions.finatra % "test"  classifier "tests",
      "com.twitter" %% "inject-modules" % versions.finatra % "test"  ,
      "com.twitter" %% "inject-modules" % versions.finatra % "test" classifier "tests",
      "org.scalatest" %% "scalatest" %  versions.scalaTest % "test",
      "com.google.inject" % "guice" % versions.guice  % "test",
      "com.google.inject.extensions" % "guice-testlib" %  versions.guice % "test",
      "org.mockito" % "mockito-core" %   versions.mockito  % "test",
      "ch.qos.logback" % "logback-classic" % versions.logback,
      "com.typesafe.slick" %% "slick" % versions.slick,
      "com.h2database"  %   "h2" % versions.h2,
       "mysql" % "mysql-connector-java" % "5.1.34"
    )
  )