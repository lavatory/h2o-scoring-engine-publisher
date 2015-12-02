/**
 * Copyright (c) 2015 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.trustedanalytics.h2oscoringengine.publisher.steps;

import static org.trustedanalytics.h2oscoringengine.publisher.http.CloudFoundryEndpoints.BIND_ROUTE_TO_APP_ENDPOINT_TEMPLATE;
import static org.trustedanalytics.h2oscoringengine.publisher.http.CloudFoundryEndpoints.GET_ROUTES_ENDPOINT_TEMPLATE;
import static org.trustedanalytics.h2oscoringengine.publisher.http.CloudFoundryEndpoints.ROUTES_ENDPOINT;
import static org.trustedanalytics.h2oscoringengine.publisher.http.CloudFoundryEndpoints.SHARED_DOMAINS_ENDPOINT;
import static org.trustedanalytics.h2oscoringengine.publisher.http.CloudFoundryResponsesJsonPaths.DOMAIN_JSON_PATH;
import static org.trustedanalytics.h2oscoringengine.publisher.http.CloudFoundryResponsesJsonPaths.ROUTES_NUMBER_JSON_PATH;
import static org.trustedanalytics.h2oscoringengine.publisher.http.CloudFoundryResponsesJsonPaths.ROUTE_GUID_JSON_PATH;
import static org.trustedanalytics.h2oscoringengine.publisher.http.CloudFoundryResponsesJsonPaths.ROUTE_JSON_PATH;
import static org.trustedanalytics.h2oscoringengine.publisher.http.JsonHttpCommunication.createPostRequest;
import static org.trustedanalytics.h2oscoringengine.publisher.http.JsonHttpCommunication.createSimpleJsonRequest;
import static org.trustedanalytics.h2oscoringengine.publisher.http.JsonHttpCommunication.getIntValueFromJson;
import static org.trustedanalytics.h2oscoringengine.publisher.http.JsonHttpCommunication.getStringValueFromJson;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.trustedanalytics.h2oscoringengine.publisher.EnginePublicationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AppRouteCreatingStep {

  private final RestTemplate cfRestTemplate;
  private final String cfApiUrl;
  private final String appGuid;

  public AppRouteCreatingStep(RestTemplate cfRestTemplate, String cfApiUrl, String appGuid) {
    this.cfRestTemplate = cfRestTemplate;
    this.cfApiUrl = cfApiUrl;
    this.appGuid = appGuid;
  }

  public AppBitsUploadingStep createAppRoute(String spaceGuid, String subdomain)
      throws EnginePublicationException {

    try {
      String domainGuid = getAvailableDomain();

      String appRoutesInfoJson = getAppRoutesInfo(subdomain, domainGuid);
      int routesNumber = getIntValueFromJson(appRoutesInfoJson, ROUTES_NUMBER_JSON_PATH);

      String routeGuid;
      if (routesNumber > 0) {
        routeGuid = getStringValueFromJson(appRoutesInfoJson, ROUTE_JSON_PATH);
      } else {
        routeGuid = createNewRoute(subdomain, domainGuid, spaceGuid);
      }
      bindRouteToApp(routeGuid, appGuid);
      return new AppBitsUploadingStep(cfApiUrl, cfRestTemplate, appGuid);
    } catch (IOException e) {
      throw new EnginePublicationException("Unable to create route for app " + subdomain, e);
    }

  }

  private String getAvailableDomain() throws JsonProcessingException, IOException {
    String cfDomainsUrl = cfApiUrl + SHARED_DOMAINS_ENDPOINT;
    ResponseEntity<String> response = cfRestTemplate.exchange(cfDomainsUrl, HttpMethod.GET,
        createSimpleJsonRequest(), String.class);
    String domainGuid = getStringValueFromJson(response.getBody(), DOMAIN_JSON_PATH);

    return domainGuid;
  }

  private String getAppRoutesInfo(String appName, String domainGuid) {
    String cfGetRoutesUrl = cfApiUrl + GET_ROUTES_ENDPOINT_TEMPLATE;
    ResponseEntity<String> response = cfRestTemplate.exchange(cfGetRoutesUrl, HttpMethod.GET,
        createSimpleJsonRequest(), String.class, appName, domainGuid);
    return response.getBody();
  }

  private String createNewRoute(String appName, String domainGuid, String spaceGuid)
      throws JsonProcessingException, IOException {

    String createRouterequestBody = createRouteBody(appName, domainGuid, spaceGuid);

    String cfCreateRouteUrl = cfApiUrl + ROUTES_ENDPOINT;
    ResponseEntity<String> response = cfRestTemplate.exchange(cfCreateRouteUrl, HttpMethod.POST,
        createPostRequest(createRouterequestBody), String.class);

    return getStringValueFromJson(response.getBody(), ROUTE_GUID_JSON_PATH);
  }

  private void bindRouteToApp(String routeGuid, String appGuid) {
    String cfBindRouteToAppUrl = cfApiUrl + BIND_ROUTE_TO_APP_ENDPOINT_TEMPLATE;
    cfRestTemplate.exchange(cfBindRouteToAppUrl, HttpMethod.PUT, createSimpleJsonRequest(),
        String.class, appGuid, routeGuid);
  }

  private String createRouteBody(String subdomain, String domainGuid, String spaceGuid) {
    ObjectMapper mapper = new ObjectMapper();

    ObjectNode requestBody = mapper.createObjectNode();
    requestBody.put("host", subdomain);
    requestBody.put("domain_guid", domainGuid);
    requestBody.put("space_guid", spaceGuid);

    return requestBody.toString();
  }
}
