package com.portfolio;

import com.jayway.jsonpath.JsonPath;
import com.portfolio.TestSupport.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentAndProfileTest {

    @Autowired
    MockMvc mvc;

    Session admin;
    Session user;
    Session otherUser;

    @BeforeEach
    void setUp() throws Exception {
        admin = loginOrRegisterOwner();
        user = TestSupport.register(mvc, "u-" + UUID.randomUUID() + "@test.dev");
        otherUser = TestSupport.register(mvc, "o-" + UUID.randomUUID() + "@test.dev");
    }

    private Session loginOrRegisterOwner() throws Exception {
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"owner@test.dev\",\"password\":\"" + TestSupport.STRONG_PASSWORD + "\"}"))
                .andReturn();
        if (login.getResponse().getStatus() == 200) {
            return TestSupport.session(login);
        }
        return TestSupport.register(mvc, "owner@test.dev");
    }

    private static String postJson(String title, String status) {
        return """
                {"title":"%s","summary":"Tóm tắt","content":"# Xin chào\\n\\nNội dung **markdown**.",
                 "tags":["Spring","React"],"status":"%s"}
                """.formatted(title, status);
    }

    @Test
    void onlyTheOwnerCanWritePosts() throws Exception {
        String body = postJson("Bài thử", "PUBLISHED");
        mvc.perform(post("/api/admin/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/posts").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/posts").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags", hasItem("spring")))
                .andExpect(jsonPath("$.publishedAt").isString());
    }

    @Test
    void adminListIncludesDraftsSortedByLastUpdate() throws Exception {
        mvc.perform(post("/api/admin/posts").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(postJson("Cũ hơn", "DRAFT")));
        mvc.perform(post("/api/admin/posts").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(postJson("Mới nhất", "DRAFT")));
        mvc.perform(get("/api/admin/posts").header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Mới nhất"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
        mvc.perform(get("/api/admin/posts").header("Authorization", user.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void draftsAreInvisibleToVisitorsAndSlugsAreUnique() throws Exception {
        String draft = mvc.perform(post("/api/admin/posts").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(postJson("Đà Nẵng mùa hè", "DRAFT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(org.hamcrest.Matchers.startsWith("da-nang-mua-he")))
                .andReturn().getResponse().getContentAsString();
        String slug = JsonPath.read(draft, "$.slug");
        mvc.perform(get("/api/posts/" + slug)).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts")).andExpect(jsonPath("$.content[*].slug", not(hasItem(slug))));

        String second = mvc.perform(post("/api/admin/posts").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(postJson("Đà Nẵng mùa hè", "PUBLISHED")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String secondSlug = JsonPath.read(second, "$.slug");
        org.assertj.core.api.Assertions.assertThat(secondSlug).isNotEqualTo(slug);
        mvc.perform(get("/api/posts/" + secondSlug)).andExpect(status().isOk());
    }

    @Test
    void commentsEnforceOwnership() throws Exception {
        String created = mvc.perform(post("/api/admin/posts").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(postJson("Có bình luận", "PUBLISHED")))
                .andReturn().getResponse().getContentAsString();
        String slug = JsonPath.read(created, "$.slug");

        mvc.perform(post("/api/posts/" + slug + "/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"ẩn danh\"}"))
                .andExpect(status().isUnauthorized());

        String comment = mvc.perform(post("/api/posts/" + slug + "/comments").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canDelete").value(true))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(comment, "$.id");

        mvc.perform(delete("/api/comments/" + id).header("Authorization", otherUser.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/comments/" + id).header("Authorization", admin.bearer()))
                .andExpect(status().isNoContent());
    }

    @Test
    void profileRejectsScriptUrlsAndOnlyOwnerCanEdit() throws Exception {
        String evil = """
                {"fullName":"X","githubUrl":"javascript:alert(document.cookie)","skills":["Java"]}
                """;
        mvc.perform(put("/api/admin/profile").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(evil))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.githubUrl").exists());

        String ok = """
                {"fullName":"Nguyễn Văn A","headline":"Backend developer","githubUrl":"https://github.com/example",
                 "skills":["Java","Spring Boot","Java"]}
                """;
        mvc.perform(put("/api/admin/profile").header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/profile").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills.length()").value(2));
        mvc.perform(get("/api/profile")).andExpect(jsonPath("$.fullName").value("Nguyễn Văn A"));
    }

    @Test
    void experienceAndProjectCrud() throws Exception {
        String exp = mvc.perform(post("/api/admin/profile/experiences").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"Dev\",\"organization\":\"ACME\",\"startDate\":\"2023-01-01\",\"sortOrder\":0}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer expId = JsonPath.read(exp, "$.id");
        mvc.perform(put("/api/admin/profile/experiences/" + expId).header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"Dev\",\"organization\":\"ACME\",\"startDate\":\"2023-01-01\","
                                + "\"endDate\":\"2022-01-01\",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATES"));

        mvc.perform(post("/api/admin/profile/projects").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Portfolio\",\"techStack\":[\"Spring\"],\"repoUrl\":\"https://github.com/x/y\"}"))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/admin/profile/experiences/" + expId).header("Authorization", admin.bearer()))
                .andExpect(status().isNoContent());
    }

    @Test
    void pagingIsClamped() throws Exception {
        mvc.perform(get("/api/posts?size=100000")).andExpect(jsonPath("$.size").value(50));
    }
}
