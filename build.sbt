name := "kbc-data-processing"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats" % "0.9.0",
  "com.github.melrief" %% "pureconfig" % "0.6.0",
  "io.monix" %% "monix" % "2.2.4"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:existentials",
  "-Ywarn-dead-code",
  "-Ywarn-unused"
)

def latestScalafmt = "0.7.0-RC1"
commands += Command.args("scalafmt", "Run scalafmt cli.") {
  case (state, args) =>
    lazy val Right(scalafmt) =
      org.scalafmt.bootstrap.ScalafmtBootstrap.fromVersion(latestScalafmt)
    scalafmt.main("--non-interactive" +: args.toArray)
    state
}
