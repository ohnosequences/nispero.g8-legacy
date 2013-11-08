import ohnosequences.sbt._

import sbtrelease._
import ReleaseStateTransformations._
import ReleasePlugin._
import ReleaseKeys._

publishMavenStyle := false

bucketSuffix := "$bucketsSuffix$"

isPrivate := true

releaseSettings

releaseProcess <<= thisProjectRef apply { ref =>
  Seq[ReleaseStep](
    inquireVersions,
    setReleaseVersion,
    setNextVersion,
    publishArtifacts
  )
}

nextVersion := { ver => sbtrelease.Version(ver).map(_.bumpMinor.string).getOrElse(versionFormatError) }

addCommandAlias("nispero-publish", ";reload;release with-defaults")

