package $name$

import ohnosequences.nispero._
import ohnosequences.nispero.bundles._
import ohnosequences.awstools.ec2.{InstanceType, InstanceSpecs}
import ohnosequences.awstools.s3.ObjectAddress
import ohnosequences.nispero.distributions.AMI201309
import ohnosequences.nispero.manager.ManagerAutoScalingGroups
import ohnosequences.nispero.worker.WorkersAutoScalingGroup
import java.io.File


object nispero extends Nispero {


  case object configuration extends Configuration {

    val metadata = meta.configuration

    //id of nispero instance
    val version = (metadata.name + metadata.version).replace(".", "").replace(this.toString, "").toLowerCase

    val specs = InstanceSpecs(
      instanceType = InstanceType.M1Medium,
      amiId = AMI201309.id,
      securityGroups = List("nispero"),
      keyName = "nispero",
      instanceProfile = Some("nispero")
    )

    val config = Config(

      email = "$email$",

      managerConfig = ManagerConfig(
        groups = ManagerAutoScalingGroups(
          instanceSpecs = specs.copy(instanceType = InstanceType.M1Medium),
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
  echo "success" > message
  """

    val configure =
  """
  echo "configuring"
  """
  }


    def test() {
      val result1 = manager.installWithDeps(worker, true)
      val result2 = nisperoDistribution.installWithDeps(console, true)
    }
}




