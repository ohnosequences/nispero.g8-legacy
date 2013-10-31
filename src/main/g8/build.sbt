name := "$name$"

description := ""

organization := "ohnosequences"

libraryDependencies ++= Seq(
  "ohnosequences" % "nispero-abstract_2.10" % "1.3.0"
)

resolvers ++= Seq(
  "Era7 maven releases"  at "http://releases.era7.com.s3.amazonaws.com",
  "Era7 maven snapshots"  at "http://snapshots.era7.com.s3.amazonaws.com"
)

addCommandAlias("nispero-run", ";reload;run")
