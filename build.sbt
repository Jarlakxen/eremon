
// ··· Project Options ···

val scalaVersions = Seq("2.12.3")

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  //"-Xplugin-require:macroparadise",
  "-Ymacro-expand:normal",
  "-Xfuture"
)

// ··· Project Repositories ···

val nexusHost = sys.props.get("NEXUS_HOST").getOrElse("nexus.cluster.fravega.com")

val realm = sys.props.get("NEXUS_REALM").getOrElse("Sonatype Nexus Repository Manager")

val nexusUrl = s"https://$nexusHost/repository"

// ··· Project Settings ···

lazy val commonSettings = Seq(
  organization := "io.eremon",
  scalaVersion := scalaVersions.head,
  crossScalaVersions := scalaVersions,
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, p)) if p >= 11 => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Yno-predef"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Yno-predef"))
  },
  scalacOptions in Test ~= {
    _.filterNot(Set("-Yno-predef"))
  },
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  publishTo := {
    if (isSnapshot.value)
      Some("snapshots" at nexusUrl + "/maven-snapshots/")
    else
      Some("releases"  at nexusUrl + "/maven-releases/")
  },
  publishMavenStyle := true,
  credentials += {
    (sys.props.get("NEXUS_USER"), sys.props.get("NEXUS_PWD")) match {
      case (Some(u), Some(p)) => Credentials(realm, nexusHost, u, p)
      case _                  => Credentials(Path.userHome / ".sbt" / ".credentials")
    }
  },
  fork in (Test,run) := false,
  parallelExecution in Test := false
)

// ··· Project Dependancies ···
val reactivemongoV  = "0.12.6"
val circeV          = "0.8.0"
val slf4JV          = "1.7.25"
val vLogback        = "1.2.3"
val vDockerKit      = "0.9.5"
val scalatestV     	= "3.0.4"

val reactiveMongoDependancies =  Seq(
  "org.reactivemongo"     %% "reactivemongo"                      % reactivemongoV    % "provided"
)

val loggingDependancies =  Seq(
  "org.slf4j"             %  "slf4j-api"                          % slf4JV,
  "ch.qos.logback"        %  "logback-classic"                    % vLogback          %  Test
)

val testDependancies =  Seq(
  "com.whisk"             %% "docker-testkit-scalatest"           % vDockerKit        %  Test, 
  "com.whisk"             %% "docker-testkit-impl-spotify"        % vDockerKit        %  Test,
  "org.scalatest"         %% "scalatest"                          % scalatestV        %  Test
)

val baseDependancies = reactiveMongoDependancies ++ loggingDependancies ++ testDependancies

// ··· Projects Settings ···

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    Seq(
      name := "eremon-core",
      libraryDependencies ++= baseDependancies ++ Seq(
        // --- Utils --
        "org.scala-lang"  %  "scala-reflect"                      % scalaVersion.value
      )
    )
  )

lazy val json = (project in file("json"))
  .settings(
    commonSettings,
    Seq(
      name := "eremon-json",
      libraryDependencies ++= baseDependancies ++ Seq(
        // --- Utils ---
        "io.circe"        %% "circe-generic"                      % circeV,
        "io.circe"        %% "circe-parser"                       % circeV,
        "io.circe"        %% "circe-optics"                       % circeV
      )
    )
  )
  .dependsOn(core)

lazy val root = (project in file(".")).settings(commonSettings).aggregate(core, json)

