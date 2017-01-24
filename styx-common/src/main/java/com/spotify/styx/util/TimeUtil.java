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

import static com.cronutils.model.definition.CronDefinitionBuilder.instanceDefinitionFor;
import static java.time.ZoneOffset.UTC;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.spotify.styx.model.Partitioning;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;

/**
 * Static utility functions for manipulating time based on {@link Partitioning} and offsets.
 */
public class TimeUtil {

  private static final String HOURLY_CRON = "0 * * * *";
  private static final String DAILY_CRON = "0 0 * * *";
  private static final String WEEKLY_CRON = "0 0 * * MON";
  private static final String MONTHLY_CRON = "0 0 1 * *";
  private static final String YEARLY_CRON = "0 0 1 1 *";

  /**
   * Gets the last execution instant for a {@link Partitioning}, relative to a given instant.
   *
   * <p>e.g. an hourly partitioning has a last execution instant at 13:00 relative to 13:22.
   *
   * @param instant      The instant to calculate the last execution instant relative to
   * @param partitioning The partitioning of executions
   * @return an instant at the last execution time
   */
  public static Instant lastInstant(Instant instant, Partitioning partitioning) {
    final ExecutionTime executionTime = ExecutionTime.forCron(cron(partitioning));
    final ZonedDateTime utcDateTime = instant.atZone(UTC);
    final ZonedDateTime lastDateTime = executionTime.lastExecution(utcDateTime);

    return lastDateTime.toInstant();
  }

  /**
   * Gets the next execution instant for a {@link Partitioning}, relative to a given instant.
   *
   * <p>e.g. an hourly partitioning has a next execution instant at 14:00 relative to 13:22.
   *
   * @param instant      The instant to calculate the next execution instant relative to
   * @param partitioning The partitioning of executions
   * @return an instant at the next execution time
   */
  public static Instant nextInstant(Instant instant, Partitioning partitioning) {
    final ExecutionTime executionTime = ExecutionTime.forCron(cron(partitioning));
    final ZonedDateTime utcDateTime = instant.atZone(UTC);
    final ZonedDateTime nextDateTime = executionTime.nextExecution(utcDateTime);

    return nextDateTime.toInstant();
  }

  /**
   * Applies an ISO 8601 Duration to a {@link ZonedDateTime}.
   *
   * <p>Since the JDK defined different types for the different parts of a Duration
   * specification, this utility method is needed when a full Duration is to be applied to a
   * {@link ZonedDateTime}. See {@link Period} and {@link Duration}.
   *
   * <p>All date-based parts of a Duration specification (Year, Month, Day or Week) are parsed
   * using {@link Period#parse(CharSequence)} and added to the time. The remaining parts (Hour,
   * Minute, Second) are parsed using {@link Duration#parse(CharSequence)} and added to the time.
   *
   * @param time   A zoned date time to apply the offset to
   * @param offset The offset in ISO 8601 Duration format
   * @return A zoned date time with the offset applied
   */
  public static ZonedDateTime addOffset(ZonedDateTime time, String offset) {
    final int tPos = offset.indexOf('T');
    final String periodOffset;
    final String durationOffset;

    switch (tPos) {
      case -1:
        periodOffset = offset;
        break;

      case 1:
        periodOffset = "P0D";
        break;

      default:
        periodOffset = offset.substring(0, tPos);
    }

    if (tPos == -1) {
      durationOffset = "PT0S";
    } else {
      durationOffset = "P" + offset.substring(tPos);
    }

    final TemporalAmount dateAmount = Period.parse(periodOffset);
    final TemporalAmount timeAmount = Duration.parse(durationOffset);

    return time.plus(dateAmount).plus(timeAmount);
  }

  private static Cron cron(Partitioning partitioning) {
    final CronDefinition cronDefinition = instanceDefinitionFor(CronType.UNIX);
    return new CronParser(cronDefinition).parse(cronExpression(partitioning));
  }

  private static String cronExpression(Partitioning partitioning) {
    switch (partitioning.wellKnown()) {
      case HOURLY:
        return HOURLY_CRON;
      case DAILY:
        return DAILY_CRON;
      case WEEKLY:
        return WEEKLY_CRON;
      case MONTHLY:
        return MONTHLY_CRON;
      case YEARLY:
        return YEARLY_CRON;

      default:
        return partitioning.expression();
    }
  }
}
