package com.madukotawatte.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.madukotawatte.erp.dto.account.AccountResponse;
import com.madukotawatte.erp.dto.account.LedgerEntryResponse;
import com.madukotawatte.erp.entity.AccountType;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.AccountService;
import com.madukotawatte.erp.service.LedgerService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class AccountControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AccountResponse sampleAccount() {
        return AccountResponse.builder()
                .code("Bank-BOC").name("Bank of Ceylon").type(AccountType.BANK)
                .balance(new BigDecimal("1000.00")).isActive(true).build();
    }

    @Test
    @DisplayName("GET /accounts is readable by a supervisor")
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCanListAccounts() throws Exception {
        when(accountService.getAccounts()).thenReturn(List.of(sampleAccount()));

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("Bank-BOC"))
                .andExpect(jsonPath("$[0].balance").value(1000.00));
    }

    @Test
    @DisplayName("POST /accounts is admin only")
    @WithMockUser(roles = "SUPERVISOR")
    void supervisorCannotCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"name\":\"X\",\"type\":\"BANK\"}"))
                .andExpect(status().isForbidden());
        verify(accountService, never()).createAccount(any());
    }

    @Test
    @DisplayName("POST /accounts creates an account and validates the code")
    @WithMockUser(roles = "ADMIN")
    void adminCreatesAccount() throws Exception {
        when(accountService.createAccount(any())).thenReturn(sampleAccount());

        mockMvc.perform(post("/api/v1/accounts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"Bank-BOC\",\"name\":\"Bank of Ceylon\",\"type\":\"BANK\",\"openingBalance\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("Bank-BOC"));

        mockMvc.perform(post("/api/v1/accounts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad code!\",\"name\":\"X\",\"type\":\"BANK\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /accounts/{code} delegates to the service")
    @WithMockUser(roles = "ADMIN")
    void adminUpdatesAccount() throws Exception {
        when(accountService.updateAccount(eq("Bank-BOC"), any())).thenReturn(sampleAccount());

        mockMvc.perform(put("/api/v1/accounts/Bank-BOC").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openingBalance\":2000}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /accounts/transfers returns 400 when funds are insufficient")
    @WithMockUser(roles = "ADMIN")
    void transferInsufficientFunds() throws Exception {
        when(ledgerService.transfer(any(), any(), any(), any(), any()))
                .thenThrow(new InsufficientFundsException("Insufficient funds in Bank-BOC"));

        mockMvc.perform(post("/api/v1/accounts/transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountCode\":\"Bank-BOC\",\"toAccountCode\":\"Cash\",\"amount\":50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient funds in Bank-BOC"));
    }

    @Test
    @DisplayName("POST /accounts/transfers rejects a non-positive amount")
    @WithMockUser(roles = "ADMIN")
    void transferValidation() throws Exception {
        mockMvc.perform(post("/api/v1/accounts/transfers").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountCode\":\"Bank-BOC\",\"toAccountCode\":\"Cash\",\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /accounts/{code}/entries creates a manual ledger entry")
    @WithMockUser(roles = "ADMIN")
    void manualEntry() throws Exception {
        when(accountService.createManualEntry(eq("Cash"), any())).thenReturn(
                LedgerEntryResponse.builder().id("e1").accountCode("Cash").category(LedgerCategory.OWNER_CAPITAL)
                        .amount(new BigDecimal("500")).build());

        mockMvc.perform(post("/api/v1/accounts/Cash/entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direction\":\"IN\",\"category\":\"OWNER_CAPITAL\",\"amount\":500}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("OWNER_CAPITAL"));
    }

    @Test
    @DisplayName("an unknown category value is a 400, not a 500")
    @WithMockUser(roles = "ADMIN")
    void unknownCategoryIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/ledger").param("category", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("reversing an entry that belongs to a business record is refused")
    @WithMockUser(roles = "ADMIN")
    void reverseLinkedEntryRefused() throws Exception {
        when(ledgerService.getEntry("e1")).thenReturn(LedgerEntryResponse.builder()
                .id("e1").referenceType("SALE_LATEX").referenceId("s1").reversible(false).reversed(false).build());

        mockMvc.perform(post("/api/v1/accounts/ledger/e1/reverse").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(ledgerService, never()).reverse(any(), any());
    }
}
