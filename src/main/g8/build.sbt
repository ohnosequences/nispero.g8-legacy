import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._
import NisperoBuild._

name := "$name$"

description := ""

organization := "ohnosequences"

isPrivate := true

//artifactsBucket := "$bucket$"

buildInfoKeys <++= (s3credentials) {
    case Some(s3c) => Seq[BuildInfoKey]("credentials" -> s3c)
    case None => throw new Error("s3credentials isn't set!")
}

buildInfoKeys <++= (artifactsBucket) {
    case bucket => Seq[BuildInfoKey]("artifactsBucket" -> bucket)
}

publishMavenStyle := false

publishTo <<= (isSnapshot, s3credentials, artifactsBucket) {
                (snapshot,   credentials, bucket) =>
  val prefix = "private." + (if (snapshot) "snapshots" else "releases")
  credentials map s3resolver("My "+prefix+" S3 bucket", "s3://"+prefix+"." + bucket, Resolver.localBasePattern)
}


statikaPrivateResolvers <<= (artifactsBucket) { bucket =>
  Seq(
    ("s3://private.releases." + bucket)  at s3("s3://private.releases." + bucket),
    ("s3://private.snapshots." + bucket) at s3("s3://private.snapshots." + bucket)
  )
}


libraryDependencies ++= Seq(
	"ohnosequences" % "nispero-abstract_2.10" % "0.0.5-SNAPSHOT",
	"ohnosequences" % "nispero-scriptexecutor-legacy_2.10" % "0.0.6"
)

bundlePackage <<= configurationPackage

bundleObject := "configuration"

// sbt-release settings

releaseSettings

releaseProcess <<= thisProjectRef apply { ref =>
  Seq[ReleaseStep](
    inquireVersions,
    setReleaseVersion,
    setNextVersion,
    publishArtifacts
  )
}

nextVersion := { ver => Version(ver).map(_.bumpMinor.string).getOrElse(versionFormatError) }

addCommandAlias("nispero-publish", ";reload;release with-defaults") 

addCommandAlias("nispero-run", ";reload;run run") 