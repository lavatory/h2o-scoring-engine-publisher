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
package org.trustedanalytics.h2oscoringengine.publisher.restapi;

import javax.validation.constraints.NotNull;

import org.trustedanalytics.h2oscoringengine.publisher.http.BasicAuthServerCredentials;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class PublishRequest {

  @NotNull
  private BasicAuthServerCredentials h2oCredentials;

  @NotNull
  private String modelName;

  @NotNull
  private String orgGuid;

}
