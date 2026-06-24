package com.madukotawatte.erp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employee.EmployeeRequest;
import com.madukotawatte.erp.dto.employee.EmployeeResponse;
import com.madukotawatte.erp.dto.employee.EmployeeSummaryResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.security.CustomUserDetailsService;
import com.madukotawatte.erp.security.JwtAccessDeniedHandler;
import com.madukotawatte.erp.security.JwtAuthenticationEntryPoint;
import com.madukotawatte.erp.security.JwtTokenProvider;
import com.madukotawatte.erp.service.WorkforceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import({JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class EmployeeControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkforceService workforceService;

    // Required by JwtAuthenticationFilter and SecurityConfig
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final String BASE_URL = "/api/v1/employees";
    private static final String EMP_ID   = "emp-uuid-001";

    private EmployeeResponse sampleResponse;
    private EmployeeRequest  sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = EmployeeResponse.builder()
                .employeeId(EMP_ID)
                .name("Nimal Perera")
                .joinedDate(LocalDate.of(2021, 3, 1))
                .salary(new BigDecimal("45000.00"))
                .position("Tapper")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = new EmployeeRequest();
        sampleRequest.setName("Nimal Perera");
        sampleRequest.setJoinedDate(LocalDate.of(2021, 3, 1));
        sampleRequest.setSalary(new BigDecimal("45000.00"));
        sampleRequest.setPosition("Tapper");
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/employees
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees")
    class GetAllEmployees {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin → 200 with paginated list")
        void asAdmin_returns200() throws Exception {
            PageResponse<EmployeeResponse> page = PageResponse.<EmployeeResponse>builder()
                    .content(List.of(sampleResponse))
                    .pageNumber(0).pageSize(20).totalElements(1).totalPages(1).last(true)
                    .build();
            when(workforceService.getAllEmployees(any(), any())).thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].employeeId").value(EMP_ID))
                    .andExpect(jsonPath("$.content[0].name").value("Nimal Perera"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @WithMockUser(roles = "SUPERVISOR")
        @DisplayName("supervisor → 200")
        void asSupervisor_returns200() throws Exception {
            PageResponse<EmployeeResponse> page = PageResponse.<EmployeeResponse>builder()
                    .content(List.of(sampleResponse))
                    .pageNumber(0).pageSize(20).totalElements(1).totalPages(1).last(true)
                    .build();
            when(workforceService.getAllEmployees(any(), any())).thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("name filter is passed through")
        void withNameFilter_returns200() throws Exception {
            PageResponse<EmployeeResponse> page = PageResponse.<EmployeeResponse>builder()
                    .content(List.of(sampleResponse))
                    .pageNumber(0).pageSize(20).totalElements(1).totalPages(1).last(true)
                    .build();
            when(workforceService.getAllEmployees(eq("Nimal"), any())).thenReturn(page);

            mockMvc.perform(get(BASE_URL).param("name", "Nimal"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Nimal Perera"));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/employees/summary
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/summary")
    class GetAllEmployeesSummary {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin → 200 with summary list")
        void asAdmin_returns200() throws Exception {
            EmployeeSummaryResponse summary = EmployeeSummaryResponse.builder()
                    .employeeId(EMP_ID).name("Nimal Perera")
                    .position("Tapper").salary(new BigDecimal("45000.00")).isActive(true)
                    .build();
            when(workforceService.getAllEmployeesSummary()).thenReturn(List.of(summary));

            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].employeeId").value(EMP_ID))
                    .andExpect(jsonPath("$[0].name").value("Nimal Perera"))
                    .andExpect(jsonPath("$[0].isActive").value(true));
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/employees/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/{id}")
    class GetEmployee {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("existing id → 200 with employee")
        void existing_returns200() throws Exception {
            when(workforceService.getEmployee(EMP_ID)).thenReturn(sampleResponse);

            mockMvc.perform(get(BASE_URL + "/{id}", EMP_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeId").value(EMP_ID))
                    .andExpect(jsonPath("$.name").value("Nimal Perera"))
                    .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("unknown id → 404")
        void unknown_returns404() throws Exception {
            when(workforceService.getEmployee("bad-id"))
                    .thenThrow(new ResourceNotFoundException("Employee", "id", "bad-id"));

            mockMvc.perform(get(BASE_URL + "/{id}", "bad-id"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", EMP_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // POST /api/v1/employees
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /employees")
    class CreateEmployee {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin + valid body → 201 with employee")
        void asAdmin_validBody_returns201() throws Exception {
            when(workforceService.createEmployee(any())).thenReturn(sampleResponse);

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeeId").value(EMP_ID))
                    .andExpect(jsonPath("$.name").value("Nimal Perera"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("missing required fields → 400")
        void missingFields_returns400() throws Exception {
            EmployeeRequest invalid = new EmployeeRequest(); // name, joinedDate, salary missing

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "SUPERVISOR")
        @DisplayName("supervisor → 403")
        void asSupervisor_returns403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/employees/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /employees/{id}")
    class UpdateEmployee {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin + valid body → 200 updated employee")
        void asAdmin_returns200() throws Exception {
            when(workforceService.updateEmployee(eq(EMP_ID), any())).thenReturn(sampleResponse);

            mockMvc.perform(put(BASE_URL + "/{id}", EMP_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.employeeId").value(EMP_ID));
        }

        @Test
        @WithMockUser(roles = "SUPERVISOR")
        @DisplayName("supervisor → 403")
        void asSupervisor_returns403() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{id}", EMP_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{id}", EMP_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/employees/{id}
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /employees/{id}")
    class DeleteEmployee {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin → 204 no content")
        void asAdmin_returns204() throws Exception {
            doNothing().when(workforceService).deleteEmployee(EMP_ID);

            mockMvc.perform(delete(BASE_URL + "/{id}", EMP_ID).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("employee has active loans → 400")
        void employeeHasActiveLoans_returns400() throws Exception {
            doThrow(new com.madukotawatte.erp.exception.BadRequestException(
                    "Cannot delete employee with active loans"))
                    .when(workforceService).deleteEmployee(EMP_ID);

            mockMvc.perform(delete(BASE_URL + "/{id}", EMP_ID).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "SUPERVISOR")
        @DisplayName("supervisor → 403")
        void asSupervisor_returns403() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", EMP_ID).with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", EMP_ID).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/employees/{id}/attendance
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/{id}/attendance")
    class GetEmployeeAttendance {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin → 200 with paginated attendance")
        void asAdmin_returns200() throws Exception {
            AttendanceResponse att = AttendanceResponse.builder()
                    .attendanceId("att-001").employeeId(EMP_ID).employeeName("Nimal Perera")
                    .timestamp(LocalDateTime.of(2024, 6, 10, 7, 30))
                    .noOfTrees(60).noWork("none")
                    .createdAt(LocalDateTime.now())
                    .build();
            PageResponse<AttendanceResponse> page = PageResponse.<AttendanceResponse>builder()
                    .content(List.of(att))
                    .pageNumber(0).pageSize(20).totalElements(1).totalPages(1).last(true)
                    .build();
            when(workforceService.getEmployeeAttendance(eq(EMP_ID), any())).thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/{id}/attendance", EMP_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].attendanceId").value("att-001"))
                    .andExpect(jsonPath("$.content[0].noOfTrees").value(60));
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}/attendance", EMP_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/employees/{id}/attendance/range
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/{id}/attendance/range")
    class GetEmployeeAttendanceByRange {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("valid date range → 200 with list")
        void validRange_returns200() throws Exception {
            AttendanceResponse att = AttendanceResponse.builder()
                    .attendanceId("att-002").employeeId(EMP_ID).employeeName("Nimal Perera")
                    .timestamp(LocalDateTime.of(2024, 6, 15, 7, 0))
                    .noOfTrees(55).noWork("none")
                    .createdAt(LocalDateTime.now())
                    .build();
            when(workforceService.getEmployeeAttendanceByDateRange(eq(EMP_ID), any(), any()))
                    .thenReturn(List.of(att));

            mockMvc.perform(get(BASE_URL + "/{id}/attendance/range", EMP_ID)
                            .param("from", "2024-06-01T00:00:00")
                            .param("to",   "2024-06-30T23:59:59"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].attendanceId").value("att-002"))
                    .andExpect(jsonPath("$[0].noOfTrees").value(55));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("missing params → 400")
        void missingParams_returns400() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}/attendance/range", EMP_ID))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}/attendance/range", EMP_ID)
                            .param("from", "2024-06-01T00:00:00")
                            .param("to",   "2024-06-30T23:59:59"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/v1/employees/{id}/transactions
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/{id}/transactions")
    class GetEmployeeTransactions {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin → 200 with transaction list")
        void asAdmin_returns200() throws Exception {
            EmployeeTransactionResponse txn = EmployeeTransactionResponse.builder()
                    .transactionRecordId("txn-001").employeeId(EMP_ID).employeeName("Nimal Perera")
                    .type("Advance").paymentType("cash").amount(new BigDecimal("5000.00"))
                    .timestamp(LocalDateTime.now()).createdAt(LocalDateTime.now())
                    .build();
            when(workforceService.getEmployeeTransactions(EMP_ID)).thenReturn(List.of(txn));

            mockMvc.perform(get(BASE_URL + "/{id}/transactions", EMP_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].transactionRecordId").value("txn-001"))
                    .andExpect(jsonPath("$[0].type").value("Advance"))
                    .andExpect(jsonPath("$[0].paymentType").value("cash"));
        }

        @Test
        @WithMockUser(roles = "SUPERVISOR")
        @DisplayName("supervisor → 200")
        void asSupervisor_returns200() throws Exception {
            when(workforceService.getEmployeeTransactions(EMP_ID)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/{id}/transactions", EMP_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("unauthenticated → 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}/transactions", EMP_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
