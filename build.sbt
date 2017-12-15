import scala.sys.process.Process

name := "tcc_demo"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "4.0.0" % "test")

scalacOptions in Test ++= Seq("-Yrangepos")


lazy val thriftExampleIdl = (project in file("examples/thrift-server/thrift-example-idl"))
  .settings(
    name := "thrift-example-idl",
    moduleName := "thrift-example-idl",
    scroogeThriftIncludeFolders in Compile := Seq(
      file("thrift/src/main/thrift/"),
      file("examples/thrift-server/thrift-example-idl/src/main/thrift")),
    libraryDependencies ++= Seq(
      "com.twitter" %% "scrooge-core" % "17.12.0",
      "com.twitter" %% "finatra-thrift" % "17.12.0"
    )
  ).dependsOn(thrift)

lazy val thrift = project
  .settings(
    name := "finatra-thrift",
    moduleName := "finatra-thrift",
    libraryDependencies ++= Seq(
      "com.twitter" %% "scrooge-core" % "17.11.0",
      "com.twitter" %% "finatra-thrift" % "17.12.0"
    )
  )

lazy val thriftExampleServer = (project in file("examples/thrift-server/thrift-example-server"))
  .settings(
    name := "thrift-example-server",
    moduleName := "thrift-example-server",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finatra-thrift" % "17.12.0",
      "com.twitter" %% "finatra-thrift" % "17.12.0"% Test classifier "tests",
      "com.twitter" %% "inject-core" % "17.12.0"% Test classifier "tests",
      "com.twitter" %% "inject-server" % "17.12.0"% Test  classifier "tests",
      "com.twitter" %% "inject-app" % "17.12.0"% Test  classifier "tests",
      "com.twitter" %% "inject-modules" % "17.12.0"% Test  classifier "tests",
      "org.scalatest" %% "scalatest" %  "3.0.0" % "test",
      "com.google.inject" % "guice" % "4.0" classifier "tests",
      "com.google.inject.extensions" % "guice-testlib" %"4.0" % "test",
      "org.mockito" % "mockito-core" %   "1.9.5" % "test",


    )
  ).dependsOn(
  thriftExampleIdl,
  thrift % "test->test;compile->compile",


  )