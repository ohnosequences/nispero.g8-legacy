import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._

isPrivate := true

publishMavenStyle := false

bucketSuffix := "$bucketsSuffix$"

publishTo <<= (isSnapshot, bucketSuffix) {
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

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys <<= (name, organization, version, scalaBinaryVersion, bucketSuffix) {
  (name, organization, version, scalaBinaryVersion, bucketSuffix) =>
    val name = file.getName

    //ivy pattern
    val pattern = "[organization]/[name]_[scalaBinaryVersion]/[version]/jars/[name]_[scalaBinaryVersion]-one.jar"

    val key =  patter
      .replace("[organization]", organization)
      .replace("[name]", name)
      .replace("[version]", version)
      .replace("[scalaBinaryVersion]", scalaBinaryVersion)

    val prefix = "private." + (if (snapshot) "snapshots" else "releases")
    val bucket = prefix + "." + bucket


    Seq[BuildInfoKey](
        "organization" -> organization,
        "name" -> name + ".configuration",  //fix it!
        "artifact" -> name,
        "version" -> version,
        "resolvers" -> Seq[String](),
        "privateResolvers" -> Seq[String](),
        "statikaVersion" -> "0.15.0",
        "jarKey" -> key,
        "jarBucket" -> bucket
    )
}

buildInfoPrefix := ""

buildInfoObjectFormat <<= (name) {
  (name) =>  "case object %s extends ohnosequences.statika.MetadataOf[" + name + ".configuration]"
}


buildInfoPackage <<= (name) {
  (name) => name + ".metadata"
}

buildInfoObject =  "configuration"

buildInfoSuffix := ""

