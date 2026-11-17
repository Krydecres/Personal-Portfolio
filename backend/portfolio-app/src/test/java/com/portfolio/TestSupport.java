package com.portfolio;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Small helpers shared by the integration tests. */
final class TestSupport {

    static final String STRONG_PASSWORD = "Str0ng!Pass";

    private TestSupport() {}

    record Session(String accessToken, String refreshCookie) {
        String bearer() {
            return "Bearer " + accessToken;
        }

        Cookie cookie() {
            return new Cookie("pf_refresh", refreshCookie);
        }
    }

    static String registerJson(String email, String name, String password) {
        return """
                {"email":"%s","displayName":"%s","password":"%s","confirmPassword":"%s"}
                """.formatted(email, name, password, password);
    }

    static Session register(MockMvc mvc, String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "Tester", STRONG_PASSWORD)))
                .andReturn();
        if (r.getResponse().getStatus() != 201) {
            throw new IllegalStateException("register failed: " + r.getResponse().getContentAsString());
        }
        return session(r);
    }

    static Session session(MvcResult r) throws Exception {
        String token = JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
        return new Session(token, refreshCookieValue(r));
    }

    static String refreshCookieValue(MvcResult r) {
        return setCookieHeaders(r).stream()
                .filter(h -> h.startsWith("pf_refresh="))
                .map(h -> h.substring("pf_refresh=".length(), h.indexOf(';')))
                .findFirst().orElse(null);
    }

    static List<String> setCookieHeaders(MvcResult r) {
        return r.getResponse().getHeaders("Set-Cookie");
    }
}
