package com.aquariux.technical.assessment.trade.service.impl;

import com.aquariux.technical.assessment.trade.dto.request.TradeRequest;
import com.aquariux.technical.assessment.trade.dto.response.TradeResponse;
import com.aquariux.technical.assessment.trade.enums.TradeType;
import com.aquariux.technical.assessment.trade.exception.TradeException;
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Transactional
class TradeServiceIntegrationTest {

    @Autowired private TradeServiceInterface tradeService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void buyPersistsTradeAndUpdatesBothWallets() {
        BigDecimal quoteBefore = balance(1L, 3L);
        TradeRequest request = request(1L, "BTCUSDT", TradeType.BUY, "0.10000000");

        TradeResponse response = tradeService.executeTrade(request);

        assertThat(response.getTradeId()).isNotNull();
        assertThat(balance(1L, 3L)).isEqualByComparingTo(quoteBefore.subtract(response.getTotalAmount()));
        assertThat(balance(1L, 1L)).isEqualByComparingTo("0.10000000");
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trades WHERE id = ?",
                Integer.class, response.getTradeId());
        assertThat(count).isEqualTo(1);
    }

    @Test
    void rejectedBuyDoesNotChangeWalletOrInsertTrade() {
        BigDecimal quoteBefore = balance(1L, 3L);
        Integer tradesBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trades", Integer.class);
        TradeRequest request = request(1L, "BTCUSDT", TradeType.BUY, "1000.00000000");

        assertThatThrownBy(() -> tradeService.executeTrade(request)).isInstanceOf(TradeException.class);

        assertThat(balance(1L, 3L)).isEqualByComparingTo(quoteBefore);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trades", Integer.class))
                .isEqualTo(tradesBefore);
    }

    private BigDecimal balance(Long userId, Long symbolId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM user_wallets WHERE user_id = ? AND symbol_id = ?",
                BigDecimal.class, userId, symbolId);
    }

    private TradeRequest request(Long userId, String pair, TradeType type, String quantity) {
        TradeRequest request = new TradeRequest();
        request.setUserId(userId);
        request.setPairName(pair);
        request.setTradeType(type);
        request.setQuantity(new BigDecimal(quantity));
        return request;
    }
}
