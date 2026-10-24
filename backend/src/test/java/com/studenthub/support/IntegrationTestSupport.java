package com.studenthub.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studenthub.security.RefreshTokenCookieService;

import jakarta.servlet.http.Cookie;

/**
 * Full application context + MockMvc against in-memory H2 (profile "test"). Each test
 * registers fresh users with unique emails, so tests never depend on each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** A registered user as the SPA would hold it: access token in memory, refresh token in a cookie. */
    public record Session(long userId, String email, String password, String accessToken, String refreshToken) {
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    protected Session register(String name) throws Exception {
        String email = uniqueEmail(name.toLowerCase().replace(' ', '.'));
        String password = "secret123";
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", name, "email", email, "password", password,
                                "timezone", "Asia/Kolkata"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = read(result);
        return new Session(body.at("/user/id").asLong(), email, password, body.get("accessToken").asText(),
                refreshCookie(result));
    }

    protected MockHttpServletRequestBuilder as(Session session, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected static String refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(RefreshTokenCookieService.COOKIE_NAME);
        return cookie == null ? null : cookie.getValue();
    }

    /** Mirrors the SPA: GET /api/auth/csrf sets XSRF-TOKEN, which is echoed in the X-XSRF-TOKEN header. */
    protected String fetchCsrfToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isNoContent()).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        if (cookie == null) {
            throw new IllegalStateException("XSRF-TOKEN cookie was not issued");
        }
        return cookie.getValue();
    }

    protected MockHttpServletRequestBuilder refreshRequest(String refreshToken, String csrfToken) {
        MockHttpServletRequestBuilder request = post("/api/auth/refresh")
                .cookie(new Cookie(RefreshTokenCookieService.COOKIE_NAME, refreshToken));
        if (csrfToken != null) {
            request.cookie(new Cookie("XSRF-TOKEN", csrfToken)).header("X-XSRF-TOKEN", csrfToken);
        }
        return request;
    }

    protected long createSubject(Session session, String name) throws Exception {
        MvcResult result = mockMvc.perform(as(session, post("/api/subjects"))
                        .content(json(Map.of("name", name, "targetPercentage", 75))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).get("id").asLong();
    }
}
