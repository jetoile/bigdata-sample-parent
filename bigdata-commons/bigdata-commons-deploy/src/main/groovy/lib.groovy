/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.io.FileType
import groovy.xml.MarkupBuilder
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.gateway.shell.Hadoop
import org.apache.hadoop.gateway.shell.hdfs.Hdfs as hdfs
import org.apache.hadoop.gateway.shell.workflow.Workflow as workflow
import org.apache.hadoop.gateway.shell.workflow.domain.OozieCoordinator
import org.apache.hadoop.gateway.shell.workflow.domain.OozieJob
import org.apache.hadoop.gateway.shell.workflow.domain.Workflows
import org.slf4j.LoggerFactory

class Deploy {
  static logger = LoggerFactory.getLogger("lib.groovy")
  public static final String KNOX_PASSWORD_KEY = "KNOX_PASSWORD"

  static def getHadoopSession(Properties config, options) {
    def hadoop
    def password = System.getenv(KNOX_PASSWORD_KEY)

    if (password == null) {
      println("The environment variable KNOX_PASSWORD have to be set.")
      println("Exit...")
      System.exit(-1)
    }

    if (options.e == "test") {
      hadoop = Hadoop.loginInsecure(config.gateway, config.username, password)
    } else {
      hadoop = Hadoop.login(config.gateway, config.username, password)
    }
    return hadoop
  }

  static def runOozieJobs(String jobPropertiesPath, Properties config, options) {
    def hadoop = getHadoopSession(config, options)
    def oozieProperty = getOozieProperty(jobPropertiesPath, config)
    runOozie(config, oozieProperty, options)
    sleep(5000)

    hadoop.shutdown()
  }

  static def cleanup(File[] dirs) {
    Arrays.asList(dirs).each {
      dir ->
        logger.debug "cleanup dir: $dir"
        dir.deleteDir()
    }
  }

  static def String getOozieProperty(String jobPath, Properties config) {
    logger.debug "is going to read job.properties: $jobPath"
    def props = loadConfig(jobPath)

    def xmlWriter = new StringWriter()
    def printer = new IndentPrinter(new PrintWriter(xmlWriter), "", false)
    def xmlMarkup = new MarkupBuilder(printer)

    xmlMarkup.configuration() {
      def keys = props.stringPropertyNames()
      keys.each {
        key ->
          property() {
            name(key)get
            value(props.getProperty(key))
          }
      }
      property() {
        name("user.name")
        value(config.username)
      }
    }
    xmlWriter
  }

  static def String runOozie(Properties config, String oozieProperty, OptionAccessor options) {
    def hadoop = getHadoopSession(config, options)

    logger.debug "is going to submit oozie task with $oozieProperty"
    def jobId = "_test_"
    if (!options.d) {
//        jobId = workflow.submit(hadoop).text(oozieProperty).now().jobId
      workflow.submit(hadoop).text(oozieProperty).now().close()
    }
    logger.debug "oozie task $jobId submitted"
    hadoop.shutdown()
    jobId
  }


  static def Workflows checkOozieWorkflowStatus(Properties config, OptionAccessor options, String... filters) {
    def hadoop = getHadoopSession(config, options)

    logger.debug "is going to check running oozie jobs"
    def jobs = workflow.list(hadoop)
    filters.toList().each { t ->
      jobs.filter(t)
    }
    def workflows = jobs.now()
    InputStream stream = workflows.getStream();
    ObjectMapper mapper = new ObjectMapper();
    def result = mapper.readValue(stream, Workflows.class);
    logger.debug "oozie jobs status: $result"
    workflows.close()
    hadoop.shutdown()
    result
  }

  static def void killOrExitWorkflow(Workflows runningWorkflow, OptionAccessor options, config) {
    List<OozieJob> currentRunningWorkflow = runningWorkflow.workflows.findAll { it -> it.appName.startsWith(config.'oozie.job.kill.prefix') }

    if (!currentRunningWorkflow.isEmpty()) {
      def response = System.console().readLine 'workflows are running. Do you want to kill them? If no, the deploy will stop (Y/[N])'
      if (StringUtils.equalsIgnoreCase("y", response)) {
        currentRunningWorkflow.each {
          w ->
            logger.debug "is going to kill workflow $w.id"
            if (!options.d) {
              def hadoop = getHadoopSession(config, options)

              workflow.kill(hadoop).job(w.id).now().close()
              hadoop.shutdown()
            }
        }
      } else {
        logger.error("jobs are running: cannot deploy oozie")
        logger.error("is stopping. Clean shouild be made")
        System.exit(-1)
      }
    }
  }

  static def Workflows checkOozieCoordinatorStatus(Properties config, OptionAccessor options, String... filters) {
    def hadoop = getHadoopSession(config, options)

    logger.debug "is going to check running oozie jobs"
    def jobs = workflow.list(hadoop)
    filters.toList().each { t ->
      jobs.filter(t)
    }
    def workflows = jobs.type("coordinator").now()
    InputStream stream = workflows.getStream();
    ObjectMapper mapper = new ObjectMapper();
    def result = mapper.readValue(stream, Workflows.class);
    logger.debug "oozie coordinator status: $result"
    workflows.close()
    hadoop.shutdown()
    result
  }


  static def void killOrExitCoordinators(Workflows runningCoordinator, OptionAccessor options, config) {
    List<OozieCoordinator> currentRunningCoord = runningCoordinator.coordinatorjobs.findAll { it -> it.coordJobName.startsWith(config.'oozie.job.kill.prefix') }

    if (!currentRunningCoord.isEmpty()) {
      def response = System.console().readLine 'coordinator are running. Do you want to kill them? If no, the deploy will stop (Y/[N])'
      if (StringUtils.equalsIgnoreCase("y", response)) {
        currentRunningCoord.each {
          c ->
            logger.debug "is going to kill coordinator $c.coordJobId"
            if (!options.d) {
              def hadoop = getHadoopSession(config, options)

              workflow.kill(hadoop).job(c.coordJobId).now().close()
              hadoop.shutdown()
            }
        }
      } else {
        logger.error("coordinators are running: cannot deploy oozie")
        logger.error("is stopping")
        System.exit(-1)
      }
    }
  }

  static def deployOnHdfs(String inputPath, String destPath, Properties config, OptionAccessor options) {
    deployOnHdfs(inputPath, destPath, config, options, false)
  }

  static def deployOnHdfs(String input, String destPath, Properties config, OptionAccessor options, boolean forceDelete) {
    def hadoop = getHadoopSession(config, options)

    def inputPath = input.replace("dist/", "")

    if (forceDelete) {
      logger.debug "is going to check for " + destPath
      def res = 404
      try {
        def resp = hdfs.ls(hadoop).dir(destPath + "/" + inputPath).now()
        res = resp.statusCode
        resp.close()
      } catch (Exception e) {
        //NOTHING TO DO
      }

      if (200 == res) {
        def response = System.console().readLine destPath + "/" + inputPath + ' directory exists. Sure you want to delete it? If no, directory will be renamed (Y/[N])'
        if ("y".equalsIgnoreCase(response)) {
          logger.debug "is going to delete " + destPath + "/" + inputPath + " on hdfs"
          if (!options.d) {
            hdfs.rm(hadoop).file(destPath + "/" + inputPath).recursive().now().close()
          }
        } else {
          def seconds = System.currentTimeSeconds()
          logger.debug """is going to rename directory ${destPath}/${inputPath} to ${destPath}/${inputPath}_${
            seconds
          } on hdfs"""
          if (!options.d) {
            hdfs.rename(hadoop).file(destPath + "/" + inputPath).to(destPath + "/" + inputPath + "_" + seconds).now().close()
          }
        }
      }
    }

    hadoop.shutdown()

    logger.debug "is going to deploy $inputPath on hdfs"

    def ressources = []

    new File(input).eachFileRecurse(FileType.FILES) {
      ressources << it
    }

    //put
    ressources.each {
      t ->
        hadoop = getHadoopSession(config, options)

//        def fileName = t.path.replace("dist/", "")

        def fileName = t.path.replaceAll("\\\\", "/").replace("dist/" + inputPath, "")
        String dir = (destPath + "/" + fileName).replaceAll("\\\\", "/")
        logger.debug "put: $t.absolutePath to $dir"

        if (!options.d) {
          hdfs.put(hadoop).file(t.absolutePath).to(dir).now().close()
        }
        hadoop.shutdown()
    }

  }


  static def Properties loadConfig(String file) {
    Properties config = new Properties()
    File propertiesFile = new File(file)
    propertiesFile.withInputStream {
      config.load(it)
    }
    config
  }

}
