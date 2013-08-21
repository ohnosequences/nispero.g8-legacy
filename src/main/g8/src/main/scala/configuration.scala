package $name$

import ohnosequences.statika._

import ohnosequences.nispero._
import ohnosequences.awstools.ec2.{Tag, InstanceType, InstanceSpecs}
import ohnosequences.awstools.autoscaling.{LaunchConfiguration, AutoScalingGroup}
import ohnosequences.nispero.bundles.{NisperoDistribution, Worker, Configuration, metadataProvider}
import ohnosequences.nispero.distributions.AMI44939930
import org.clapper.avsl.Logger
import ohnosequences.nispero.TerminationConditions
import ohnosequences.nispero.Config
import ohnosequences.nispero.ManagerConfig
import ohnosequences.nispero.VisibilityTimeoutPolicy
import ohnosequences.nispero.Resources
import ohnosequences.awstools.s3.ObjectAddress

case object configuration extends Configuration {

  val metadata = meta.configuration

  val version = metadata.version.replace(".", "")

  val cred = meta.configuration.credentials

  val awsClients: AWSClients = AWSClients.fromCredentials(cred._1, cred._2)

  val specs = InstanceSpecs(
    instanceType = InstanceType.C1Medium,
    amiId = AMI44939930.id,
    securityGroups = List("nispero"),
    keyName = "nispero"
  )

  val config = Config(

    accessKey = cred._1,
    secretKey = cred._2,

    email = "$email$",

    managerConfig = ManagerConfig(
      instanceSpecs = specs
    ),

    initialTasks = Some(ObjectAddress("team1-test-bucket", "tasksOldTeam1")),

    workersDir = ".",

    visibilityTimeoutPolicy = VisibilityTimeoutPolicy(),

    terminationConditions = TerminationConditions(
      terminateAfterInitialTasks = false
    ),

    resources = Resources(
      id = version
    )(
      workersGroup = AutoScalingGroup(
        name = "nisperoWorkersGroup" + version,
        minSize = 1,
        maxSize = 2,
        desiredCapacity = 1,

        launchingConfiguration = LaunchConfiguration(
          name = "nisperoLaunchConfiguration" + version,
          spotPrice = awsClients.ec2.getCurrentSpotPrice(specs.instanceType) + 0.001,
          instanceSpecs = specs
        )
      )
    )
  )

  override def install[D <: DistributionAux](distribution: D): InstallResults = {
    success("configuration finished")
  }
}



case object resources extends ohnosequences.nispero.bundles.Resources(configuration) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object logUploader extends ohnosequences.nispero.bundles.LogUploader(resources, configuration) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object worker extends Worker(configuration, $instructionsObject$, resources, logUploader) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object manager extends ohnosequences.nispero.bundles.Manager(configuration, resources, logUploader, worker, AMI44939930) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}

case object nisperoDistribution extends NisperoDistribution(manager, worker, AMI44939930) {
  val metadata = metadataProvider.generateMetadata[this.type, meta.configuration.type](this.toString, meta.configuration)
}


object nisperoCLI {

  val prep = new AccountPrepare(configuration.config, configuration.metadata.artifactsBucket)

  val awsClients: AWSClients = AWSClients.fromCredentials(configuration.config.accessKey, configuration.config.secretKey)
  val logger = Logger(this.getClass)

  def runManager() {
    logger.info("setup AWS account")

    if(prep.prepareAccount()) {

      logger.info("adding initial tasks")
      configuration.config.initialTasks.foreach(Utils.addInitialTasks(awsClients, _, configuration.config.resources.inputQueue))

      logger.info("generating userScript for manager")
      val us = nisperoDistribution.userScript(manager, Left(configuration.cred))

      val specs = configuration.specs.copy(userData = us)

      val instance = awsClients.ec2.runInstances(1, specs).headOption

      instance.foreach(_.createTag(Tag(InstanceTags.STATUS_TAG_NAME, "manager"))

      println("manager run at " + instance.map(_.getInstanceId()))
     }

  }

  def main(args: Array[String]) {

    def test() {
     val result = manager.installWithDeps(worker, true)
    }

    args.headOption match {
      case Some("prepare") => prep.prepareAccount()
      case Some("run")  => runManager()
      //case Some("deps") => println(ohnosequences.nispero.scriptexecutor.scriptexecutor.deps)
      case _ => runManager()
    }

  }

}