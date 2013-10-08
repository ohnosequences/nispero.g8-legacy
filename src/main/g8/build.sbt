name := "$name$"

description := ""

organization := "ohnosequences"

libraryDependencies ++= Seq(
  "ohnosequences" % "nispero-abstract_2.10" % "1.0.0-RC1"
)

bundleObjects := Seq(
  "$name$.configuration"
)

addCommandAlias("nispero-run", ";reload;publish;run")
