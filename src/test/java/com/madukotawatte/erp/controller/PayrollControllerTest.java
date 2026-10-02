package com.madukotawatte.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.madukotawatte.erp.dto.payroll.PayrollPreviewRow;
import com.madukotawatte.erp.dto.payroll.PayslipRequest;
import com.madukotawatte.erp.dto.payroll.PayslipResponse;
import com.madukotawatte.erp.entity.PayType;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.PayrollService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PayrollController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class PayrollControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;
    @MockBean private PayrollService payrollService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private PayslipRequest request;
    private PayslipResponse response;

    @BeforeEach
    void setUp() {
        request = new PayslipRequest();
        request.setEmployeeId("emp-1");
        request.setPeriodStart(LocalDate.of(2026, 9, 1));
        request.setPeriodEnd(LocalDate.of(2026, 9, 30));
        request.setAdvanceDeduction(new BigDecimal("5000"));
        request.setAccountCode("Bank-BOC");
        request.setPaymentDate(LocalDate.of(2026, 9, 30));

        response = PayslipResponse.builder().payslipId("ps-1").employeeId("emp-1").employeeName("Nimal")
                .grossPay(new BigDecimal("45000.00")).advanceDeduction(new BigDecimal("5000.00"))
                .netPay(new BigDecimal("40000.00")).status("PAID").build();
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("preview: supervisor can read")
    void preview() throws Exception {
        PayrollPreviewRow row = PayrollPreviewRow.builder().employeeId("emp-1").employeeName("Nimal")
                .payType(PayType.FIXED_MONTHLY).grossPay(new BigDecimal("45000.00")).build();
        when(payrollService.preview(eq(LocalDate.of(2026, 9, 1)), eq(LocalDate.of(2026, 9, 30)), any()))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/payroll/preview").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value("emp-1"))
                .andExpect(jsonPath("$[0].payType").value("FIXED_MONTHLY"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("create payslip: admin gets 201")
    void createPayslip() throws Exception {
        when(payrollService.createPayslip(any(PayslipRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payroll/payslips").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payslipId").value("ps-1"))
                .andExpect(jsonPath("$.netPay").value(40000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("create payslip: missing account gives 400")
    void createPayslipMissingAccount() throws Exception {
        request.setAccountCode(null);
        mockMvc.perform(post("/api/v1/payroll/payslips").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("create payslip: business rule violation gives 400 with the message")
    void createPayslipRuleViolation() throws Exception {
        when(payrollService.createPayslip(any(PayslipRequest.class)))
                .thenThrow(new BadRequestException("advance deduction exceeds the outstanding advances"));

        mockMvc.perform(post("/api/v1/payroll/payslips").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("advance deduction exceeds the outstanding advances"));
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("create payslip: supervisor gets 403")
    void createPayslipSupervisorForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/payroll/payslips").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("bulk: array body gives 201 with all payslips")
    void bulk() throws Exception {
        when(payrollService.createBulk(any())).thenReturn(List.of(response));

        mockMvc.perform(post("/api/v1/payroll/payslips/bulk").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].payslipId").value("ps-1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("void: admin gets 200 and the reason is passed on")
    void voidPayslip() throws Exception {
        when(payrollService.voidPayslip(eq("ps-1"), eq("wrong amount"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payroll/payslips/ps-1/void").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"wrong amount\"}"))
                .andExpect(status().isOk());
        verify(payrollService).voidPayslip("ps-1", "wrong amount");
    }

    @Test
    @DisplayName("unauthenticated gets 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/payroll/summary").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isUnauthorized());
    }
}
