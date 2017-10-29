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
package fr.jetoile.hadoop.sample

import fr.jetoile.hadoop.sample.model.FooData
import org.apache.spark.sql.{DataFrame, SparkSession}

class SimpleJob(sc: SparkSession) {
  val sparkSession = sc

  def read(path: String): DataFrame = {
    sparkSession.read.format("orc").load(path)
  }

  def write(dataFrame: DataFrame, index: String, docType: String): Unit = {
    import sparkSession.implicits._
    val rdd = dataFrame.map(t => new FooData(t.getAs("id"), t.getAs("value")))

    //    println("====================")
    //    rdd.collect().foreach(
    //      t => {
    //        println(t.id)
    //        println(t.value)
    //      }
    //    )
    //    println("====================")
    import org.elasticsearch.spark._
    rdd.rdd.saveToEs(index + "/" + docType)
  }

  def shutdown(): Unit = {
    if (sparkSession != null) {
      sparkSession.stop()
    }
  }

  def createExternalTable(dataframe: DataFrame, hiveTableName: String, location: String) = {
    dataframe.registerTempTable("my_temp_table")
    sparkSession.sql("CREATE EXTERNAL TABLE " + hiveTableName + " (id STRING, value STRING) STORED AS ORC LOCATION '" + location + "'")
    sparkSession.sql("INSERT INTO " + hiveTableName + " SELECT * from my_temp_table")
  }
}
