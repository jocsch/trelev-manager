import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := "event-manager"

version := "0.1"

scalaVersion := "2.11.1"

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "spray nightlies" at "http://nightlies.spray.io"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

//inmemory journal akka
resolvers += "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven"

//courier mail
resolvers += "courier-resolver-0" at "http://dl.bintray.com/content/softprops/maven"


libraryDependencies ++= Seq(
    "com.typesafe.akka"  %% "akka-actor"       % "2.3.6",
    "com.typesafe.akka"  %% "akka-slf4j"       % "2.3.6",
    //"com.typesafe.akka"  %% "akka-persistence-experimental" % "2.4-SNAPSHOT",
    "com.typesafe.akka"  %% "akka-persistence-experimental" % "2.3.6",
    "ch.qos.logback"      % "logback-classic"  % "1.0.13",
    "io.spray"           %% "spray-can"        % "1.3.1",
    "io.spray"           %% "spray-routing"    % "1.3.1",
    "io.spray"           %% "spray-json"       % "1.2.6",
    "com.github.nscala-time" %% "nscala-time" % "1.4.0",
    "me.lessis"          %% "courier"          % "0.1.3",
    "org.specs2"         %% "specs2"           % "2.4.6"         % "test",
    "io.spray"           %% "spray-testkit"    % "1.3.1" % "test",
    "com.typesafe.akka"  %% "akka-testkit"     % "2.3.6"        % "test",
	"com.github.dnvriend" %% "akka-persistence-inmemory" % "1.0.0",
    "com.novocode"        % "junit-interface"  % "0.11"          % "test->default"
    )

scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-Xlint",
    "-Ywarn-dead-code",
    "-language:_",
    "-target:jvm-1.7",
    "-encoding", "UTF-8"
    )

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

Revolver.settings

mainClass in Revolver.reStart := Some("org.trelev.eventmgr.Rest")

javaOptions in Revolver.reStart += "-Dconfig.file=conf/trelev.conf"

javaOptions in run += "-Dconfig.file=conf/trelev.conf"

initialCommands in console := """
import org.trelev.eventmgr.Cli
import org.trelev.eventmgr.Cli._
import org.trelev.eventmgr._
Cli
"""

//for packaing the complete project
packageArchetype.java_application

Keys.mainClass in (Compile) := Some("org.trelev.eventmgr.Rest")

//needed for akka leveldb in run. Might not be needed if revolver is used
fork := true

