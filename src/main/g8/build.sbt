import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._
import NisperoBuild._

name := "$name$"

description := ""

organization := "ohnosequences"

isPrivate := true

artifactsBucketsSuffix := "$bucketsSuffix$"


buildInfoObjectFormat <<= (bundlePackage, bundleObject) { (bp, bo) =>
  "object %s extends ohnosequences.statika.MetadataOf["+bp+"."+bo+".type]"
}

buildInfoKeys <++= (artifactsBucketsSuffix) {
    case bucket => Seq[BuildInfoKey](
        "artifactsBucket" -> bucket,
        "instanceProfileARN" -> None,
        "password" -> math.random.hashCode.abs.toString
    )
}

publishMavenStyle := false

publishTo <<= (isSnapshot, artifactsBucketsSuffix) {
                (snapshot, bucket) =>
  val prefix = "private." + (if (snapshot) "snapshots" else "releases")
  Some("$resolver-accessKey$" -> "$resolver-secretKey$") map s3resolver("My "+prefix+" S3 bucket", "s3://"+prefix+"." + bucket, Resolver.localBasePattern)
}

statikaVersion := "0.14.0"


statikaPrivateResolvers <<= (artifactsBucketsSuffix) { bucket =>
  Seq(
    ("s3://private.releases." + bucket)  at s3("s3://private.releases." + bucket),
    ("s3://private.snapshots." + bucket) at s3("s3://private.snapshots." + bucket)
  )
}


libraryDependencies ++= Seq(
  "ohnosequences" % "nispero-abstract_2.10" % "0.1.5"
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

addCommandAlias("nispero-run", ";reload;publish;run")
