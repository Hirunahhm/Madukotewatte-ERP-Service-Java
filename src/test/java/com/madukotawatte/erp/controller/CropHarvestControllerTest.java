package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.cropharvest.CropHarvestResponse;
import com.madukotawatte.erp.dto.cropharvest.CropHarvestSummaryRow;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.CropHarvestService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CropHarvestController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class CropHarvestControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;

    @MockBean private CropHarvestService cropHarvestService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private static final String BODY =
            "{\"crop\":\"Cocoa\",\"harvestDate\":\"2026-09-10\",\"quantity\":45.5,\"unit\":\"kg\",\"fieldBlock\":\"A\"}";

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201() throws Exception {
        when(cropHarvestService.create(any())).thenReturn(CropHarvestResponse.builder()
                .id("h1").crop("Cocoa").harvestDate(LocalDate.of(2026, 9, 10)).quantity(new BigDecimal("45.5")).unit("kg").build());

        mockMvc.perform(post("/api/v1/crop-harvests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("h1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_unknownCropIsBadRequest() throws Exception {
        when(cropHarvestService.create(any())).thenThrow(new BadRequestException("Invalid crop 'Tea'"));

        mockMvc.perform(post("/api/v1/crop-harvests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid crop 'Tea'"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_rejectsNegativeQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/crop-harvests").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"crop\":\"Cocoa\",\"harvestDate\":\"2026-09-10\",\"quantity\":-1,\"unit\":\"kg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void summary_visibleToSupervisor() throws Exception {
        when(cropHarvestService.summary(any(), any())).thenReturn(List.of(CropHarvestSummaryRow.builder()
                .crop("Cocoa").month("2026-09").unit("kg").quantity(new BigDecimal("80")).harvests(2).build()));

        mockMvc.perform(get("/api/v1/crop-harvests/summary?from=2026-09-01&to=2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].crop").value("Cocoa"))
                .andExpect(jsonPath("$[0].month").value("2026-09"));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void create_forbiddenForSupervisor() throws Exception {
        mockMvc.perform(post("/api/v1/crop-harvests").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }
}
