val dottyVersion = "3.1.2"
val org = "org.mycompany"

lazy val plugin = project
  .settings(
    name := "scala-plugin-test",
    organization := org,
    version := "0.1.0-SNAPSHOT",
    scalaVersion := dottyVersion,

    libraryDependencies += "org.scala-lang" %% "scala3-compiler" % dottyVersion % "provided"
  )

lazy val hello = project
  .settings(
    name := "hello",
    version := "0.1.0",
    scalaVersion := dottyVersion,

    libraryDependencies += compilerPlugin("org.mycompany" %% "scala-plugin-test" % "0.1.0-SNAPSHOT"),
  )


lazy val root = project
  .aggregate(plugin, hello)


  addCommandAlias("rebuildWithRebuiltPlugin", ";root/clean;plugin/compile;plugin/publishLocal;compile")