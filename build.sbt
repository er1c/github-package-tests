import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val ScalaTestVersion = "3.2.7"

val Scala300 = "3.0.0-RC2"
val Scala213 = "2.13.5"
val Scala212 = "2.12.13"
val Scala211 = "2.11.12"
val Scala210 = "2.10.7"

ThisBuild / scalaVersion := Scala212
// GitHub Actions requires a single list of crossScalaVersions, add filters in workflow steps
ThisBuild / crossScalaVersions := Seq(Scala210, Scala211, Scala212, Scala213, Scala300)
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

// For https://github.com/djspiewak/sbt-github-packages
ThisBuild / githubOwner := "er1c"
ThisBuild / githubRepository := "github-packages-tests"
ThisBuild / githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")

// Update ci via `sbt githubWorkflowGenerate`
// For https://github.com/djspiewak/sbt-github-actions
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map("GITHUB_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}")
  )
)

val checkCI = TaskKey[Unit]("checkCI")

lazy val commonSettings = Seq(
  checkCI := Def.sequential(
    Compile / compile,
    Test / test,
  ).value,
  publishMavenStyle := true,
  publishTo := githubPublishTo.value,
)

ThisBuild / githubWorkflowArtifactUpload := false
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"))
ThisBuild / githubWorkflowBuildMatrixAdditions += "platform" -> List(
  "jvm",
  "js",
)

ThisBuild / githubWorkflowBuildMatrixExclusions ++= {
  (ThisBuild / crossScalaVersions).value.collect {
    // Exclude js platform for Scala 3.x and Scala 2.10.x
    case scalaVersion: String if scalaVersion.startsWith("3.") || scalaVersion.startsWith("2.10") =>
      MatrixExclude(Map("platform" -> "js", "scala" -> scalaVersion))
  }
}

val JvmCond = s"matrix.platform == 'jvm'"
val JsCond = s"matrix.platform == 'js'"

val Scala210Cond = s"matrix.scala == '$Scala210'"
val Scala211Cond = s"matrix.scala == '$Scala211'"
val Scala212Cond = s"matrix.scala == '$Scala212'"
val Scala213Cond = s"matrix.scala == '$Scala213'"
val Scala300Cond = s"matrix.scala == '$Scala300'"

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("crossScalaExample/checkCI"),
    name = Some("Test cross-scala-example"),
    cond = Some(JvmCond)
  ),
  WorkflowStep.Sbt(List("crossPlatformExampleJVM/checkCI"),
    name = Some("Test cross-platform-example JVM"),
    cond = Some(JvmCond + " && " + Seq(
      // No Scala 2.10
      Scala211Cond,
      Scala212Cond,
      Scala213Cond,
      Scala300Cond,
    ).mkString("(", " || ", ")"))
  ),
  WorkflowStep.Sbt(List("crossPlatformExampleJS/checkCI"),
    name = Some("Test cross-platform-example JS"),
    cond = Some(JsCond+ " && " + Seq(
      // No Scala 2.10
      Scala211Cond,
      Scala212Cond,
      Scala213Cond,
      Scala300Cond,
    ).mkString("(", " || ", ")"))
  ),
  WorkflowStep.Sbt(List("sbtPluginExample/checkCI"),
    name = Some("Test sbt-plugin-example"),
    cond = Some(JvmCond + " && (" + Scala210Cond + " || " + Scala212Cond + ")")
  ),
  WorkflowStep.Sbt(List("javaProjectExample/checkCI"),
    name = Some("Test java-project-example"),
    cond = Some(JvmCond + " && " + Scala212Cond)
  ),
)

lazy val crossScalaExample = project.in(file("cross-scala-example"))
  .settings(
    commonSettings,
    name := "cross-scala-example",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
    ),
  )

lazy val crossPlatformExample = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("cross-platform-example"))
  .settings(
    commonSettings,
    name := "cross-platform-example",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % ScalaTestVersion % Test,
    ),
    crossScalaVersions := Seq(Scala211, Scala212, Scala213),
  )
  .jsSettings(Seq(
    jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
  ))

lazy val crossPlatformExampleJVM = crossPlatformExample.jvm
lazy val crossPlatformExampleJS = crossPlatformExample.js

lazy val javaProjectExample = (project in file("java-project-example"))
  .settings(
    commonSettings,
    name := "java-project-example",
    crossPaths := false,
    scalaVersion := Scala212,
    crossScalaVersions := Seq(Scala212)
  )

lazy val sbtPluginExample = project.in(file("sbt-plugin-example"))
  .enablePlugins(SbtPlugin)
  .settings(
    commonSettings,
    name := "sbt-plugin-example",
    scalaVersion := Scala212,
    crossScalaVersions := Seq(Scala210, Scala212),
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.10" => "0.13.8"
        case "2.12" => "1.0.1"
        case _ => (pluginCrossBuild / sbtVersion).value
      }
    },
  )

lazy val root = project.in(file("."))
  .aggregate(
    crossPlatformExampleJVM,
    crossPlatformExampleJS,
    crossScalaExample,
    javaProjectExample,
    sbtPluginExample,
  )
  .settings(publishArtifact := false)
