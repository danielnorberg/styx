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

package com.spotify.styx.util;

import static com.spotify.styx.util.ParameterUtil.rangeOfInstants;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.spotify.styx.model.Partitioning;
import java.time.Instant;
import java.util.List;
import org.junit.Test;

public class ParameterUtilTest {

  private static final Instant TIME = Instant.parse("2016-01-19T09:11:22.333Z");

  @Test
  public void shouldFormatDate() throws Exception {
    final String date = ParameterUtil.formatDate(TIME);

    assertThat(date, is("2016-01-19"));
  }

  @Test
  public void shouldFormatDateTime() throws Exception {
    final String dateTime = ParameterUtil.formatDateTime(TIME);

    assertThat(dateTime, is("2016-01-19T09:11:22Z"));
  }

  @Test
  public void shouldFormatDateHour() throws Exception {
    final String dateHour = ParameterUtil.formatDateHour(TIME);

    assertThat(dateHour, is("2016-01-19T09"));
  }

  @Test
  public void shouldFormatMonth() throws Exception {
    final String month = ParameterUtil.formatMonth(TIME);

    assertThat(month, is("2016-01"));
  }

  @Test
  public void shouldRangeOfInstantsHours() throws Exception {
    final Instant startInstant = Instant.parse("2016-12-31T23:00:00.00Z");
    final Instant endInstant = Instant.parse("2017-01-01T02:00:00.00Z");

    List<Instant> list = rangeOfInstants(startInstant, endInstant, Partitioning.HOURS);
    assertThat(list, contains(
        Instant.parse("2016-12-31T23:00:00.00Z"),
        Instant.parse("2017-01-01T00:00:00.00Z"),
        Instant.parse("2017-01-01T01:00:00.00Z"))
    );
  }

  @Test
  public void shouldRangeOfInstantsDays() throws Exception {
    final Instant startInstant = Instant.parse("2016-12-31T00:00:00.00Z");
    final Instant endInstant = Instant.parse("2017-01-03T00:00:00.00Z");

    List<Instant> list = rangeOfInstants(startInstant, endInstant, Partitioning.DAYS);
    assertThat(list, contains(
        Instant.parse("2016-12-31T00:00:00.00Z"),
        Instant.parse("2017-01-01T00:00:00.00Z"),
        Instant.parse("2017-01-02T00:00:00.00Z"))
    );
  }

  @Test
  public void shouldRangeOfInstantsWeeks() throws Exception {
    final Instant startInstant = Instant.parse("2016-12-26T00:00:00.00Z");
    final Instant endInstant = Instant.parse("2017-01-16T00:00:00.00Z");

    List<Instant> list = rangeOfInstants(startInstant, endInstant, Partitioning.WEEKS);
    assertThat(list, contains(
        Instant.parse("2016-12-26T00:00:00.00Z"),
        Instant.parse("2017-01-02T00:00:00.00Z"),
        Instant.parse("2017-01-09T00:00:00.00Z"))
    );
  }

  @Test
  public void shouldRangeOfInstantsMonths() throws Exception {
    final Instant startInstant = Instant.parse("2017-01-01T00:00:00.00Z");
    final Instant endInstant = Instant.parse("2017-04-01T00:00:00.00Z");

    List<Instant> list = rangeOfInstants(startInstant, endInstant, Partitioning.MONTHS);
    assertThat(list, contains(
        Instant.parse("2017-01-01T00:00:00.00Z"),
        Instant.parse("2017-02-01T00:00:00.00Z"),
        Instant.parse("2017-03-01T00:00:00.00Z"))
    );
  }

  @Test
  public void shouldReturnEmptyListStartEqualsEnd() throws Exception {
    final Instant startInstant = Instant.parse("2016-12-31T23:00:00.00Z");
    final Instant endInstant = Instant.parse("2016-12-31T23:00:00.00Z");

    List<Instant> list = rangeOfInstants(startInstant, endInstant, Partitioning.HOURS);
    assertThat(list, hasSize(0));
  }

  @Test(expected=IllegalArgumentException.class)
  public void shouldRaiseRangeOfInstantsStartAfterEnd() throws Exception {
    final Instant startInstant = Instant.parse("2016-12-31T23:00:00.00Z");
    final Instant endInstant = Instant.parse("2016-01-01T01:00:00.00Z");

    rangeOfInstants(startInstant, endInstant, Partitioning.HOURS);
  }
}