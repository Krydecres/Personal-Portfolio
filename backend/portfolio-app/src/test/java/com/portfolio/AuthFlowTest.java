package com.portfolio;

import com.portfolio.TestSupport.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.portfolio.TestSupport.STRONG_PASSWORD;
import static com.portfolio.TestSupport.register;
import static com.portfolio.TestSupport.registerJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired
    MockMvc mvc;

    @Test
    void weakPasswordsAreRejectedWithTheBrokenRules() throws Exception {
        String[] weak = {"Sh0rt!", "alllowercase1!", "ALLUPPER1!", "NoDigits!!", "NoSpecial123", "With Space1!"};
        for (String password : weak) {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(registerJson("weak-" + Math.abs(password.hashCode()) + "@test.dev", "Weak", password)))
                    .andExpect(status().isBadRequest());
        }
        // exactly 8 chars with every class present is accepted
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ok8@test.dev", "Ok", "Ab1!efgh")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("weak2@test.dev", "Weak", "password")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.password", containsString("chữ hoa")))
                .andExpect(jsonPath("$.fieldErrors.password", containsString("chữ số")))
                .andExpect(jsonPath("$.fieldErrors.password", containsString("ký tự đặc biệt")));
    }

    @Test
    void registerSetsHardenedRefreshCookieAndNeverReturnsItInBody() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("cookie@test.dev", "Cookie", STRONG_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();
        String cookie = TestSupport.setCookieHeaders(r).getFirst();
        assertThat(cookie).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth");
    }

    @Test
    void configuredOwnerEmailBecomesAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("owner@test.dev", "Owner", STRONG_PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() throws Exception {
        register(mvc, "dup@test.dev");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("DUP@Test.dev", "Dup", STRONG_PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void loginErrorsDoNotRevealWhetherTheAccountExists() throws Exception {
        register(mvc, "enum@test.dev");
        String unknown = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.dev\",\"password\":\"Wrong!123\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String wrong = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"enum@test.dev\",\"password\":\"Wrong!123\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(message(unknown)).isEqualTo(message(wrong));
    }

    @Test
    void accountIsLockedAfterFiveFailedLogins() throws Exception {
        register(mvc, "lock@test.dev");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"lock@test.dev\",\"password\":\"Wrong!123\"}"))
                    .andExpect(status().isUnauthorized());
        }
        // even the correct password is refused while locked
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lock@test.dev\",\"password\":\"" + STRONG_PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void refreshRotatesTokenAndReplayRevokesTheWholeFamily() throws Exception {
        Session first = register(mvc, "rotate@test.dev");

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(first.cookie()))
                .andExpect(status().isOk()).andReturn();
        Session second = TestSupport.session(refreshed);
        assertThat(second.refreshCookie()).isNotEqualTo(first.refreshCookie());

        // an attacker replays the stolen, already-rotated token (test profile: reuse grace = 0s)
        mvc.perform(post("/api/auth/refresh").cookie(first.cookie())).andExpect(status().isUnauthorized());
        // ...which revokes the whole family: the legitimate newest token is dead too
        mvc.perform(post("/api/auth/refresh").cookie(second.cookie())).andExpect(status().isUnauthorized());
    }

    @Autowired
    com.portfolio.identity.internal.domain.RefreshTokenRepository refreshTokens;

    @Test
    void sessionLastsOneDayAndRefreshingNeverExtendsIt() throws Exception {
        Session first = register(mvc, "absolute@test.dev");
        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(first.cookie()))
                .andExpect(status().isOk()).andReturn();
        String cookie = refreshed.getResponse().getHeaders("Set-Cookie").getFirst();
        long maxAge = Long.parseLong(cookie.replaceAll(".*Max-Age=(\\d+).*", "$1"));
        assertThat(maxAge).isBetween(86_000L, 86_400L);

        Integer userId = com.jayway.jsonpath.JsonPath.read(refreshed.getResponse().getContentAsString(), "$.user.id");
        var tokens = refreshTokens.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId.longValue())).toList();
        assertThat(tokens).hasSize(2);
        // the rotated token keeps the expiry of the original sign-in (absolute 24h session)
        assertThat(tokens.get(1).getExpiresAt()).isEqualTo(tokens.get(0).getExpiresAt());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Session s = register(mvc, "logout@test.dev");
        mvc.perform(post("/api/auth/logout").cookie(s.cookie())).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").cookie(s.cookie())).andExpect(status().isUnauthorized());
    }

    @Test
    void cookieEndpointsRejectForeignOrigins() throws Exception {
        Session s = register(mvc, "origin@test.dev");
        mvc.perform(post("/api/auth/refresh").cookie(s.cookie()).header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/refresh").cookie(s.cookie()).header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsNeedAValidToken() throws Exception {
        mvc.perform(get("/api/account/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/account/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        // a token signed with alg=none must be rejected
        String unsigned = "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIn0.";
        mvc.perform(get("/api/admin/posts").header("Authorization", "Bearer " + unsigned))
                .andExpect(status().isUnauthorized());

        Session s = register(mvc, "me@test.dev");
        mvc.perform(get("/api/account/me").header("Authorization", s.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.dev"));
    }

    @Test
    void changingPasswordSignsOutOtherSessions() throws Exception {
        Session s = register(mvc, "change@test.dev");
        MvcResult r = mvc.perform(post("/api/account/password").header("Authorization", s.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + STRONG_PASSWORD
                                + "\",\"newPassword\":\"N3w!Password\",\"confirmPassword\":\"N3w!Password\"}"))
                .andExpect(status().isOk()).andReturn();
        mvc.perform(post("/api/auth/refresh").cookie(s.cookie())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(TestSupport.session(r).cookie())).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"change@test.dev\",\"password\":\"N3w!Password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    void errorsNeverLeakInternals() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message", not(containsString("Exception"))));
    }

    @Test
    void providersEndpointPublishesPasswordRules() throws Exception {
        mvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.google").value(false))
                .andExpect(jsonPath("$.passwordRules.length()").value(7));
    }

    private static String message(String json) {
        return com.jayway.jsonpath.JsonPath.read(json, "$.message");
    }
}
