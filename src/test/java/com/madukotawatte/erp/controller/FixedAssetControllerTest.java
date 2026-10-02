package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.fixedasset.DepreciationRowResponse;
import com.madukotawatte.erp.dto.fixedasset.DepreciationScheduleResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetResponse;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.DepreciationService;
import com.madukotawatte.erp.service.FixedAssetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FixedAssetController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class FixedAssetControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FixedAssetService fixedAssetService;

    @MockBean
    private DepreciationService depreciationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /fixed-assets validates required fields and creates with paidFromAccountCode")
    @WithMockUser(roles = "ADMIN")
    void createAsset() throws Exception {
        when(fixedAssetService.createFixedAsset(any())).thenReturn(FixedAssetResponse.builder()
                .assetId("a1").name("Hilux").category("Vehicle").purchaseAccountCode("Bank-BOC")
                .netBookValue(new BigDecimal("100.00")).build());

        mockMvc.perform(post("/api/v1/fixed-assets").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Vehicle\",\"name\":\"Hilux\",\"acquisitionDate\":\"2026-01-10\","
                                + "\"acquisitionValue\":100,\"paidFromAccountCode\":\"Bank-BOC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purchaseAccountCode").value("Bank-BOC"));

        mockMvc.perform(post("/api/v1/fixed-assets").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /fixed-assets/{id}/dispose returns the gain or loss and requires a date and proceeds")
    @WithMockUser(roles = "ADMIN")
    void disposeAsset() throws Exception {
        when(fixedAssetService.disposeFixedAsset(eq("a1"), any())).thenReturn(FixedAssetResponse.builder()
                .assetId("a1").status("disposed").gainLoss(new BigDecimal("-370000.00")).build());

        mockMvc.perform(post("/api/v1/fixed-assets/a1/dispose").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposalDate\":\"2026-09-10\",\"proceeds\":5000000,\"accountCode\":\"Cash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gainLoss").value(-370000.00));

        mockMvc.perform(post("/api/v1/fixed-assets/a1/dispose").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proceeds\":5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /fixed-assets/depreciation returns the schedule")
    @WithMockUser(roles = "ADMIN")
    void depreciationSchedule() throws Exception {
        DepreciationRowResponse total = DepreciationRowResponse.builder().chargeForPeriod(new BigDecimal("270000.00")).build();
        when(depreciationService.schedule(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(DepreciationScheduleResponse.builder().assets(List.of()).categories(List.of()).total(total).build());

        mockMvc.perform(get("/api/v1/fixed-assets/depreciation?from=2026-01-01&to=2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.chargeForPeriod").value(270000.00));

        mockMvc.perform(get("/api/v1/fixed-assets/depreciation"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /fixed-assets/{id} is admin only")
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/fixed-assets/a1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /fixed-assets/{id} returns 204 for admin")
    @WithMockUser(roles = "ADMIN")
    void adminDeletes() throws Exception {
        mockMvc.perform(delete("/api/v1/fixed-assets/a1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(fixedAssetService).deleteFixedAsset("a1");
    }
}
