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

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.gateway.shell.AbstractRequest;
import org.apache.hadoop.gateway.shell.BasicResponse;
import org.apache.hadoop.gateway.shell.Hadoop;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class JobList {
  public static class Response extends BasicResponse {
    Response(HttpResponse response) {
      super(response);
    }
  }

  static public class Request extends AbstractRequest<Response> {
    private List<String> filter = new ArrayList<>();
    private String type;

    Request(Hadoop session) {
      super(session);
    }

    public Request filter(String filter) {
      this.filter.add(filter);
      return this;
    }

    public Request type(String type) {
      this.type = type;
      return this;
    }

    protected Callable<Response> callable() {
      return () -> {
        String comma = URLEncoder.encode(";");
        String filters = Request.this.filter.stream().map(e -> URLEncoder.encode(e) ).collect(Collectors.joining(comma));
        URIBuilder uri = null;
        if (StringUtils.isEmpty(filters)) {
          uri = Request.this.uri(new String[]{Workflow.SERVICE_PATH, "/jobs/"});
        } else {
          uri = Request.this.uri(new String[]{Workflow.SERVICE_PATH, "/jobs/", "?filter=" + filters});
        }
        if (StringUtils.isNotEmpty(type)) {
          uri.addParameter("jobtype", type);
        }
        HttpGet request = new HttpGet(uri.build());
        return new Response(Request.this.execute(request));
      };
    }
  }
}
