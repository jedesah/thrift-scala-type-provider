import sbt._, Keys._

object TypeProviderExamples extends Build {
  import BuildSettings._

  lazy val rdfsPublic: Project = Project(
    "rdfs-public",
    file("rdfs-public"),
    settings = macroProjectSettings ++ Seq(
      libraryDependencies ++= (sesameDependencies :+ "default" %% "thrift_parser" % "0.0.1-SNAPSHOT")
    )
  )

  lazy val rdfs: Project = Project(
    "rdfs",
    file("rdfs"),
    settings = buildSettings ++ Seq(
      /** See this Stack Overflow question and answer for some discussion of
        * why we need this line: http://stackoverflow.com/q/17134244/334519
        */
      unmanagedClasspath in Compile <++= unmanagedResources in Compile
    )
  ).dependsOn(rdfsPublic)
}

object BuildSettings {
  val paradiseVersion = "2.0.1"
  val paradiseDependency =
    "org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full

  val buildSettings = Defaults.defaultSettings ++ Seq(
    version := "0.0.0-SNAPSHOT",
    scalaVersion := "2.11.2",
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases")
    ),

    /** We need the Macro Paradise plugin both to support the macro
      * annotations used in the public type provider implementation and to
      * allow us to use quasiquotes in both implementations. The anonymous
      * type providers could easily (although much less concisely) be
      * implemented without the plugin.
      */
    addCompilerPlugin(paradiseDependency)
  )

  val bananaDependencies = Seq(

  )

  val sesameDependencies = Seq(

  )

  val macroProjectSettings = buildSettings ++ Seq(
    libraryDependencies <+= (scalaVersion)(
      "org.scala-lang" % "scala-reflect" % _
    ),
    libraryDependencies ++= (
      if (scalaVersion.value.startsWith("2.10")) List(paradiseDependency) else Nil
    )
  )
}

