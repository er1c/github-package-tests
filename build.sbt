import Dependencies._

// Update ci via `sbt githubWorkflowGenerate`

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

// For https://github.com/djspiewak/sbt-github-actions
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map("GITHUB_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}")
  )
)

lazy val root = (project in file("."))
  .settings(
    name := "GitHub Packages Tests",
    libraryDependencies += scalaTest % Test,
    // For https://github.com/djspiewak/sbt-github-packages
    githubOwner := "er1c",
    githubRepository := "github-packages-tests",
    githubTokenSource := TokenSource.Environment("GITHUB_TOKEN"),
    publishTo := githubPublishTo.value,
    publishMavenStyle := true,
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
