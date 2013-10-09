
resolvers ++= Seq(
  "Era7 maven releases"  at "http://releases.era7.com.s3.amazonaws.com"
, "Era7 maven snapshots"  at "http://snapshots.era7.com.s3.amazonaws.com"
)

//addSbtPlugin("ohnosequences" % "sbt-statika" % "0.9.0")

addSbtPlugin("org.scala-sbt.plugins" % "sbt-onejar" % "0.8")

addSbtPlugin("ohnosequences" % "sbt-s3-resolver" % "0.6.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.2.5")
