package $name$

import ohnosequences.statika._

import ohnosequences.nispero._
import ohnosequences.nispero.bundles._
import ohnosequences.nispero.bundles.console.{Console, FarmStateLogger}
import ohnosequences.awstools.ec2.{EC2, InstanceType, InstanceSpecs}
import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.nispero.bundles.{NisperoDistribution, Worker, Configuration, metadataProvider}
import ohnosequences.nispero.distributions.AMI44939930
import ohnosequences.nispero.manager.ManagerAutoScalingGroups

import java.io.File
import ohnosequences.nispero.worker.WorkersAutoScalingGroup

case object configuration extends Configuration {

  val metadata = meta.configuration

  //id of nispero instance
  val version = (metadata.name + metadata.version).replace(".", "").replace(this.toString, "").toLowerCase

  val specs = InstanceSpecs(
    instanceType = InstanceType.M1Medium,
    amiId = AMI44939930.id,
    securityGroups = List("nispero"),
    keyName = "nispero",
    instanceProfile = Some("nispero")

  )

  val config = Config(

    email = "$email$",

    managerConfig = ManagerConfig(
      groups = ManagerAutoScalingGroups(
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
        )
      )
    )
  )

}

case object instructions extends ScriptExecutor() {
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


case object aws extends AWS(configuration) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object resources extends bundles.Resources(configuration, aws) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object logUploader extends LogUploader(resources, aws) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object worker extends Worker(instructions, resources, logUploader, aws) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object controlQueueHandler extends ControlQueueHandler(resources, aws) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object terminationDaemon extends TerminationDaemon(resources, aws) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object manager extends Manager(controlQueueHandler, terminationDaemon, resources, logUploader, aws, worker, AMI44939930) {
  val metadata = metadataProvider.generateAWSMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object farmStateLogger extends FarmStateLogger(resources, aws) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object console extends Console(resources, logUploader, farmStateLogger, aws) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}


case object nisperoDistribution extends NisperoDistribution(manager, console, AMI44939930) {
  val metadata = metadataProvider.generateAWSMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}


object nisperoCLI {

  def main(args: Array[String]) {
    new NisperoRunner(nisperoDistribution, configuration.config, args.toList).run()
  }

  //compiler check
  def test() {
    val result = manager.installWithDeps(worker, true)
  }
}

