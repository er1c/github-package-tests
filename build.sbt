import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("ci-release")))

lazy val root = (project in file("."))
  .settings(
    name := "GitHub Packages Tests",
    libraryDependencies += scalaTest % Test,
    githubTokenSource := TokenSource.Environment("GITHUB_TOKEN")
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
