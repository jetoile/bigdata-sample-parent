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
package ${package}

import com.beust.jcommander.JCommander
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory

object Main {

  lazy val logger = LoggerFactory.getLogger(this.getClass.getName)


  private def logArguments(reader: String, configPath: String, persister: String) = {
  }

  def parseArgs(args: Array[String]): (String, String, String) = {
    val jCommander = new JCommander(CommandLineArgs, args.toArray: _*)

    if (CommandLineArgs.help) {
      jCommander.usage()
      System.exit(0)
    }
    (CommandLineArgs.inputPath, CommandLineArgs.index, CommandLineArgs.docType)

  }

  def main(args: Array[String]): Unit = {

    val sparkSession = SparkSession.builder.appName("simpleApp").config("es.index.auto.create", "true").enableHiveSupport.getOrCreate

    val (inputPath, index, docType) = parseArgs(args)

    val job = new SimpleJob(sparkSession)
    val dataFrame = job.read(inputPath)
    dataFrame.cache()
    job.write(dataFrame, index, docType)
    job.createExternalTable(dataFrame, "tmp", inputPath + "/tmp")


  }
}
