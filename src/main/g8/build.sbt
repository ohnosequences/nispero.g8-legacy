import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._
import NisperoBuild._

name := "w"

description := ""

organization := "ohnosequences"

isPrivate := true

//artifactsBucket := "frutero.org"

buildInfoKeys <++= (s3credentials) {
    case s3cred => Seq[BuildInfoKey]("credentials" -> s3cred)
}


//buildInfoKeys <++= (s3credentials) {
//    case Some(s3c) => Seq[BuildInfoKey]("credentials" -> s3c)
//    case None => throw new Error("s3credentials isn't set!")
//}

buildInfoObjectFormat <<= (bundlePackage, bundleObject) { (bp, bo) =>
  "object %s extends ohnosequences.statika.MetadataOf["+bp+"."+bo+".type]"
}


buildInfoKeys <++= (artifactsBucket) {
    case bucket => Seq[BuildInfoKey](
        "artifactsBucket" -> bucket,
        "instanceProfileARN" -> None,
        "password" -> math.random.hashCode.abs.toString
    )
}

publishMavenStyle := false

publishTo <<= (isSnapshot, s3credentials, artifactsBucket) {
                (snapshot,   credentials, bucket) =>
  val prefix = "private." + (if (snapshot) "snapshots" else "releases")
  credentials map s3resolver("My "+prefix+" S3 bucket", "s3://"+prefix+"." + bucket, Resolver.localBasePattern)
}

statikaVersion := "0.14.0"


statikaPrivateResolvers <<= (artifactsBucket) { bucket =>
  Seq(
    ("s3://private.releases." + bucket)  at s3("s3://private.releases." + bucket),
    ("s3://private.snapshots." + bucket) at s3("s3://private.snapshots." + bucket)
  )
}



libraryDependencies ++= Seq(
	"ohnosequences" % "nispero-abstract_2.10" % "0.1.2-SNAPSHOT"
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