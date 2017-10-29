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

import java.io.File
import java.util
import java.util.Arrays

import groovy.lang.GroovyShell
import org.slf4j.LoggerFactory

object Main {

  lazy val logger = LoggerFactory.getLogger(this.getClass.getName)


  def main(args: Array[String]): Unit = {

    if (args.length < 1) {
      System.err.println("Error on arguments. First parameters should be the script to run (ex: deploy.groovy).")
      System.exit(-1)
    }

    val shell: GroovyShell = new GroovyShell
    shell.run(new File(args(0)), util.Arrays.copyOfRange(args, 1, args.length))





  }
}
