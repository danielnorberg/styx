/*-
 * -\-\-
 * Spotify Styx Scheduler Service
 * --
 * Copyright (C) 2016 - 2017 Spotify AB
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

package com.spotify.styx.workflow;

import static com.spotify.styx.util.TimeUtil.lastInstant;

import com.google.common.base.Throwables;
import com.spotify.styx.model.Partitioning;
import com.spotify.styx.model.Workflow;
import com.spotify.styx.storage.Storage;
import com.spotify.styx.util.Time;
import com.spotify.styx.util.TriggerInstantSpec;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class WorkflowInitializer {

  private final Storage storage;
  private final Time time;

  public WorkflowInitializer(Storage storage, Time time) {
    this.storage = Objects.requireNonNull(storage);
    this.time = Objects.requireNonNull(time);
  }

  public void inspectChange(Workflow workflow) {
    final Optional<Workflow> previous;
    try {
      previous = storage.workflow(workflow.id());
      storage.storeWorkflow(workflow);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    // either the workflow is completely new, or the partitioning has changed
    final Partitioning newPartitioning = workflow.schedule().partitioning();
    if (!previous.isPresent()
        || !previous.get().schedule().partitioning().equals(newPartitioning)) {
      initializeNaturalTrigger(workflow);
    }
  }

  private void initializeNaturalTrigger(Workflow workflow) {
    final Instant now = time.get();
    final Partitioning partitioning = workflow.schedule().partitioning();
    final Instant nextTrigger = lastInstant(now, partitioning);
    final Instant nextWithOffset = workflow.schedule().addOffset(nextTrigger);
    final TriggerInstantSpec nextSpec = TriggerInstantSpec.create(nextTrigger, nextWithOffset);

    try {
      storage.updateNextNaturalTrigger(workflow.id(), nextSpec);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
