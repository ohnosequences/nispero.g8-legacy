import AssemblyKeys._

Statika.distributionProject

name := "$name$"

description := ""

organization := "$organization$"

libraryDependencies ++= Seq(
  "ohnosequences" % "nispero-abstract_2.10" % "1.5.0"
)

metadataObject := name.value

addCommandAlias("nispero-run", ";reload;run")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case "about.html" => MergeStrategy.first
    case x => old(x)
  }
}
