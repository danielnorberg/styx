/*-
 * -\-\-
 * Spotify Styx Scheduler Service
 * --
 * Copyright (C) 2016 Spotify AB
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

package com.spotify.styx;

import static com.spotify.styx.testdata.TestData.FULL_DATA_ENDPOINT;
import static java.time.Instant.parse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spotify.styx.model.Workflow;
import com.spotify.styx.state.Trigger;
import com.spotify.styx.storage.Storage;
import com.spotify.styx.util.AlreadyInitializedException;
import com.spotify.styx.util.Time;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TriggerManagerTest {

  private static final Trigger NATURAL_TRIGGER = Trigger.natural();

  private static Workflow WORKFLOW_DAILY =
      Workflow.create("comp", URI.create("http:/foo"), FULL_DATA_ENDPOINT);

  @Mock
  Storage storage;
  @Mock
  TriggerListener triggerListener;

  private TriggerManager triggerManager;
  private final Time MANAGER_TIME = () -> parse("2016-10-10T13:11:11Z");

  @Before
  public void setUp() throws IOException {
    triggerManager = new TriggerManager(triggerListener, MANAGER_TIME, storage);
  }

  @Test
  public void shouldTriggerExecutionOnEnabledWithNextNaturalTrigger() throws IOException {
    setupWithNextNaturalTrigger(true, parse("2016-10-01T00:00:00Z"));
    triggerManager.tick();

    verify(triggerListener).event(WORKFLOW_DAILY, NATURAL_TRIGGER, parse("2016-10-01T00:00:00Z"));
    verify(storage).updateNextNaturalTrigger(WORKFLOW_DAILY.id(), parse("2016-10-02T00:00:00Z"));
  }

  @Test
  public void shouldNotTriggerExecutionOnDisabledWorkflowWithNextNaturalTrigger() throws IOException {
    setupWithNextNaturalTrigger(false, parse("2016-10-09T00:00:00Z"));
    triggerManager.tick();

    verify(triggerListener, never()).event(any(), any(), any());
    verify(storage).updateNextNaturalTrigger(WORKFLOW_DAILY.id(), parse("2016-10-10T00:00:00Z"));
  }

  @Test
  public void shouldNotTriggerExecutionIfNextNaturalTriggerAfterManagerTime() throws IOException {
    setupWithNextNaturalTrigger(true, parse("2016-10-11T00:00:00Z"));
    triggerManager.tick();

    verify(triggerListener, never()).event(any(), any(), any());
    verify(storage, never()).updateNextNaturalTrigger(any(), any());
  }

  @Test
  public void shouldNotTriggerExecutionOnDisabledGlobally() throws IOException {
    when(storage.globalEnabled()).thenReturn(false);
    triggerManager.tick();
    verify(triggerListener, never()).event(any(), any(), any());
    verify(storage, never()).updateNextNaturalTrigger(any(), any());
  }

  @Test
  public void shouldNotUpdateNextNaturalTriggerIfTriggerListenerThrows() throws Exception {
    setupWithNextNaturalTrigger(true, parse("2016-10-01T00:00:00Z"));
    doThrow(new RuntimeException()).when(triggerListener).event(any(), any(), any());
    triggerManager.tick();

    verify(storage, never()).updateNextNaturalTrigger(any(), any());
  }

  @Test
  public void shouldUpdateNextNaturalTriggerIfAlreadyInitialized() throws Exception {
    setupWithNextNaturalTrigger(true, parse("2016-10-01T00:00:00Z"));
    doThrow(new AlreadyInitializedException("")).when(triggerListener).event(any(), any(), any());
    triggerManager.tick();

    verify(storage).updateNextNaturalTrigger(WORKFLOW_DAILY.id(), parse("2016-10-02T00:00:00Z"));
  }

  @Test // todo: don't set up initial trigger in trigger manager
  public void shouldTriggerExecutionOnEnabledWithoutNextNaturalTrigger() throws IOException {
    setupWithoutNextNaturalTrigger(true);
    triggerManager.tick();

    verify(triggerListener).event(WORKFLOW_DAILY, NATURAL_TRIGGER, parse("2016-10-09T00:00:00Z"));
    verify(storage).updateNextNaturalTrigger(WORKFLOW_DAILY.id(), parse("2016-10-10T00:00:00Z"));
  }

  @Test // todo: don't set up initial trigger in trigger manager
  public void shouldNotTriggerExecutionOnDisabledWorkflowWithoutNextNaturalTrigger() throws IOException {
    setupWithoutNextNaturalTrigger(false);
    triggerManager.tick();

    verify(triggerListener, never()).event(any(), any(), any());
    verify(storage).updateNextNaturalTrigger(WORKFLOW_DAILY.id(), parse("2016-10-10T00:00:00Z"));
  }

  private void setupWithNextNaturalTrigger(boolean enabled, Instant nextNaturalTrigger) throws IOException {
    when(storage.globalEnabled()).thenReturn(true);
    if (enabled) {
      when(storage.enabled()).thenReturn(ImmutableSet.of(WORKFLOW_DAILY.id()));
    } else {
      when(storage.enabled()).thenReturn(ImmutableSet.of());
    }

    when(storage.workflowsWithNextNaturalTrigger())
        .thenReturn(ImmutableMap.of(WORKFLOW_DAILY, Optional.of(nextNaturalTrigger)));
  }

  private void setupWithoutNextNaturalTrigger(boolean enabled) throws IOException {
    when(storage.globalEnabled()).thenReturn(true);
    if (enabled) {
      when(storage.enabled()).thenReturn(ImmutableSet.of(WORKFLOW_DAILY.id()));
    } else {
      when(storage.enabled()).thenReturn(ImmutableSet.of());
    }

    when(storage.workflowsWithNextNaturalTrigger())
        .thenReturn(ImmutableMap.of(WORKFLOW_DAILY, Optional.empty()));
  }
}
