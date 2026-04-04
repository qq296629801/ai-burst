package com.aiburst.mag.api;

import com.aiburst.dto.PageResult;
import com.aiburst.mag.testsupport.AbstractMagControllersSliceTest;
import com.aiburst.mag.testsupport.WithMockMagUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.ServletException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MagProjectControllerWebTest extends AbstractMagControllersSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void listProjects_forbiddenWithoutAuthority() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/mag/projects").param("pageNum", "1").param("pageSize", "10")))
                .isInstanceOf(ServletException.class)
                .hasCauseInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockMagUser(userId = 10L, authorities = {"mag:project:list"})
    void listProjects_ok() throws Exception {
        when(projectService.page(any())).thenReturn(new PageResult<>(0, List.of()));
        mockMvc.perform(get("/api/mag/projects").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:manage"})
    void createProject_ok() throws Exception {
        when(projectService.create(any())).thenReturn(Map.of("id", 1L, "name", "P"));
        mockMvc.perform(post("/api/mag/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("P"));
    }

    @Test
    @WithMockMagUser(userId = 10L, authorities = {"mag:project:list"})
    void getProject_ok() throws Exception {
        when(projectService.get(eq(5L), eq(10L))).thenReturn(Map.of("id", 5L, "name", "X"));
        mockMvc.perform(get("/api/mag/projects/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:list"})
    void listMembers_ok() throws Exception {
        when(memberService.list(anyLong(), anyLong())).thenReturn(List.of());
        mockMvc.perform(get("/api/mag/projects/3/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockMagUser(authorities = {"mag:project:manage"})
    void archiveProject_ok() throws Exception {
        mockMvc.perform(delete("/api/mag/projects/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
