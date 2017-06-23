
// ··· Project Options ···

val scalaVersions = Seq("2.12.2", "2.11.11")

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
  fork in (Test,run) := false
)

// ··· Project Dependancies ···
val reactivemongoV          = "0.12.3"
val scalametaV              = "1.7.0"
val catsV                   = "0.9.0"
val circeV                  = "0.8.0"
val slf4JV                  = "1.7.25"
val vLogback                = "1.2.3"
val vDockerKit              = "0.9.3"
val spec2V                  = "3.9.1"
val jUnitV                  = "4.12"

val reactiveMongoDependancies =  Seq(
  "org.reactivemongo"             %% "reactivemongo"                      % reactivemongoV    % "provided"
)

val loggingDependancies =  Seq(
  "org.slf4j"                     %  "slf4j-api"                          % slf4JV,
  "ch.qos.logback"                %  "logback-classic"                    % vLogback          %  "test"
)

val testDependancies =  Seq(
  "com.whisk"                     %% "docker-testkit-specs2"              % vDockerKit        %  "test", 
  "com.whisk"                     %% "docker-testkit-impl-spotify"        % vDockerKit        %  "test",
  "org.specs2"                    %% "specs2-mock"                        % spec2V            %  "test",
  "org.specs2"                    %% "specs2-junit"                       % spec2V            %  "test",
  "junit"                         %  "junit"                              % jUnitV            %  "test"
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
        "org.scala-lang"                %  "scala-reflect"                      % scalaVersion.value
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
        "io.circe"                      %% "circe-generic"                      % circeV,
        "io.circe"                      %% "circe-parser"                       % circeV,
        "io.circe"                      %% "circe-optics"                       % circeV
      )
    )
  )
  .dependsOn(core)

lazy val root = (project in file(".")).settings(commonSettings).aggregate(core, json)

