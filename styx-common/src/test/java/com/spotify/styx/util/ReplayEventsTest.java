/*-
 * -\-\-
 * Spotify Styx Common
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

package com.spotify.styx.util;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.google.common.collect.Sets.newTreeSet;
import static com.spotify.styx.state.RunState.State.DONE;
import static com.spotify.styx.state.RunState.State.RUNNING;
import static com.spotify.styx.testdata.TestData.WORKFLOW_INSTANCE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.spotify.styx.model.Event;
import com.spotify.styx.model.SequenceEvent;
import com.spotify.styx.state.RunState;
import com.spotify.styx.state.Trigger;
import com.spotify.styx.storage.Storage;
import com.spotify.styx.testdata.TestData;
import java.io.IOException;
import java.util.Optional;
import java.util.SortedSet;
import org.junit.Before;
import org.junit.Test;

public class ReplayEventsTest {
  Storage storage;

  @Before
  public void setUp() throws IOException {
    storage = mock(Storage.class);
  }

  @Test
  public void restoreRunStateForActiveBackfill() throws Exception {
    SortedSet<SequenceEvent> events = newTreeSet(SequenceEvent.COUNTER_COMPARATOR);
    events.add(SequenceEvent.create(Event.triggerExecution(WORKFLOW_INSTANCE, Trigger.natural()),        0L, 0L));
    events.add(SequenceEvent.create(Event.halt(WORKFLOW_INSTANCE),                                       1L, 1L));
    events.add(SequenceEvent.create(Event.triggerExecution(WORKFLOW_INSTANCE, Trigger.backfill("bf-1")), 2L, 2L));
    events.add(SequenceEvent.create(Event.dequeue(WORKFLOW_INSTANCE),                                    3L, 3L));
    events.add(SequenceEvent.create(Event.submit(WORKFLOW_INSTANCE, TestData.EXECUTION_DESCRIPTION),     4L, 4L));
    events.add(SequenceEvent.create(Event.submitted(WORKFLOW_INSTANCE, "exec-1"),                        5L, 5L));
    events.add(SequenceEvent.create(Event.started(WORKFLOW_INSTANCE),                                    6L, 6L));

    when(storage.readEvents(WORKFLOW_INSTANCE)).thenReturn(events);
    when(storage.readActiveWorkflowInstances(WORKFLOW_INSTANCE.workflowId().componentId()))
        .thenReturn(ImmutableMap.of(WORKFLOW_INSTANCE, 6L));

    RunState restoredRunState =
        ReplayEvents.getBackfillRunState(WORKFLOW_INSTANCE, storage, "bf-1").get();

    assertThat(restoredRunState.state(), is(RUNNING));
    assertThat(restoredRunState.data().trigger(), isPresent());
    assertThat(restoredRunState.data().trigger().get(), is(Trigger.backfill("bf-1")));
  }

  @Test
  public void restoreRunStateForInactiveBackfill() throws Exception {
    SortedSet<SequenceEvent> events = newTreeSet(SequenceEvent.COUNTER_COMPARATOR);
    events.add(SequenceEvent.create(Event.triggerExecution(WORKFLOW_INSTANCE, Trigger.backfill("bf-1")), 1L, 1L));
    events.add(SequenceEvent.create(Event.dequeue(WORKFLOW_INSTANCE),                                    2L, 2L));
    events.add(SequenceEvent.create(Event.submit(WORKFLOW_INSTANCE, TestData.EXECUTION_DESCRIPTION),     3L, 3L));
    events.add(SequenceEvent.create(Event.submitted(WORKFLOW_INSTANCE, "exec-1"),                        4L, 4L));
    events.add(SequenceEvent.create(Event.started(WORKFLOW_INSTANCE),                                    5L, 5L));
    events.add(SequenceEvent.create(Event.terminate(WORKFLOW_INSTANCE, 0),                               6L, 6L));
    events.add(SequenceEvent.create(Event.success(WORKFLOW_INSTANCE),                                    7L, 7L));
    events.add(SequenceEvent.create(Event.triggerExecution(WORKFLOW_INSTANCE, Trigger.adhoc("ad-hoc")),  8L, 8L));
    events.add(SequenceEvent.create(Event.dequeue(WORKFLOW_INSTANCE),                                    9L, 9L));
    events.add(SequenceEvent.create(Event.halt(WORKFLOW_INSTANCE),                                     10L, 10L));

    when(storage.readEvents(WORKFLOW_INSTANCE)).thenReturn(events);
    when(storage.readActiveWorkflowInstances(WORKFLOW_INSTANCE.workflowId().componentId()))
        .thenReturn(ImmutableMap.of());

    RunState restoredRunState =
        ReplayEvents.getBackfillRunState(WORKFLOW_INSTANCE, storage, "bf-1").get();

    assertThat(restoredRunState.state(), is(DONE));
    assertThat(restoredRunState.data().lastExit(), isPresent());
    assertThat(restoredRunState.data().lastExit().get(), is(0));
  }

  @Test
  public void returnsEmptyWithMissingBackfill() throws Exception {
    SortedSet<SequenceEvent> events = newTreeSet(SequenceEvent.COUNTER_COMPARATOR);
    events.add(SequenceEvent.create(Event.triggerExecution(WORKFLOW_INSTANCE, Trigger.backfill("bf-1")), 1L, 1L));
    events.add(SequenceEvent.create(Event.dequeue(WORKFLOW_INSTANCE),                                    2L, 2L));

    when(storage.readEvents(WORKFLOW_INSTANCE)).thenReturn(events);
    when(storage.readActiveWorkflowInstances(WORKFLOW_INSTANCE.workflowId().componentId()))
        .thenReturn(ImmutableMap.of(WORKFLOW_INSTANCE, 2L));

    Optional<RunState> restoredRunState =
        ReplayEvents.getBackfillRunState(WORKFLOW_INSTANCE, storage, "erroneous-id");

    assertThat(restoredRunState, is(Optional.empty()));
  }
}
