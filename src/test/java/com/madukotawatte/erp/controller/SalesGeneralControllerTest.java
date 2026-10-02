package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.sales.SalesGeneralResponse;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.SalesService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalesGeneralController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class SalesGeneralControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired private MockMvc mockMvc;

    @MockBean private SalesService salesService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private static final String BODY = """
            {"product":"Cocoa","quantity":10,"unit":"kg","unitPrice":900,"buyerName":"Perera",
             "saleDate":"2026-06-01","dueDate":"2026-06-20"}""";

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201() throws Exception {
        when(salesService.createSalesGeneral(any())).thenReturn(SalesGeneralResponse.builder()
                .saleId("s1").product("Cocoa").totalAmount(new BigDecimal("9000.00")).buyerName("Perera")
                .saleDate(LocalDate.of(2026, 6, 1)).isPaid(false).build());

        mockMvc.perform(post("/api/v1/sales/general").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleId").value("s1"))
                .andExpect(jsonPath("$.totalAmount").value(9000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_rejectsMissingQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/sales/general").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product\":\"Cocoa\",\"unit\":\"kg\",\"unitPrice\":5,\"saleDate\":\"2026-06-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void create_forbiddenForSupervisor() throws Exception {
        mockMvc.perform(post("/api/v1/sales/general").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPERVISOR")
    void options_visibleToSupervisor() throws Exception {
        mockMvc.perform(get("/api/v1/sales/general/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0]").value("Cocoa"))
                .andExpect(jsonPath("$.units[0]").value("kg"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void undoPayment_returnsSale() throws Exception {
        when(salesService.undoSalesGeneralPayment("s1")).thenReturn(SalesGeneralResponse.builder()
                .saleId("s1").isPaid(false).status("pending").build());

        mockMvc.perform(post("/api/v1/sales/general/s1/payment/undo").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPaid").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_paidSaleRefusedWithMessage() throws Exception {
        doThrow(new BadRequestException("This sale is already paid. Undo the payment first, then delete it"))
                .when(salesService).deleteSalesGeneral(eq("s1"));

        mockMvc.perform(delete("/api/v1/sales/general/s1").with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This sale is already paid. Undo the payment first, then delete it"));
    }
}
