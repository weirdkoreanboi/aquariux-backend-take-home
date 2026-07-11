package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.entity.CryptoPair;
import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import com.aquariux.technical.assessment.trade.entity.Trade;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.TradeException;
import com.aquariux.technical.assessment.trade.mapper.CryptoPairMapper;
import com.aquariux.technical.assessment.trade.mapper.CryptoPriceMapper;
import com.aquariux.technical.assessment.trade.mapper.TradeMapper;
import com.aquariux.technical.assessment.trade.mapper.UserWalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

    @Mock private TradeMapper tradeMapper;
    @Mock private CryptoPairMapper cryptoPairMapper;
    @Mock private CryptoPriceMapper cryptoPriceMapper;
    @Mock private UserWalletMapper userWalletMapper;
    @InjectMocks private TradeServiceImpl tradeService;

    private CryptoPair pair;
    private CryptoPrice price;

    @BeforeEach
    void setUp() {
        pair = new CryptoPair();
        pair.setId(1L);
        pair.setBaseSymbolId(1L);
        pair.setQuoteSymbolId(3L);
        pair.setPairName("BTCUSDT");
        pair.setActive(true);

        price = new CryptoPrice();
        price.setBidPrice(new BigDecimal("45000.00000000"));
        price.setAskPrice(new BigDecimal("45010.00000000"));
    }

    @Test
    void buyUsesAskDebitsQuoteAndCreatesBaseWallet() {
        TradeRequest request = request(TradeType.BUY, "0.10000000");
        arrangeMarket();
        when(userWalletMapper.debitIfSufficient(1L, 3L, new BigDecimal("4501.00000000"))).thenReturn(1);
        when(userWalletMapper.credit(1L, 1L, request.getQuantity())).thenReturn(0);
        doAnswer(invocation -> {
            invocation.<Trade>getArgument(0).setId(99L);
            return null;
        }).when(tradeMapper).insert(any(Trade.class));

        TradeResponse response = tradeService.executeTrade(request);

        assertThat(response.getTradeId()).isEqualTo(99L);
        assertThat(response.getPairName()).isEqualTo("BTCUSDT");
        assertThat(response.getPrice()).isEqualByComparingTo("45010.00000000");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("4501.00000000");
        verify(userWalletMapper).insertWallet(1L, 1L, request.getQuantity());
        verify(tradeMapper).insert(any(Trade.class));
    }

    @Test
    void sellUsesBidDebitsBaseAndCreditsQuote() {
        TradeRequest request = request(TradeType.SELL, "0.20000000");
        arrangeMarket();
        when(userWalletMapper.debitIfSufficient(1L, 1L, request.getQuantity())).thenReturn(1);
        when(userWalletMapper.credit(1L, 3L, new BigDecimal("9000.00000000"))).thenReturn(1);

        TradeResponse response = tradeService.executeTrade(request);

        assertThat(response.getPrice()).isEqualByComparingTo("45000.00000000");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("9000.00000000");
        verify(userWalletMapper, never()).insertWallet(any(), any(), any());
    }

    @Test
    void insufficientBalanceRejectsTrade() {
        TradeRequest request = request(TradeType.BUY, "1.00000000");
        arrangeMarket();
        when(userWalletMapper.debitIfSufficient(1L, 3L, new BigDecimal("45010.00000000"))).thenReturn(0);

        assertThatThrownBy(() -> tradeService.executeTrade(request))
                .isInstanceOfSatisfying(TradeException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(tradeMapper, never()).insert(any());
        verify(userWalletMapper, never()).credit(any(), any(), any());
    }

    @Test
    void unknownPairReturnsNotFound() {
        TradeRequest request = request(TradeType.BUY, "1");
        when(cryptoPairMapper.findByPairName("UNKNOWN")).thenReturn(null);
        request.setPairName("unknown");

        assertStatus(request, HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownUserReturnsNotFound() {
        TradeRequest request = request(TradeType.BUY, "1");
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(pair);
        when(userWalletMapper.lockUser(1L)).thenReturn(null);

        assertStatus(request, HttpStatus.NOT_FOUND);
    }

    @Test
    void missingMarketPriceReturnsServiceUnavailable() {
        TradeRequest request = request(TradeType.BUY, "1");
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(pair);
        when(userWalletMapper.lockUser(1L)).thenReturn(1L);
        when(cryptoPriceMapper.findLatestPrice(1L)).thenReturn(null);

        assertStatus(request, HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void arrangeMarket() {
        when(cryptoPairMapper.findByPairName("BTCUSDT")).thenReturn(pair);
        when(userWalletMapper.lockUser(1L)).thenReturn(1L);
        when(cryptoPriceMapper.findLatestPrice(1L)).thenReturn(price);
    }

    private TradeRequest request(TradeType type, String quantity) {
        TradeRequest request = new TradeRequest();
        request.setUserId(1L);
        request.setPairName("BTCUSDT");
        request.setTradeType(type);
        request.setQuantity(new BigDecimal(quantity));
        return request;
    }

    private void assertStatus(TradeRequest request, HttpStatus status) {
        assertThatThrownBy(() -> tradeService.executeTrade(request))
                .isInstanceOfSatisfying(TradeException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(status));
    }
}
