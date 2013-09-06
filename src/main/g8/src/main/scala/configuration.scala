package $name$

import ohnosequences.statika._

import ohnosequences.nispero._
import ohnosequences.awstools.ec2.{InstanceType, InstanceSpecs}
import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.nispero.bundles.{NisperoDistribution, Worker, Configuration, metadataProvider}
import ohnosequences.nispero.distributions.AMI44939930
import ohnosequences.nispero.manager.{WorkersAutoScalingGroup, ManagerAutoScalingGroup}

import java.io.File

case object configuration extends Configuration {

  val metadata = meta.configuration

  //id of nispero instance
  val version = (metadata.name + metadata.version).replace(".", "").replace(this.toString, "").toLowerCase

  val cred = meta.configuration.credentials

  val awsClients: AWSClients = AWSClients.fromCredentials(cred._1, cred._2)

  val specs = InstanceSpecs(
    instanceType = InstanceType.M1Medium,
    amiId = AMI44939930.id,
    securityGroups = List("nispero"),
    keyName = "nispero"
  )

  val config = Config(

    accessKey = cred._1,
    secretKey = cred._2,

    email = "$email$",

    managerConfig = ManagerConfig(
      managerGroup = ManagerAutoScalingGroup(
        instanceSpecs = specs.copy(instanceType = InstanceType.C1Medium),
        version = version
      ),
      password = metadata.password
    ),

    tasksProvider = EmptyTasks,

    workersDir =  "/media/ephemeral0",

    visibilityTimeoutPolicy = VisibilityTimeoutPolicy(),

    terminationConditions = TerminationConditions(
      terminateAfterInitialTasks = false
    ),

    resources = Resources(
      id = version
    )(
      workersGroup = WorkersAutoScalingGroup(
        desiredCapacity = 1,
        version = version,
        instanceSpecs = specs.copy(
          deviceMapping = Map("/dev/xvdb" -> "ephemeral0")
        ),
        spotPrice = Some(awsClients.ec2.getCurrentSpotPrice(specs.instanceType) + 0.001)
      )
    )
  )

}

case object instructions extends ohnosequences.nispero.bundles.ScriptExecutor() {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)

  val script =
"""
cp input/input1 output/output1
echo "success" > message
"""

  val configure =
"""
echo "configuring"
"""
}



case object resources extends ohnosequences.nispero.bundles.Resources(configuration) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object logUploader extends ohnosequences.nispero.bundles.LogUploader(resources, configuration) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object worker extends Worker(configuration, instructions, resources, logUploader) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object manager extends ohnosequences.nispero.bundles.Manager(configuration, resources, logUploader, worker, AMI44939930) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object nisperoDistribution extends NisperoDistribution(manager, worker, AMI44939930) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}


object nisperoCLI {

  def main(args: Array[String]) {

    //val tasksProvider = new FileTasks(new File("tasks"))
    val nisperoRunner = new NisperoRunner(nisperoDistribution, configuration.config, configuration.metadata.artifactsBucket)

    args.headOption match {
      case Some("run")  => {
        nisperoRunner.run()
      }
      case _ =>  nisperoRunner.run()
    }
  }

  //compiler check
  def test() {
    val result = manager.installWithDeps(worker, true)
  }
}

