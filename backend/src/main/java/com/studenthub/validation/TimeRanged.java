package com.studenthub.validation;

import java.time.LocalTime;

/** Implemented by request DTOs that carry a start and end time on the same day. */
public interface TimeRanged {

    LocalTime startTime();

    LocalTime endTime();
}
