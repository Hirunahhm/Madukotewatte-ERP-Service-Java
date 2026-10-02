package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.weather.WeatherDailyResponse;
import com.madukotawatte.erp.dto.weather.WeatherOutlookResponse;
import com.madukotawatte.erp.dto.weather.WeatherSyncResponse;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.WeatherInsightsService;
import com.madukotawatte.erp.service.WeatherOutlookService;
import com.madukotawatte.erp.service.WeatherSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeatherController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class WeatherControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;

    @MockBean private WeatherSyncService syncService;
    @MockBean private WeatherInsightsService insightsService;
    @MockBean private WeatherOutlookService outlookService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void history_returnsRowsForSupervisor() throws Exception {
        LocalDate d = LocalDate.of(2026, 9, 1);
        when(syncService.history(any(), any())).thenReturn(List.of(new WeatherDailyResponse(
                d, new BigDecimal("12.5"), new BigDecimal("4.0"), new BigDecimal("1.2"), null,
                new BigDecimal("30.1"), new BigDecimal("22.0"), new BigDecimal("80.0"),
                new BigDecimal("15.0"), new BigDecimal("3.1"), "archive")));

        mockMvc.perform(get("/api/v1/weather/history?from=2026-09-01&to=2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-09-01"))
                .andExpect(jsonPath("$[0].morningRainMm").value(1.2))
                .andExpect(jsonPath("$[0].source").value("archive"));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void history_rejectsReversedRange() throws Exception {
        mockMvc.perform(get("/api/v1/weather/history?from=2026-10-01&to=2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void sync_isForbiddenForSupervisor() throws Exception {
        mockMvc.perform(post("/api/v1/weather/sync").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sync_worksForAdmin() throws Exception {
        when(syncService.sync(any(), any())).thenReturn(
                new WeatherSyncResponse(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), 30, 25, 5));

        mockMvc.perform(post("/api/v1/weather/sync?from=2026-09-01&to=2026-09-30").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daysUpserted").value(30));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void outlook_returnsDays() throws Exception {
        when(outlookService.outlook()).thenReturn(new WeatherOutlookResponse(
                Instant.parse("2026-10-02T00:00:00Z"), BigDecimal.ONE, new BigDecimal("60"),
                new WeatherOutlookResponse.YieldBasis("basis", null, 0, 60), 0, null, "litres", List.of()));

        mockMvc.perform(get("/api/v1/weather/outlook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basis.sampleDays").value(0))
                .andExpect(jsonPath("$.expectedLatexTotal").doesNotExist());
    }

    @Test
    void history_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/weather/history?from=2026-09-01&to=2026-09-30"))
                .andExpect(status().is4xxClientError());
    }
}
