#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
import org.slf4j.LoggerFactory

import static Deploy.*

logger = LoggerFactory.getLogger("deploy.groovy")

def cli = new CliBuilder(
  usage: 'deploy.groovy -e dev -f deploy.properties',
  header: '${symbol_escape}nAvailable options (use -h for help):${symbol_escape}n')

cli.with {
  h(longOpt: 'help', 'Usage Information', required: false)
  e(longOpt: 'env', 'dev/prd/test', args: 1, required: true)
  f(longOpt: 'properties', 'deployment property file', args: 1, required: true)
  d(longOpt: 'dryRun', 'dry run', required: false)
}
def options = cli.parse(args)

def config = loadConfig(options.f)

deployOnHdfs("dist/lib", config.getProperty("hdfs.lib.path"), config, options, true)
deployOnHdfs("dist/oozie", config.getProperty("hdfs.oozie.path"), config, options, true)

runOozieJobs("dist/oozie/run/coordinator/${rootArtifactId}/job.properties", config, options)


//sample

//deleteOnHdfs("/user/user/test_oozie/oozie_1508427630", config, options)

//runOozieJobs("dist/oozie/run/workflow/sample/job.properties", config, option)
//runOozieJobs("dist/oozie/run/coordinator/sample/job.properties", config, options)

//checkOozieCoordinatorStatus(config, options, "status=SUCCEEDED")
//def w = checkOozieCoordinatorStatus(config, options, "status=RUNNING")
//killOrExitCoordinators("SAMPLE COORD", w, options, config)

println "done"
