/*-
 * -\-\-
 * Spotify Styx Common
 * --
 * Copyright (C) 2017 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.styx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.norberg.automatter.AutoMatter;
import java.time.Instant;

@AutoMatter
@JsonIgnoreProperties(ignoreUnknown = true)
public interface BackfillInput {

  @JsonProperty
  Instant start();

  @JsonProperty
  Instant end();

  @JsonProperty
  String component();

  @JsonProperty
  String workflow();

  @JsonProperty
  int concurrency();

  BackfillInputBuilder builder();

  static BackfillInput create(Instant start, Instant end, String component, String workflow,
                              int concurrency) {
    return newBuilder()
        .start(start)
        .end(end)
        .component(component)
        .workflow(workflow)
        .concurrency(concurrency)
        .build();
  }

  static BackfillInputBuilder newBuilder() {
    return new BackfillInputBuilder();
  }
}
