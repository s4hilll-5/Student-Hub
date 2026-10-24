package com.studenthub.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.studenthub.support.IntegrationTestSupport;

/**
 * Users can only ever see and change their own records. Cross-user access answers 404
 * (not 403) so that record ids belonging to other users are not even confirmed to exist.
 */
class OwnershipIntegrationTest extends IntegrationTestSupport {

    private Map<String, Object> assignmentBody(Long subjectId) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Operating systems lab");
        body.put("dueAt", Instant.now().plus(3, ChronoUnit.DAYS).toString());
        body.put("priority", "HIGH");
        if (subjectId != null) {
            body.put("subjectId", subjectId);
        }
        return body;
    }

    private long createAssignment(Session session, Long subjectId) throws Exception {
        return read(mockMvc.perform(as(session, post("/api/assignments")).content(json(assignmentBody(subjectId))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    @Test
    void aStudentCannotReadChangeOrDeleteAnotherStudentsAssignment() throws Exception {
        Session alice = register("Alice Fernandes");
        Session bob = register("Bob Thomas");
        long aliceAssignment = createAssignment(alice, null);

        mockMvc.perform(as(bob, get("/api/assignments/" + aliceAssignment))).andExpect(status().isNotFound());
        mockMvc.perform(as(bob, put("/api/assignments/" + aliceAssignment)).content(json(assignmentBody(null))))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(bob, patch("/api/assignments/" + aliceAssignment + "/status"))
                        .content(json(Map.of("status", "COMPLETED"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(bob, delete("/api/assignments/" + aliceAssignment))).andExpect(status().isNotFound());
        mockMvc.perform(as(bob, get("/api/assignments"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Alice's assignment is untouched.
        mockMvc.perform(as(alice, get("/api/assignments/" + aliceAssignment))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void aUserIdInTheRequestBodyIsIgnored() throws Exception {
        Session alice = register("Alice Pinto");
        Session bob = register("Bob DSouza");
        Map<String, Object> body = assignmentBody(null);
        body.put("userId", alice.userId());

        mockMvc.perform(as(bob, post("/api/assignments")).content(json(body))).andExpect(status().isCreated());

        mockMvc.perform(as(alice, get("/api/assignments"))).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(as(bob, get("/api/assignments"))).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void anotherStudentsSubjectCannotBeReferencedOrUsedForAttendance() throws Exception {
        Session alice = register("Alice Kapoor");
        Session bob = register("Bob Varma");
        long aliceSubject = createSubject(alice, "Computer Networks");

        mockMvc.perform(as(bob, post("/api/assignments")).content(json(assignmentBody(aliceSubject))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("subjectId"));
        mockMvc.perform(as(bob, get("/api/subjects/" + aliceSubject))).andExpect(status().isNotFound());
        mockMvc.perform(as(bob, get("/api/subjects/" + aliceSubject + "/attendance"))).andExpect(status().isNotFound());
        mockMvc.perform(as(bob, post("/api/subjects/" + aliceSubject + "/attendance"))
                        .content(json(Map.of("classDate", LocalDate.now().minusDays(1).toString(), "status", "PRESENT"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(bob, delete("/api/subjects/" + aliceSubject))).andExpect(status().isNotFound());
    }

    @Test
    void attendanceRecordsAreOwnedThroughTheirSubject() throws Exception {
        Session alice = register("Alice Nair");
        Session bob = register("Bob Menon");
        long aliceSubject = createSubject(alice, "Discrete Maths");
        long record = read(mockMvc.perform(as(alice, post("/api/subjects/" + aliceSubject + "/attendance"))
                        .content(json(Map.of("classDate", LocalDate.now().minusDays(2).toString(), "status", "PRESENT"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();

        mockMvc.perform(as(bob, put("/api/attendance/" + record))
                        .content(json(Map.of("classDate", LocalDate.now().minusDays(2).toString(), "status", "ABSENT"))))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(bob, delete("/api/attendance/" + record))).andExpect(status().isNotFound());
    }

    @Test
    void notesExamsAndTimetableAreScopedToTheirOwner() throws Exception {
        Session alice = register("Alice Joshi");
        Session bob = register("Bob Shetty");
        long note = read(mockMvc.perform(as(alice, post("/api/notes"))
                        .content(json(Map.of("title", "Recursion", "content", "Base case first"))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        long exam = read(mockMvc.perform(as(alice, post("/api/exams"))
                        .content(json(Map.of("title", "DBMS midterm", "examType", "MIDTERM",
                                "examAt", Instant.now().plus(10, ChronoUnit.DAYS).toString()))))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();

        mockMvc.perform(as(bob, get("/api/notes/" + note))).andExpect(status().isNotFound());
        mockMvc.perform(as(bob, delete("/api/notes/" + note))).andExpect(status().isNotFound());
        mockMvc.perform(as(bob, get("/api/exams/" + exam))).andExpect(status().isNotFound());
        mockMvc.perform(as(bob, get("/api/notes"))).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(as(bob, get("/api/exams"))).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(as(bob, get("/api/timetable"))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void studentsCannotUseAdminEndpoints() throws Exception {
        Session student = register("Chris Lobo");
        mockMvc.perform(as(student, get("/api/admin/users"))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(as(student, get("/api/admin/stats"))).andExpect(status().isForbidden());
    }
}
