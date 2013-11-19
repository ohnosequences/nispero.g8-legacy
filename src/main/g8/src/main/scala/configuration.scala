package $name$

import ohnosequences.statika._
import ohnosequences.statika.ami.AMI149f7863
import ohnosequences.nispero._
import ohnosequences.nispero.bundles._
import ohnosequences.nispero.bundles.console.{Console, FarmStateLogger}
import ohnosequences.awstools.ec2.{EC2, InstanceType, InstanceSpecs}
import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.awstools.autoscaling._
import ohnosequences.nispero.bundles.{NisperoDistribution, Worker, Configuration}
import ohnosequences.nispero.manager.ManagerAutoScalingGroups
import ohnosequences.nispero.worker.WorkersAutoScalingGroup
import java.io.File
import ohnosequences.typesets._
import shapeless._

case object configuration extends Configuration {

  type Metadata = generated.metadata.$name$
  val metadata = new generated.metadata.$name$()  
  val version = generateId(metadata)
  
  type AMI = AMI149f7863.type
  val ami = AMI149f7863

  val specs = InstanceSpecs(
    instanceType = InstanceType.C1Medium,
    amiId = ami.id,
    securityGroups = List("nispero"),
    keyName = "nispero",
    instanceProfile = Some("nispero")
  )

  val config = Config(

    email = "$email$",

    managerConfig = ManagerConfig(
      groups = ManagerAutoScalingGroups(
        instanceSpecs = specs.copy(instanceType = InstanceType.C1Medium),
        version = version,
        purchaseModel = SpotAuto
      ),
      password = "$password$"
    ),

    tasksProvider = EmptyTasks,

    //sets working directory to ephemeral storage
    workersDir =  "/media/ephemeral0",

    //maximum time for processing task
    taskProcessTimeout = 60 * 60 * 1000,

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

    terminationConditions = TerminationConditions(
      terminateAfterInitialTasks = false
    ),

    jarAddress = getAddress(metadata.artifactUrl)
  )
}

case object instructions extends ScriptExecutor() {
  val instructionsScript =
    """
echo "success" > message
bash -x input/script.sh
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

case object manager extends Manager(controlQueueHandler, terminationDaemon, resources, logUploader, aws, worker)

case object farmStateLogger extends FarmStateLogger(resources, aws)

case object console extends Console(resources, logUploader, farmStateLogger, aws)

case object nisperoDistribution extends NisperoDistribution(manager, console)

object nisperoCLI extends NisperoRunner(nisperoDistribution) {
  def compilerChecks() {
    manager.installWithDeps(worker)  
    nisperoDistribution.installWithDeps(console) 
    nisperoDistribution.installWithDeps(manager)
  }
}

