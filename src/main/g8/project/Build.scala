import sbt._
import Keys._

import java.io.File

object NisperoBuild extends Build {

  lazy val artifactsBucketsSuffix = SettingKey[String]("artifacts-buckets-suffix", "suffix of buckets for artifacts")

  lazy val artifactsBucket = SettingKey[String]("artifacts-bucket", "bucket for artifacts")

  lazy val awsScalaTools = Project(
    id = "nispero-project",
    base = file("."),
    settings = Defaults.defaultSettings
  )
}