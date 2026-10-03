package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.dashboard.DashboardOverviewResponse;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.DashboardOverviewService;
import com.madukotawatte.erp.service.DashboardService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class DashboardControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;

    @MockBean private DashboardService dashboardService;
    @MockBean private DashboardOverviewService dashboardOverviewService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private DashboardOverviewResponse sample() {
        BigDecimal z = BigDecimal.ZERO;
        return new DashboardOverviewResponse(LocalDate.of(2026, 10, 3), "2026-10",
                new DashboardOverviewResponse.Money(new BigDecimal("1500"), List.of(), z, z, List.of(),
                        new DashboardOverviewResponse.Party(z, z, 0, 0), new DashboardOverviewResponse.Party(z, z, 0, 0),
                        z, List.of(), z, z, 0),
                null, List.of(), null, null,
                List.of(new DashboardOverviewResponse.Alert("OVERDUE_PAYABLES", "DANGER", "t", "d", z, "/financials")));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void overview_isReadableBySupervisor() throws Exception {
        when(dashboardOverviewService.overview(eq("2026-10"))).thenReturn(sample());
        mockMvc.perform(get("/api/v1/dashboard/overview?month=2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.money.cashAndBankTotal").value(1500))
                .andExpect(jsonPath("$.alerts[0].href").value("/financials"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void overview_defaultsMonthWhenOmitted() throws Exception {
        when(dashboardOverviewService.overview(any())).thenReturn(sample());
        mockMvc.perform(get("/api/v1/dashboard/overview")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void overview_badMonthIs400() throws Exception {
        when(dashboardOverviewService.overview(eq("nope"))).thenThrow(new BadRequestException("month must be in the format YYYY-MM"));
        mockMvc.perform(get("/api/v1/dashboard/overview?month=nope")).andExpect(status().isBadRequest());
    }

    @Test
    void overview_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview")).andExpect(status().isUnauthorized());
    }
}
