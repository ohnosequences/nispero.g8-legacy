package $name$

import ohnosequences.statika._

import ohnosequences.nispero._
import ohnosequences.awstools.ec2.{Tag, InstanceType, InstanceSpecs}
import ohnosequences.awstools.autoscaling.{LaunchConfiguration, AutoScalingGroup}
import ohnosequences.nispero.bundles.{NisperoDistribution, Worker, Configuration, metadataProvider}
import ohnosequences.nispero.distributions.AMI44939930
import org.clapper.avsl.Logger
import ohnosequences.awstools.s3.ObjectAddress

case object configuration extends Configuration {

  val metadata = meta.configuration

  //id of nispero instance
  val version = (metadata.name + metadata.version).replace(".", "").replace(this.toString, "").toLowerCase


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
      instanceSpecs = specs.copy(instanceType = InstanceType.C1Medium)
    ),

    initialTasks = Some(ObjectAddress("team1-test-bucket", "tasksTeam1")),

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

  val logger = Logger(this.getClass)


  def runManager() {


    logger.info("setup AWS account")

    val config = configuration.config

    val awsClients: AWSClients = AWSClients.fromCredentials(config.accessKey, config.secretKey)

    val prep = new AccountPrepare(configuration.config, configuration.metadata.artifactsBucket)

    if(prep.prepareAccount()) {


      logger.info("creating notification topic: " + config.resources.notificationTopic)
      val topic = awsClients.sns.createTopic(config.resources.notificationTopic)

      if (!topic.isEmailSubscribed(config.email)) {
        logger.info("subscribing " + config.email + " to notification topic")
        topic.subscribeEmail(config.email)
        logger.info("please confirm subscription")
      }

      logger.info("adding initial tasks")
      configuration.config.initialTasks.foreach(Utils.addInitialTasks(awsClients, _, configuration.config.resources.inputQueue))

      logger.info("generating userScript for manager")
      val us = nisperoDistribution.userScript(manager, Explicit(config.accessKey, config.secretKey))


      val specs = configuration.config.managerConfig.instanceSpecs.copy(userData = us)

      val instance = awsClients.ec2.runInstances(1, specs).headOption

      instance.foreach(_.createTag(Tag(InstanceTags.STATUS_TAG_NAME, "manager")))

      println("manager run at " + instance.map(_.getInstanceId()))


    }

  }

  def main(args: Array[String]) {

    def test() {
     //compiler check
     val result = manager.installWithDeps(worker, true)
    }

    args.headOption match {
      case Some("run")  => runManager()
      case _ => runManager()
    }

  }

}
