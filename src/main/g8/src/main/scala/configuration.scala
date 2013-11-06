package $name$

import ohnosequences.statika._
import ohnosequences.nispero._
import ohnosequences.nispero.bundles._
import ohnosequences.nispero.bundles.console.{Console, FarmStateLogger}
import ohnosequences.awstools.ec2.{EC2, InstanceType, InstanceSpecs}
import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.awstools.autoscaling._
import ohnosequences.nispero.bundles.{NisperoDistribution, Worker, Configuration, metadataProvider}
import ohnosequences.nispero.distributions._
import ohnosequences.nispero.manager.ManagerAutoScalingGroups
import ohnosequences.nispero.worker.WorkersAutoScalingGroup
import java.io.File

case object configuration extends Configuration {

  val metadata = metadataProvider.fromMap[this.type]("configuration", meta.configuration.metadata)
  //id of nispero instance
  val version = (metadata.name + metadata.version).replace(".", "").replace(this.toString, "")

  val specs = InstanceSpecs(
    instanceType = InstanceType.M1Medium,
    amiId = OneJarAmi201309.id,
    securityGroups = List("nispero"),
    keyName = "nispero",
    instanceProfile = Some("nispero")

  )

  val config = Config(

    email = "$email$",

    managerConfig = ManagerConfig(
      groups = ManagerAutoScalingGroups(
        instanceSpecs = specs.copy(instanceType = InstanceType.M1Medium),
        version = version,
        purchaseModel = SpotAuto
      ),
      password = "$password$"
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
    ),

    jarAddress = ObjectAddress(metadata.jarBucket, metadata.jarKey)
  )

  case object ami extends OneJarAmi201309(config.jarAddress, config.resources.bucket)
}

case object instructions extends ScriptExecutor() {
  val metadata = metadataProvider.generateMetadata[this.type, configuration.metadata.type](this.toString, configuration.metadata)

  val instructionsScript =
    """
echo "success" > message
    """

  val configureScript =
    """
echo "configuring"
    """
}


case object aws extends AWS(configuration)

case object resources extends bundles.Resources(configuration, aws)

case object logUploader extends LogUploader(resources, aws)

case object worker extends Worker(instructions, resources, logUploader, aws)

case object controlQueueHandler extends ControlQueueHandler(resources, aws)

case object terminationDaemon extends TerminationDaemon(resources, aws)

case object manager extends Manager(controlQueueHandler, terminationDaemon, resources, logUploader, aws, worker, configuration.ami)

case object farmStateLogger extends FarmStateLogger(resources, aws)

case object console extends Console(resources, logUploader, farmStateLogger, aws)

case object nisperoDistribution extends NisperoDistribution(manager, console, configuration.ami)


object nisperoCLI extends NisperoRunner(configuration.config) {

  def runNispero(args: List[String]) {
    runNispero(args, nisperoDistribution)
  }

  def installWorker() {
    manager.installWithDeps(worker, true)
  }

  def installConsole() {
    nisperoDistribution.installWithDeps(console, true)
  }

  def installManager() {
    nisperoDistribution.installWithDeps(manager, true)
  }
}

