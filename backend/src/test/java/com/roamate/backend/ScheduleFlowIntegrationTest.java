package com.roamate.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 회원가입_로그인_일정CRUD_소유권_전체흐름() throws Exception {
        signUp("flow-owner@test.dev", "flowowner");
        String ownerToken = login("flow-owner@test.dev");

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Trip","travelDate":"2026-08-01"}
                                """))
                .andExpect(status().isUnauthorized());

        MvcResult createResult = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Busan Trip","travelDate":"2026-08-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Busan Trip"))
                .andReturn();
        Long scheduleId = readLong(createResult, "$.data.id");

        mockMvc.perform(get("/api/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PLANNED"));

        signUp("flow-other@test.dev", "flowother");
        String otherToken = login("flow-other@test.dev");

        mockMvc.perform(get("/api/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Busan Trip v2","travelDate":"2026-08-02"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Busan Trip v2"));

        mockMvc.perform(delete("/api/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/schedules/" + scheduleId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void 잘못된_JSON은_400을_반환한다() throws Exception {
        signUp("flow-badjson@test.dev", "flowbadjson");
        String token = login("flow-badjson@test.dev");

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 즐겨찾기_추가_조회_삭제_흐름() throws Exception {
        signUp("flow-favorite@test.dev", "flowfavorite");
        String token = login("flow-favorite@test.dev");

        MvcResult placeResult = mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentId":"FAVTEST1","name":"Haeundae","latitude":35.15,"longitude":129.16}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        Long placeId = readLong(placeResult, "$.data.id");

        mockMvc.perform(post("/api/places/" + placeId + "/favorite"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/places/" + placeId + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/places/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(placeId));

        mockMvc.perform(delete("/api/places/" + placeId + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/places/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void 일정_목록은_페이지네이션된다() throws Exception {
        signUp("flow-paging@test.dev", "flowpaging");
        String token = login("flow-paging@test.dev");

        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/schedules")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"Trip %d","travelDate":"2026-08-0%d"}
                                    """.formatted(i, i)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/schedules?page=0&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    private void signUp(String email, String nickname) throws Exception {
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"password1234","nickname":"%s"}
                        """.formatted(email, nickname)));
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password1234"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private Long readLong(MvcResult result, String path) throws Exception {
        Number value = JsonPath.read(result.getResponse().getContentAsString(), path);
        return value.longValue();
    }
}
