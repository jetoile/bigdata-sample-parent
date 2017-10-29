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
package org.apache.hadoop.gateway.shell.workflow;

import org.apache.hadoop.gateway.shell.Hadoop;
import org.apache.hadoop.gateway.shell.workflow.Submit.Request;

public class Workflow {
  static String SERVICE_PATH = "/oozie/v1";

  public Workflow() {
  }

  public static Request submit(Hadoop session) {
    return new Request(session);
  }

  public static JobList.Request list(Hadoop session) {
    return new JobList.Request(session);
  }

  public static JobKill.Request kill(Hadoop session) {
    return new JobKill.Request(session);
  }

  public static Status.Request status(Hadoop session) {
    return new Status.Request(session);
  }
}
