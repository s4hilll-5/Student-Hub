package com.studenthub.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import com.studenthub.service.notification.PasswordResetNotifier;
import com.studenthub.support.IntegrationTestSupport;

class AuthIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private PasswordResetNotifier resetNotifier;

    @Test
    void registerReturnsTokensSetsAnHttpOnlyCookieAndNeverReturnsThePassword() throws Exception {
        String email = uniqueEmail("new");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Neha Shah", "email", email, "password", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String refreshCookie = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(value -> value.startsWith("studenthub_refresh="))
                .findFirst().orElseThrow();
        assertThat(refreshCookie).contains("HttpOnly").contains("SameSite=Strict").contains("Path=/api/auth");
    }

    @Test
    void duplicateEmailIsRejectedWith409RegardlessOfCase() throws Exception {
        Session existing = register("Ravi Kumar");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Someone", "email", existing.email().toUpperCase(),
                                "password", "secret123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
    }

    @Test
    void weakPasswordAndInvalidEmailAreReportedPerField() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "", "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Some fields need attention."))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("fullName")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameGeneric401() throws Exception {
        Session session = register("Meera Iyer");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", session.email(), "password", "wrongpass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail("ghost"), "password", "wrongpass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void loginSucceedsWithCorrectCredentials() throws Exception {
        Session session = register("Kabir Singh");
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", session.email(), "password", session.password()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("studenthub_refresh="))));
    }

    @Test
    void protectedEndpointsNeedAValidToken() throws Exception {
        Session session = register("Ishaan Rao");
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(session.email()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void refreshRotatesTheCookieAndRequiresTheCsrfHeader() throws Exception {
        Session session = register("Tara Das");

        // Without the CSRF header the cookie alone is not enough.
        mockMvc.perform(refreshRequest(session.refreshToken(), null)).andExpect(status().isForbidden());

        String csrf = fetchCsrfToken();
        MvcResult refreshed = mockMvc.perform(refreshRequest(session.refreshToken(), csrf))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String rotated = refreshCookie(refreshed);
        assertThat(rotated).isNotBlank().isNotEqualTo(session.refreshToken());

        // The new access token works.
        String accessToken = read(refreshed).get("accessToken").asText();
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void replayingARotatedRefreshTokenRevokesEverySession() throws Exception {
        Session session = register("Zoya Khan");
        String csrf = fetchCsrfToken();
        String rotated = refreshCookie(mockMvc.perform(refreshRequest(session.refreshToken(), csrf))
                .andExpect(status().isOk()).andReturn());

        // Replay of the old token (e.g. stolen) -> 401 and the whole family is revoked.
        mockMvc.perform(refreshRequest(session.refreshToken(), csrf)).andExpect(status().isUnauthorized());
        mockMvc.perform(refreshRequest(rotated, csrf)).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        Session session = register("Arjun Mehta");
        String csrf = fetchCsrfToken();
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("studenthub_refresh", session.refreshToken()),
                                new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrf))
                        .header("X-XSRF-TOKEN", csrf))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        hasItem(allOf(containsString("studenthub_refresh="), containsString("Max-Age=0")))));

        mockMvc.perform(refreshRequest(session.refreshToken(), csrf)).andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordAnswersIdenticallyForKnownAndUnknownEmails() throws Exception {
        Session session = register("Diya Patel");
        String known = mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", session.email()))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String unknown = mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", uniqueEmail("nobody")))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertThat(read(known).get("message")).isEqualTo(read(unknown).get("message"));
    }

    @Test
    void passwordResetFlowChangesThePasswordOnceAndEndsOldSessions() throws Exception {
        Session session = register("Rohan Gupta");
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", session.email()))))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(resetNotifier).sendPasswordReset(eq(session.email()), anyString(), link.capture(), any());
        String token = link.getValue().substring(link.getValue().indexOf("token=") + 6);

        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token, "newPassword", "brandnew123"))))
                .andExpect(status().isOk());

        // Single use.
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token, "newPassword", "another123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("token"));

        // Access tokens issued before the reset stop working immediately.
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", session.email(), "password", session.password()))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", session.email(), "password", "brandnew123"))))
                .andExpect(status().isOk());
    }

    @Test
    void deletingTheAccountRequiresThePasswordAndConfirmation() throws Exception {
        Session session = register("Anya Bose");
        mockMvc.perform(as(session, delete("/api/users/me"))
                        .content(json(Map.of("password", "wrongpass1", "confirmation", "DELETE"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(session, delete("/api/users/me"))
                        .content(json(Map.of("password", session.password(), "confirmation", "delete"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(session, delete("/api/users/me"))
                        .content(json(Map.of("password", session.password(), "confirmation", "DELETE"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", session.email(), "password", session.password()))))
                .andExpect(status().isUnauthorized());
    }
}
