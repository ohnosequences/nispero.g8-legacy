import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._

publishMavenStyle := false

artifactsBucketsSuffix := "$bucketsSuffix$"

artifactsBucket <<= (isSnapshot, artifactsBucketsSuffix) { (snapshot, suffix) =>
  val prefix = "private." + (if (snapshot) "snapshots" else "releases")
  prefix + "." + suffix
}

publishTo <<= (isSnapshot, artifactsBucket) {
                (snapshot, bucket) =>
  val url = "s3://" + bucket
  Some("$resolver-accessKey$" -> "$resolver-secretKey$") map S3Resolver(url, url, Resolver.ivyStylePatterns).toSbtResolver
}

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

artifact in oneJar <<= moduleName(Artifact(_, "one"))

addArtifact(artifact in (Compile, oneJar), oneJar)

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

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys <<= (name, organization, version, scalaBinaryVersion, artifactsBucket) {
  (name, organization, version, scalaBinaryVersion, artifactsBucket) =>
    //ivy pattern
    val pattern = "[organization]/[name]_[scalaBinaryVersion]/[version]/jars/[name]_[scalaBinaryVersion]-one.jar"
    val key =  pattern
      .replace("[organization]", organization)
      .replace("[name]", name)
      .replace("[version]", version)
      .replace("[scalaBinaryVersion]", scalaBinaryVersion)
    Seq[BuildInfoKey](
      "metadata" -> Map(
        "organization" -> organization,
        "name" -> (name + ".configuration"),  //fix it!
        "artifact" -> name,
        "version" -> version,
        "statikaVersion" -> "0.15.0",
        "jarKey" -> key,
        "jarBucket" -> artifactsBucket
      )
    )
}

//buildInfoPrefix := ""

//buildInfoObjectFormat <<= (name) {
//  (name) =>  "case object %s extends ohnosequences.statika.MetadataOf[" + name + ".configuration]"
//}


buildInfoPackage <<= (name) {
  (name) => name + ".meta"
}

buildInfoObject :=  "configuration"

//buildInfoSuffix := ""

