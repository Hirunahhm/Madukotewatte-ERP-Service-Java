package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.finance.PayablesResponse;
import com.madukotawatte.erp.dto.finance.ReceivablesResponse;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.ExpenseService;
import com.madukotawatte.erp.service.PayablesService;
import com.madukotawatte.erp.service.ReceivablesService;
import com.madukotawatte.erp.service.support.ExpenseCategories;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReceivablesController.class, ExpenseController.class})
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class ReceivablesPayablesControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;

    @MockBean private ReceivablesService receivablesService;
    @MockBean private PayablesService payablesService;
    @MockBean private ExpenseService expenseService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void receivables_returnsAgingForSupervisor() throws Exception {
        when(receivablesService.getReceivables(any())).thenReturn(ReceivablesResponse.builder()
                .asOf(LocalDate.of(2026, 10, 2)).total(new BigDecimal("1200")).overdueTotal(BigDecimal.ZERO)
                .rows(List.of()).buckets(List.of()).byBuyer(List.of()).build());

        mockMvc.perform(get("/api/v1/finance/receivables?asOf=2026-10-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value("2026-10-02"))
                .andExpect(jsonPath("$.total").value(1200));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void payables_returnsAgingForSupervisor() throws Exception {
        when(payablesService.getPayables(any())).thenReturn(PayablesResponse.builder()
                .asOf(LocalDate.of(2026, 10, 2)).total(new BigDecimal("500")).overdueTotal(BigDecimal.ZERO)
                .rows(List.of()).buckets(List.of()).byPayee(List.of()).build());

        mockMvc.perform(get("/api/v1/expenses/payables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(500));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void categories_areExposed() throws Exception {
        when(expenseService.getCategories()).thenReturn(ExpenseCategories.ALL);

        mockMvc.perform(get("/api/v1/expenses/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Fertilizer"))
                .andExpect(jsonPath("$[18]").value("Other"));
    }

    @Test
    void categoryValidation_isCaseInsensitiveAndRejectsUnknown() {
        assertEquals("Labour", ExpenseCategories.require("labour"));
        assertEquals("Interest", ExpenseCategories.require("Interest"));
        assertThrows(RuntimeException.class, () -> ExpenseCategories.require("Labor"));
    }
}
