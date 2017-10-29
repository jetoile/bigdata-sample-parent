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

import org.apache.hadoop.gateway.shell.AbstractRequest;
import org.apache.hadoop.gateway.shell.BasicResponse;
import org.apache.hadoop.gateway.shell.Hadoop;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;

import java.util.concurrent.Callable;

public class JobKill {
  public static class Response extends BasicResponse {
    Response(HttpResponse response) {
      super(response);
    }
  }

  static public class Request extends AbstractRequest<Response> {
    private String jobId;

    Request(Hadoop session) {
      super(session);
    }

    public Request job(String jobId) {
      this.jobId = jobId;
      return this;
    }

    protected Callable<Response> callable() {
      return () -> {
        URIBuilder uri = null;
        uri = Request.this.uri(new String[]{Workflow.SERVICE_PATH, "/job/"+jobId});
        uri.addParameter("action", "kill");
        HttpPut request = new HttpPut(uri.build());
        return new Response(Request.this.execute(request));
      };
    }
  }
}
