package com.studenthub.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.studenthub.support.IntegrationTestSupport;

/** Dashboard and attendance numbers come from the database, including the empty case. */
class DashboardIntegrationTest extends IntegrationTestSupport {

    @Test
    void aBrandNewAccountGetsZerosNotErrors() throws Exception {
        Session session = register("Empty Student");
        mockMvc.perform(as(session, get("/api/dashboard/summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Empty"))
                .andExpect(jsonPath("$.assignments.total").value(0))
                .andExpect(jsonPath("$.assignments.overdue").value(0))
                .andExpect(jsonPath("$.attendance.overallPercentage").doesNotExist())
                .andExpect(jsonPath("$.attendance.subjects").value(0))
                .andExpect(jsonPath("$.study.minutesThisWeek").value(0))
                .andExpect(jsonPath("$.todaysClasses.length()").value(0));

        mockMvc.perform(as(session, get("/api/dashboard/analytics?weeks=6")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeks").value(6))
                .andExpect(jsonPath("$.assignments.completionRate").doesNotExist())
                .andExpect(jsonPath("$.assignments.weeklyTrend.length()").value(6))
                .andExpect(jsonPath("$.weeklyStudy.length()").value(6))
                .andExpect(jsonPath("$.subjectAttendance.length()").value(0));
    }

    @Test
    void assignmentCountsAndOverdueAreCalculated() throws Exception {
        Session session = register("Busy Student");
        String yesterday = Instant.now().minus(1, ChronoUnit.DAYS).toString();
        String nextWeek = Instant.now().plus(7, ChronoUnit.DAYS).toString();

        mockMvc.perform(as(session, post("/api/assignments"))
                        .content(json(Map.of("title", "Late essay", "dueAt", yesterday, "priority", "HIGH"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.overdue").value(true));
        long done = read(mockMvc.perform(as(session, post("/api/assignments"))
                        .content(json(Map.of("title", "Finished lab", "dueAt", yesterday, "priority", "LOW"))))
                .andReturn()).get("id").asLong();
        mockMvc.perform(as(session, patch("/api/assignments/" + done + "/status"))
                        .content(json(Map.of("status", "COMPLETED"))))
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        mockMvc.perform(as(session, post("/api/assignments"))
                .content(json(Map.of("title", "Project", "dueAt", nextWeek, "priority", "MEDIUM",
                        "status", "IN_PROGRESS"))));

        mockMvc.perform(as(session, get("/api/dashboard/summary")))
                .andExpect(jsonPath("$.assignments.total").value(3))
                .andExpect(jsonPath("$.assignments.pending").value(1))
                .andExpect(jsonPath("$.assignments.inProgress").value(1))
                .andExpect(jsonPath("$.assignments.completed").value(1))
                .andExpect(jsonPath("$.assignments.overdue").value(1))
                .andExpect(jsonPath("$.upcomingDeadlines.length()").value(1))
                .andExpect(jsonPath("$.upcomingDeadlines[0].title").value("Project"));

        mockMvc.perform(as(session, get("/api/assignments?overdue=true")))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Late essay"));
        mockMvc.perform(as(session, get("/api/dashboard/analytics")))
                .andExpect(jsonPath("$.assignments.completionRate").value(33.3));
    }

    @Test
    void attendanceCombinesPriorClassesWithRecordsAndIgnoresCancelled() throws Exception {
        Session session = register("Regular Student");
        long subject = read(mockMvc.perform(as(session, post("/api/subjects"))
                        .content(json(Map.of("name", "Microprocessors", "code", "mp-301", "targetPercentage", 75,
                                "priorAttended", 10, "priorTotal", 12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MP-301"))
                .andReturn()).get("id").asLong();

        String[] statuses = {"PRESENT", "PRESENT", "PRESENT", "ABSENT", "CANCELLED"};
        for (int i = 0; i < statuses.length; i++) {
            mockMvc.perform(as(session, post("/api/subjects/" + subject + "/attendance"))
                            .content(json(Map.of("classDate", LocalDate.now().minusDays(i + 1L).toString(),
                                    "status", statuses[i]))))
                    .andExpect(status().isCreated());
        }

        // 13 present of 16 counted = 81.25%; 13*100 - 75*16 = 100 -> can miss floor(100/75) = 1
        mockMvc.perform(as(session, get("/api/subjects/" + subject)))
                .andExpect(jsonPath("$.attendance.present").value(13))
                .andExpect(jsonPath("$.attendance.absent").value(3))
                .andExpect(jsonPath("$.attendance.cancelled").value(1))
                .andExpect(jsonPath("$.attendance.totalCounted").value(16))
                .andExpect(jsonPath("$.attendance.percentage").value(81.25))
                .andExpect(jsonPath("$.attendance.meetsTarget").value(true))
                .andExpect(jsonPath("$.attendance.classesYouCanMiss").value(1));

        mockMvc.perform(as(session, get("/api/subjects?belowTarget=true"))).andExpect(jsonPath("$.length()").value(0));

        // Raise the target to 90%: now below target, needs ceil((90*16 - 1300) / 10) = 14 classes.
        mockMvc.perform(as(session, put("/api/subjects/" + subject))
                        .content(json(Map.of("name", "Microprocessors", "targetPercentage", 90,
                                "priorAttended", 10, "priorTotal", 12))))
                .andExpect(jsonPath("$.attendance.meetsTarget").value(false))
                .andExpect(jsonPath("$.attendance.classesNeededToReachTarget").value(14));
        mockMvc.perform(as(session, get("/api/subjects?belowTarget=true"))).andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(as(session, get("/api/dashboard/summary")))
                .andExpect(jsonPath("$.attendance.overallPercentage").value(81.25))
                .andExpect(jsonPath("$.attendance.subjectsBelowTarget").value(1));
    }
}
