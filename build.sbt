lazy val ScalaVersions = Seq("2.11.8", "2.12.1")

organization in ThisBuild := "com.geirsson"
scalaVersion in ThisBuild := ScalaVersions.head
crossScalaVersions in ThisBuild := ScalaVersions

lazy val MetaVersion = "1.7.0-487-13087aaf"
lazy val ParadiseVersion = "3.0.0-185"
lazy val baseSettings = Seq(
  version := "0.1.3",
  // Only needed when using bintray snapshot versions
  resolvers += Resolver.bintrayIvyRepo("scalameta", "maven"),
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.1" % Test
)

lazy val metaMacroSettings: Seq[Def.Setting[_]] = Seq(
  addCompilerPlugin(
    ("org.scalameta" % "paradise" % ParadiseVersion).cross(CrossVersion.full)),
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
  libraryDependencies += "org.scalameta" %%% "scalameta" % MetaVersion % Provided,
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.
  sources in (Compile, doc) := Nil // macroparadise doesn't work with scaladoc yet.
)

lazy val publishSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (version.value.endsWith("-SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/olafurpg/metaconfig")),
  autoAPIMappings := true,
  apiURL := Some(url("https://github.com/olafurpg/metaconfig")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/olafurpg/metaconfig"),
      "scm:git:git@github.com:olafurpg/metaconfig.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>olafurpg</id>
        <name>Ólafur Páll Geirsson</name>
        <url>https://geirsson.com</url>
      </developer>
    </developers>
)

lazy val allSettings = baseSettings ++ publishSettings ++ metaMacroSettings

lazy val `metaconfig-core` = crossProject
  .settings(
    allSettings,
    metaMacroSettings
  )
lazy val `metaconfig-coreJVM` = `metaconfig-core`.jvm
lazy val `metaconfig-coreJS` = `metaconfig-core`.js

lazy val `metaconfig-hocon` = crossProject
  .settings(
    allSettings,
    metaMacroSettings,
    libraryDependencies += "eu.unicredit" %%% "shocon" % "0.1.6"
  )
  .dependsOn(`metaconfig-core`)
lazy val `metaconfig-hoconJVM` = `metaconfig-hocon`.jvm
lazy val `metaconfig-hoconJS` = `metaconfig-hocon`.js
