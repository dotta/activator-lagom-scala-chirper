organization in ThisBuild := "sample.chirper"

scalaVersion in ThisBuild := "2.11.8"

lazy val friendApi = project("friend-api")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies += lagomJavadslApi
  )

lazy val friendImpl = project("friend-impl")
  .enablePlugins(LagomJava)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomJavadslPersistence,
      lagomJavadslTestKit
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(friendApi, utils)

lazy val chirpApi = project("chirp-api")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomJavadslApi,
      lagomJavadslJackson
    )
  )

lazy val chirpImpl = project("chirp-impl")
  .enablePlugins(LagomJava)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      lagomJavadslPersistence,
      lagomJavadslPubSub,
      lagomJavadslTestKit
    )
  )
  .settings(lagomForkedTestSettings: _*)
  .dependsOn(chirpApi, utils)

lazy val activityStreamApi = project("activity-stream-api")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies += lagomJavadslApi
  )
  .dependsOn(chirpApi)

lazy val activityStreamImpl = project("activity-stream-impl")
  .enablePlugins(LagomJava)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies += lagomJavadslTestKit
  )
  .dependsOn(activityStreamApi, chirpApi, friendApi, utils)

lazy val frontEnd = project("front-end")
  .enablePlugins(PlayScala, LagomPlay)
  .settings(
    version := "1.0-SNAPSHOT",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      "org.webjars" % "react" % "0.14.3",
      "org.webjars" % "react-router" % "1.0.3",
      "org.webjars" % "jquery" % "2.2.0",
      "org.webjars" % "foundation" % "5.3.0"
    ),
    ReactJsKeys.sourceMapInline := true
  )

lazy val loadTestApi = project("load-test-api")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies += lagomJavadslApi
  )

lazy val loadTestImpl = project("load-test-impl")
  .enablePlugins(LagomJava)
  .settings(version := "1.0-SNAPSHOT")
  .dependsOn(loadTestApi, friendApi, activityStreamApi, chirpApi, utils)

lazy val utils = project("utils")
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies += lagomJavadslApi
  )

def project(id: String) = Project(id, base = file(id))
  .settings(
    scalacOptions in Compile += "-Xexperimental" // this enables Scala lambdas to be passed as Java SAMs  
  )
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.3" // actually, only api projects need this
    )
  )

// do not delete database files on start
lagomCassandraCleanOnStart in ThisBuild := false

// set up information for where to publish our bundles to
// (see http://conductr.lightbend.com/docs/1.1.x/CreatingBundles#Publishing-bundles for more
// information)
licenses in ThisBuild := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
bintrayVcsUrl in Bundle in ThisBuild := Some("https://github.com/lagom/activator-lagom-scala-chirper")
bintrayOrganization in Bundle in ThisBuild := Some("typesafe")
bintrayReleaseOnPublish in Bundle in ThisBuild := true
