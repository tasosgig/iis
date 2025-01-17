General notes
====================

Oozie-installer is a utility allowing building, uploading and running oozie workflows. In practice, it creates a `*.tar.gz` package that contains resouces that define a workflow and some helper scripts. See the `iis-wf-core-examples` project for examples of usage.

This module is automatically executed when running: 

`mvn package -Poozie-package -Dworkflow.source.dir=classpath/to/parent/directory/of/oozie_app` 

on module having set:

	<parent>
    		<groupId>eu.dnetlib</groupId>
	        <artifactId>iis-wf</artifactId>
            <version>0.0.1-SNAPSHOT</version>
	</parent>

in `pom.xml` file. `oozie-package` profile initializes oozie workflow packaging, `workflow.source.dir` property points to a workflow (notice: this is not a relative path but a classpath to directory). 
 
The outcome of this packaging is `oozie-package.tar.gz` file containing inside all the resources required to run Oozie workflow:

- jar packages
- workflow definitions
- job properties
- maintenance scripts

Required properties
====================

In order to include proper workflow within package, `workflow.source.dir` property has to be set. It could be provided by setting `-Dworkflow.source.dir=some/job/dir` maven parameter.

Other placeholders used in shell scripts (`*.sh`) along with default values in `pom.xml` file:

	property name		| default value
	---------------------------------------------------
	iis.hadoop.frontend.host.name	| localhost
	iis.hadoop.master.host.name	| localhost
	iis.hadoop.frontend.user.name	| ${user.name} which maven property holding current user name
	iis.hadoop.frontend.home.dir	| /mnt/tmp
	sandboxName			| generated by dedicated plugin, based on `workflow.source.dir`
	sandboxDir			| /user/${iis.hadoop.frontend.user.name}/${sandboxName}
	workingDir			| ${sandboxDir}/working_dir
	oozieAppDir			| oozie_app
	oozieServiceLoc			| http://${iis.hadoop.master.host.name}:11000/oozie
	
this list can be supplemented with job.properties default values defined in `pom.xml` file:

	property name		| default value
	---------------------------------------------------
	nameNode			| hdfs://${iis.hadoop.master.host.name}:8020
	jobTracker			| ${iis.hadoop.master.host.name}:8021
	queueName			| default

All values will be overriden with the ones from `job.properties` and eventually `job-override.properties` stored in module's main folder. Values can be also provided as maven command line -D arguments.

When overriding properties from `job.properties`, `job-override.properties` file can be created in main module directory (the one containing `pom.xml` file) and define all new properties which will override existing properties. One can provide those properties one by one as command line arguments.

Properties overriding order is the following:

1. `pom.xml` defined properties (located in the project root dir)
2. `~/.m2/settings.xml` defined properties
3. `${workflow.source.dir}/job.properties`
4. `job-override.properties` (located in the project root dir)
5. `maven -Dparam=value`

where the maven `-Dparam` property is overriding all the other ones.

Workflow definition requirements
====================

`workflow.source.dir` property should point to the following directory structure:

	[${workflow.source.dir}]
		|
		|-job.properties (optional)
		|
		\-[oozie_app]
			|
			\-workflow.xml

This property can be set using maven `-D` switch.

`[oozie_app]` is the default directory name however it can be set to any value as soon as `oozieAppDir` property is provided with directory name as value. 

Subworkflows are supported as well and subworkflow directories should be nested within `[oozie_app]` directory. 

Creating oozie installer step-by-step
=====================================

Automated oozie-installer steps are the following:

1. creating jar packages:  `*.jar` and `*tests.jar` along with copying all dependancies in `target/dependencies`
2. reading properties from maven, `job.properties`, `job-override.properties`
3. invoking priming mechanism linking resources from import.txt file (currently resolving subworkflow resources)
4. assembling shell scripts for preparing Hadoop filesystem, uploading Oozie application and starting workflow
5. copying whole `${workflow.source.dir}` content to `target/${oozie.package.file.name}`
6. generating updated `job.properties` file in `target/${oozie.package.file.name}` based on maven, `job.properties` and `job-override.properties`
7. creating lib directory (or multiple directories for subworkflows for each nested directory) and copying jar packages created at step (1) to each one of them
8. bundling whole `${oozie.package.file.name}` directory into single tar.gz package

Uploading oozie package and running workflow on cluster
=======================================================

In order to simplify deployment and execution process four dedicated profiles were introduced:

- deploy-local
- run-local
- deploy
- run

to be used along with `oozie-package` profile e.g. by providing `-Poozie,deploy-local,run-local` or `-Poozie,deploy,run` maven parameters.

`deploy-local` profile supplements packaging process with:
1) extracting oozie package to `target/local-upload` directory
2) uploading oozie package content to local hadoop cluster

`run-local` profile introduces:
1) executing workflow uploaded to HDFS cluster using `deploy-local` command

`deploy` profile supplements packaging process with:
1) uploading oozie-package via scp to `/mnt/tmp/${user.name}/oozie-package-${timestamp}` directory on `${iis.hadoop.frontend.host.name}` machine
2) extracting uploaded package
3) uploading oozie content to hadoop cluster

`run` profile introduces:
1) executing workflow uploaded to HDFS cluster using `deploy` command
2) removing uploaded files

Notice: ssh access to frontend machine has to be configured on system level and it is preferable to set key-based authentication in order to simplify remote operations.

Other tips
==========

It is a good practice to define all hadoop cluster related environment variables in local  `~/.m2/settings.xml` file.
