package com.payflow.payflow_backend.controller;

import com.payflow.payflow_backend.entity.ExternalPaymentRecord;
import com.payflow.payflow_backend.entity.Transaction;
import com.payflow.payflow_backend.repository.TransactionRepository;
import com.payflow.payflow_backend.security.JwtAuthenticationFilter;
import com.payflow.payflow_backend.security.TenantContext;
import com.payflow.payflow_backend.service.ExternalPaymentSimulatorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ExternalPaymentSimulatorController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc
@Import(ExternalPaymentSimulatorControllerTest.TestSecurityConfig.class)
class ExternalPaymentSimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private ExternalPaymentSimulatorService simulatorService;

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http)
                throws Exception {

            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .anyRequest().authenticated()
                    )
                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint(
                                    new HttpStatusEntryPoint(
                                            HttpStatus.UNAUTHORIZED
                                    )
                            )
                            .accessDeniedHandler((request, response, ex) ->
                                    response.sendError(
                                            HttpStatus.FORBIDDEN.value()
                                    )
                            )
                    );

            return http.build();
        }
    }

    @BeforeEach
    void setUp() {
        TenantContext.setOrgId(1L);
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
    }

    @Test
    void shouldAllowMerchantAdminToCreateExternalPayment()
            throws Exception {

        Transaction transaction = mock(Transaction.class);

        when(transaction.getId()).thenReturn(10L);
        when(transaction.getOrgId()).thenReturn(1L);

        ExternalPaymentRecord externalRecord =
                mock(ExternalPaymentRecord.class);

        when(transactionRepository.findByIdAndOrgId(10L, 1L))
                .thenReturn(Optional.of(transaction));

        when(simulatorService.createExternalRecord(
                transaction,
                "GATEWAY_A"
        )).thenReturn(externalRecord);

        mockMvc.perform(
                        post("/api/simulator/external-payment/10")
                                .with(user("admin").roles("MERCHANT_ADMIN"))
                                .param("gateway", "GATEWAY_A")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(transactionRepository)
                .findByIdAndOrgId(10L, 1L);

        verify(simulatorService)
                .createExternalRecord(transaction, "GATEWAY_A");

        verify(transactionRepository, never())
                .findById(10L);
    }

    @Test
    void merchantUserShouldNotCreateExternalPayment()
            throws Exception {

        mockMvc.perform(
                        post("/api/simulator/external-payment/10")
                                .with(user("merchant").roles("MERCHANT_USER"))
                                .param("gateway", "GATEWAY_A")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                transactionRepository,
                simulatorService
        );
    }

    @Test
    void shouldReturnNotFoundWhenTransactionDoesNotBelongToOrganization()
            throws Exception {

        when(transactionRepository.findByIdAndOrgId(10L, 1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        post("/api/simulator/external-payment/10")
                                .with(user("admin").roles("MERCHANT_ADMIN"))
                                .param("gateway", "GATEWAY_A")
                )
                .andExpect(status().isNotFound());

        verify(transactionRepository)
                .findByIdAndOrgId(10L, 1L);

        verify(transactionRepository, never())
                .findById(10L);

        verifyNoInteractions(simulatorService);
    }

    @Test
    void unauthenticatedUserShouldNotCreateExternalPayment()
            throws Exception {

        mockMvc.perform(
                        post("/api/simulator/external-payment/10")
                                .param("gateway", "GATEWAY_A")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                transactionRepository,
                simulatorService
        );
    }

    @Test
    void shouldRejectRequestWhenGatewayIsMissing()
            throws Exception {

        mockMvc.perform(
                        post("/api/simulator/external-payment/10")
                                .with(user("admin").roles("MERCHANT_ADMIN"))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                transactionRepository,
                simulatorService
        );
    }
}