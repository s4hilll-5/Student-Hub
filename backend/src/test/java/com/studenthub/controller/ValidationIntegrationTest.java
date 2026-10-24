package com.studenthub.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.studenthub.support.IntegrationTestSupport;

class ValidationIntegrationTest extends IntegrationTestSupport {

    @Test
    void missingRequiredAssignmentFieldsAreListed() throws Exception {
        Session session = register("Val One");
        mockMvc.perform(as(session, post("/api/assignments")).content(json(Map.of("description", "no title"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("dueAt")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("priority")));
    }

    @Test
    void unknownEnumValuesAndMalformedJsonAre400() throws Exception {
        Session session = register("Val Two");
        mockMvc.perform(as(session, post("/api/assignments"))
                        .content("{\"title\":\"X\",\"dueAt\":\"2030-01-01T00:00:00Z\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("priority"));
        mockMvc.perform(as(session, post("/api/assignments")).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is missing or isn't valid JSON."));
        mockMvc.perform(as(session, get("/api/assignments?status=DONE"))).andExpect(status().isBadRequest());
    }

    @Test
    void invalidAttendanceCountsAreRejected() throws Exception {
        Session session = register("Val Three");
        mockMvc.perform(as(session, post("/api/subjects"))
                        .content(json(Map.of("name", "Physics", "priorAttended", 12, "priorTotal", 10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("priorAttended"));
        mockMvc.perform(as(session, post("/api/subjects"))
                        .content(json(Map.of("name", "Physics", "priorAttended", -1, "priorTotal", 10))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("priorAttended")));
        mockMvc.perform(as(session, post("/api/subjects"))
                        .content(json(Map.of("name", "Physics", "targetPercentage", 120))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("targetPercentage")));
    }

    @Test
    void duplicateSubjectNamesConflict() throws Exception {
        Session session = register("Val Four");
        createSubject(session, "Chemistry");
        mockMvc.perform(as(session, post("/api/subjects")).content(json(Map.of("name", "chemistry"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void attendanceCannotBeRecordedForTheFuture() throws Exception {
        Session session = register("Val Five");
        long subject = createSubject(session, "Biology");
        mockMvc.perform(as(session, post("/api/subjects/" + subject + "/attendance"))
                        .content(json(Map.of("classDate", LocalDate.now().plusDays(3).toString(), "status", "PRESENT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("classDate"));
    }

    @Test
    void timetableTimesAreValidatedAndOverlapsConflict() throws Exception {
        Session session = register("Val Six");
        long subject = createSubject(session, "Data Structures");

        mockMvc.perform(as(session, post("/api/timetable")).content(json(Map.of(
                        "subjectId", subject, "dayOfWeek", "MONDAY", "startTime", "10:00", "endTime", "09:00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("endTime"));

        mockMvc.perform(as(session, post("/api/timetable")).content(json(Map.of(
                        "subjectId", subject, "dayOfWeek", "MONDAY", "startTime", "09:00", "endTime", "10:00"))))
                .andExpect(status().isCreated());
        mockMvc.perform(as(session, post("/api/timetable")).content(json(Map.of(
                        "subjectId", subject, "dayOfWeek", "MONDAY", "startTime", "09:30", "endTime", "10:30"))))
                .andExpect(status().isConflict());
        // Back-to-back classes are fine.
        mockMvc.perform(as(session, post("/api/timetable")).content(json(Map.of(
                        "subjectId", subject, "dayOfWeek", "MONDAY", "startTime", "10:00", "endTime", "11:00"))))
                .andExpect(status().isCreated());
    }

    @Test
    void studySessionsCannotClashWithAClass() throws Exception {
        Session session = register("Val Seven");
        long subject = createSubject(session, "Algorithms");
        LocalDate nextMonday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
        mockMvc.perform(as(session, post("/api/timetable")).content(json(Map.of(
                        "subjectId", subject, "dayOfWeek", "MONDAY", "startTime", "09:00", "endTime", "10:00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(as(session, post("/api/study-sessions")).content(json(Map.of(
                        "title", "Revise graphs", "sessionDate", nextMonday.toString(),
                        "startTime", "09:30", "endTime", "11:00"))))
                .andExpect(status().isConflict());
        mockMvc.perform(as(session, post("/api/study-sessions")).content(json(Map.of(
                        "title", "Revise graphs", "sessionDate", nextMonday.toString(),
                        "startTime", "10:00", "endTime", "11:00"))))
                .andExpect(status().isCreated());
    }

    @Test
    void sortingIsRestrictedToKnownFields() throws Exception {
        Session session = register("Val Eight");
        mockMvc.perform(as(session, get("/api/assignments?sort=user.passwordHash,asc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
        mockMvc.perform(as(session, get("/api/assignments?sort=dueAt,desc"))).andExpect(status().isOk());
    }

    @Test
    void errorsNeverLeakStackTraces() throws Exception {
        Session session = register("Val Nine");
        mockMvc.perform(as(session, get("/api/does-not-exist")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("email=x"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
