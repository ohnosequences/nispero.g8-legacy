import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._

isPrivate := true

publishMavenStyle := false

artifactsBucketsSuffix := "$bucketsSuffix$"

publishTo <<= (isSnapshot, artifactsBucketsSuffix) {
                (snapshot, bucket) =>
  val prefix = "private." + (if (snapshot) "snapshots" else "releases")
  val url = "s3://" + prefix + "." + bucket
  Some("$resolver-accessKey$" -> "$resolver-secretKey$") map S3Resolver(url, url, Resolver.ivyStylePatterns).toSbtResolver
}

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

