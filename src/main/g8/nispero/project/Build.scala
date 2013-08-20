import sbt._
import Keys._
import sbtrelease.ReleasePlugin._
import sbtrelease._
import ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import java.io.File

object NisperoBuild extends Build {

  def getConfigurationPackage(): String = {
    //println(file(".").getAbsolutePath)
    val config = "src/main/scala/configuration.scala"

    val lines = scala.io.Source.fromFile(new File(file("."), config)).getLines()

    lines.find(_.matches("\\s*package.+")) match {
      case None => throw new Error("package isn't specified in " + config)
      case Some(s) => {

        s.replace("package","").trim
      }
    }

  }

  lazy val artifactsBucket = SettingKey[String]("artifacts-bucket", "bucket for releases")

  lazy val configurationPackage = SettingKey[String]("configuration-package", "configuration-package")

  lazy val awsScalaTools = Project(
    id = getConfigurationPackage(),
    base = file("."),
    settings = Defaults.defaultSettings ++ releaseSettings ++ Seq(
      configurationPackage := getConfigurationPackage()
    )
  )

}