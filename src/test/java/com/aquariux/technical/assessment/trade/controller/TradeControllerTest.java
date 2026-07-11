package com.aquariux.technical.assessment.trade.controller;

import com.aquariux.technical.assessment.trade.config.SecurityConfig;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
@Import(SecurityConfig.class)
class TradeControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TradeServiceInterface tradeService;

    @Test
    void validRequestReturnsExecutedTrade() throws Exception {
        TradeResponse response = new TradeResponse();
        response.setTradeId(42L);
        response.setUserId(1L);
        response.setPairName("BTCUSDT");
        response.setTradeType(TradeType.BUY);
        response.setQuantity(new BigDecimal("0.1"));
        response.setPrice(new BigDecimal("45010"));
        response.setTotalAmount(new BigDecimal("4501"));
        response.setTradeTime(LocalDateTime.of(2026, 7, 11, 10, 0));
        when(tradeService.executeTrade(any())).thenReturn(response);

        mockMvc.perform(post("/api/trades/execute")
                        .contentType("application/json")
                        .content("""
                                {"userId":1,"pairName":"BTCUSDT","tradeType":"BUY","quantity":0.1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeId").value(42))
                .andExpect(jsonPath("$.totalAmount").value(4501));
    }

    @Test
    void invalidRequestReturnsFieldErrorsWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/trades/execute")
                        .contentType("application/json")
                        .content("""
                                {"userId":1,"pairName":"","tradeType":"BUY","quantity":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.pairName").exists())
                .andExpect(jsonPath("$.validationErrors.quantity").exists());

        verifyNoInteractions(tradeService);
    }
}
