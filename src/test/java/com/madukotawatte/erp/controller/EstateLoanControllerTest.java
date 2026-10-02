package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.estateloan.BorrowingRowResponse;
import com.madukotawatte.erp.dto.estateloan.BorrowingScheduleResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanResponse;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.EstateLoanService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EstateLoanController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class EstateLoanControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EstateLoanService estateLoanService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private EstateLoanResponse loan() {
        return EstateLoanResponse.builder().loanId("l1").name("BOC term loan")
                .currentBalance(new BigDecimal("900.00")).isActive(true).build();
    }

    @Test
    @DisplayName("POST /loans/{id}/payments takes a principal and interest split")
    @WithMockUser(roles = "ADMIN")
    void repayment() throws Exception {
        when(estateLoanService.recordEstateLoanPayment(eq("l1"), any())).thenReturn(loan());

        mockMvc.perform(post("/api/v1/estate-loans/loans/l1/payments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"principalAmount\":100,\"interestAmount\":20,\"accountCode\":\"Bank-BOC\",\"paymentDate\":\"2026-05-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(900.00));

        mockMvc.perform(post("/api/v1/estate-loans/loans/l1/payments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"principalAmount\":100}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("repayment above the balance surfaces as 400")
    @WithMockUser(roles = "ADMIN")
    void repaymentTooLarge() throws Exception {
        when(estateLoanService.recordEstateLoanPayment(eq("l1"), any()))
                .thenThrow(new InsufficientFundsException("Principal exceeds outstanding balance"));

        mockMvc.perform(post("/api/v1/estate-loans/loans/l1/payments").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"principalAmount\":100000,\"accountCode\":\"Bank-BOC\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /loans/{id}/drawdowns creates a drawdown; amount must be positive")
    @WithMockUser(roles = "ADMIN")
    void drawdown() throws Exception {
        when(estateLoanService.recordDrawdown(eq("l1"), any())).thenReturn(loan());

        mockMvc.perform(post("/api/v1/estate-loans/loans/l1/drawdowns").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500,\"date\":\"2026-06-01\",\"accountCode\":\"Bank-BOC\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/estate-loans/loans/l1/drawdowns").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /schedule returns the borrowings schedule")
    @WithMockUser(roles = "ADMIN")
    void schedule() throws Exception {
        BorrowingRowResponse total = BorrowingRowResponse.builder().closingBalance(new BigDecimal("1200.00")).build();
        when(estateLoanService.schedule(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(BorrowingScheduleResponse.builder().loans(List.of()).total(total).build());

        mockMvc.perform(get("/api/v1/estate-loans/schedule?from=2026-01-01&to=2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.closingBalance").value(1200.00));
    }

    @Test
    @DisplayName("undo latest movement is admin only")
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotUndo() throws Exception {
        mockMvc.perform(delete("/api/v1/estate-loans/loans/l1/movements/latest").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("undo latest movement returns the updated loan for admin")
    @WithMockUser(roles = "ADMIN")
    void adminUndo() throws Exception {
        when(estateLoanService.undoLastMovement("l1")).thenReturn(loan());
        mockMvc.perform(delete("/api/v1/estate-loans/loans/l1/movements/latest").with(csrf()))
                .andExpect(status().isOk());
    }
}
