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

import com.beust.jcommander.Parameter

object CommandLineArgs {
  @Parameter(
    names = Array("-h", "--help"), help = true)
  var help = false

  @Parameter(
    names = Array("-p", "--inputPath"),
    description = "hdfs input path",
    required = true)
  var inputPath: String = _

  @Parameter(
    names = Array("-i", "--index"),
    description = "Elasticsearch index's name",
    required = true)
  var index: String = _

  @Parameter(
    names = Array("-d", "--docType"),
    description = "Document type",
    required = true)
  var docType: String = _
}
