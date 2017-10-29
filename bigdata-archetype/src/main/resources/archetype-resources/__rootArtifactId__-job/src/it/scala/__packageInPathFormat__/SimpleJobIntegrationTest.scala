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

import java.net.InetAddress

import com.ninja_squad.dbsetup.Operations.{sequenceOf, sql}
import com.ninja_squad.dbsetup.operation.Operation
import fr.jetoile.hadoopunit.HadoopUnitConfig._
import fr.jetoile.hadoopunit.test.hdfs.HdfsUtils
import fr.jetoile.hadoopunit.test.hive.{HiveConnectionUtils, HiveSetup}
import fr.jetoile.hadoopunit.{HadoopUnitConfig, HadoopUtils}
import org.apache.commons.configuration.{Configuration, PropertiesConfiguration}
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.assertj.core.api.Assertions._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.scalatest._

class SimpleJobIntegrationTest extends FeatureSpec with BeforeAndAfterAll with BeforeAndAfter with GivenWhenThen {

  var configuration: Configuration = _
  val inputCsvPath: String = "/input/csv"
  val inputOrcPath: String = "/input/orc"
  val index: String = "test_index"
  val docType: String = "foo"
  var DROP_TABLES: Operation = _


  override protected def beforeAll(): Unit = {
    HadoopUtils.INSTANCE

    configuration = new PropertiesConfiguration(HadoopUnitConfig.DEFAULT_PROPS_FILE)

    DROP_TABLES = sequenceOf(sql("DROP TABLE IF EXISTS default.toto"));
  }

  before {
    val fileSystem = HdfsUtils.INSTANCE.getFileSystem

    val hdfsPath = "hdfs://" + configuration.getString(HDFS_NAMENODE_HOST_KEY) + ":" + configuration.getInt(HDFS_NAMENODE_PORT_KEY) + "/"

    val created = fileSystem.mkdirs(new Path(hdfsPath + inputCsvPath))

    fileSystem.copyFromLocalFile(new Path(SimpleJobIntegrationTest.this.getClass.getClassLoader.getResource("simplefile.csv").toURI), new Path(hdfsPath + inputCsvPath + "/simplefile.csv"))

    val sparkSession = SparkSession.builder.appName("test").master("local[*]").enableHiveSupport.getOrCreate

    val dataFrame = sparkSession.read.format("com.databricks.spark.csv")
      .option("header", "true")
      .option("delimiter", ",")
      .load(hdfsPath + inputCsvPath + "/simplefile.csv")

    dataFrame.write.option("orc.compress", "ZLIB")
      .mode(SaveMode.Append)
      .orc(hdfsPath + inputOrcPath + "/simplefile.orc")

    sparkSession.stop()
  }

  after {
    new HiveSetup(HiveConnectionUtils.INSTANCE.getDestination, sequenceOf(DROP_TABLES)).launch()
    HdfsUtils.INSTANCE.getFileSystem().delete(new Path("/input"))
  }

  feature("simple test") {
    scenario("read data") {

      Given("a local spark conf")
      val sparkSession = SparkSession.builder.appName("test").master("local[*]").enableHiveSupport.getOrCreate

      And("my job")
      val job = new SimpleJob(sparkSession)

      When("I read an orc")
      val hdfsPath = "hdfs://" + configuration.getString(HDFS_NAMENODE_HOST_KEY) + ":" + configuration.getInt(HDFS_NAMENODE_PORT_KEY) + "/"
      val dataFrame = job.read(hdfsPath + inputOrcPath + "/simplefile.orc")

      Then("I have the right schema")
      assertThat(dataFrame.schema.fieldNames).contains("id", "value")

      job.shutdown()
    }

    scenario("write into es") {

      Given("a local spark conf")
      val sparkSession = SparkSession.builder
        .appName("test")
        .master("local[*]")
        .config("spark.driver.allowMultipleContexts", "true")
        .config("es.index.auto.create", "true")
        .config("es.nodes", configuration.getString(ELASTICSEARCH_IP_KEY))
        .config("es.port", configuration.getString(ELASTICSEARCH_HTTP_PORT_KEY))
        .config("es.nodes.wan.only", "true")
        .enableHiveSupport
        .getOrCreate

      And("my job")
      val job = new SimpleJob(sparkSession)

      When("I read an orc")
      val hdfsPath = "hdfs://" + configuration.getString(HDFS_NAMENODE_HOST_KEY) + ":" + configuration.getInt(HDFS_NAMENODE_PORT_KEY) + "/"
      val dataFrame = job.read(hdfsPath + inputOrcPath + "/simplefile.orc")

      And("I call write method")
      job.write(dataFrame, index, docType)
      job.shutdown()

      Then("data is indexed into ES")
      val client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(configuration.getString(ELASTICSEARCH_IP_KEY)), configuration.getInt(ELASTICSEARCH_TCP_PORT_KEY)))

      client.admin.indices.prepareRefresh(index).execute.actionGet

      val response = client.prepareSearch(index)
        .setTypes(docType)
        .setSize(0)
        .setQuery(QueryBuilders.queryStringQuery("*")).get().getHits().getTotalHits();

      assertThat(response).isEqualTo(3)
    }

    scenario("create external table") {

      Given("a local spark conf")
      val sparkSession = SparkSession.builder.appName("test").master("local[*]").enableHiveSupport.getOrCreate

      And("my job")
      val job = new SimpleJob(sparkSession)

      When("I read an orc")
      val hdfsPath = "hdfs://" + configuration.getString(HDFS_NAMENODE_HOST_KEY) + ":" + configuration.getInt(HDFS_NAMENODE_PORT_KEY) + "/"
      val dataFrame = job.read(hdfsPath + inputOrcPath + "/simplefile.orc")

      And("I call createExternalTable method")
      job.createExternalTable(dataFrame, "default.toto", hdfsPath + "input/titi")
      job.shutdown()

      Then("my external table is created")
      val stmt = HiveConnectionUtils.INSTANCE.getConnection.createStatement
      val resultSet = stmt.executeQuery("SELECT * FROM default.toto")
      while (resultSet.next) {
        val id = resultSet.getInt(1)
        val value = resultSet.getString(2)
        assertThat(id).isNotNull
        assertThat(value).isNotNull
      }

    }

  }

}
