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
import com.aquariux.technical.assessment.trade.service.TradeServiceInterface;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeServiceInterface {

    private final TradeMapper tradeMapper;
    private final CryptoPairMapper cryptoPairMapper;
    private final CryptoPriceMapper cryptoPriceMapper;
    private final UserWalletMapper userWalletMapper;

    @Override
    @Transactional
    public TradeResponse executeTrade(TradeRequest tradeRequest) {
        String pairName = tradeRequest.getPairName().trim().toUpperCase(Locale.ROOT);
        CryptoPair pair = cryptoPairMapper.findByPairName(pairName);
        if (pair == null) {
            throw new TradeException(HttpStatus.NOT_FOUND, "Trading pair not found: " + pairName);
        }
        if (!Boolean.TRUE.equals(pair.getActive())) {
            throw new TradeException(HttpStatus.CONFLICT, "Trading pair is inactive: " + pairName);
        }

        if (userWalletMapper.lockUser(tradeRequest.getUserId()) == null) {
            throw new TradeException(HttpStatus.NOT_FOUND, "User not found: " + tradeRequest.getUserId());
        }

        CryptoPrice marketPrice = cryptoPriceMapper.findLatestPrice(pair.getId());
        if (marketPrice == null) {
            throw new TradeException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No market price is available for " + pairName);
        }

        BigDecimal executionPrice = tradeRequest.getTradeType() == TradeType.BUY
                ? marketPrice.getAskPrice()
                : marketPrice.getBidPrice();
        BigDecimal totalAmount = tradeRequest.getQuantity()
                .multiply(executionPrice)
                .setScale(8, RoundingMode.HALF_UP);

        if (tradeRequest.getTradeType() == TradeType.BUY) {
            debit(tradeRequest.getUserId(), pair.getQuoteSymbolId(), totalAmount, "quote currency");
            credit(tradeRequest.getUserId(), pair.getBaseSymbolId(), tradeRequest.getQuantity());
        } else {
            debit(tradeRequest.getUserId(), pair.getBaseSymbolId(), tradeRequest.getQuantity(), "base currency");
            credit(tradeRequest.getUserId(), pair.getQuoteSymbolId(), totalAmount);
        }

        Trade trade = new Trade();
        trade.setUserId(tradeRequest.getUserId());
        trade.setCryptoPairId(pair.getId());
        trade.setTradeType(tradeRequest.getTradeType().name());
        trade.setQuantity(tradeRequest.getQuantity());
        trade.setPrice(executionPrice);
        trade.setTotalAmount(totalAmount);
        trade.setTradeTime(LocalDateTime.now());
        tradeMapper.insert(trade);

        return toResponse(trade, pairName, tradeRequest.getTradeType());
    }

    private void debit(Long userId, Long symbolId, BigDecimal amount, String currencyRole) {
        if (userWalletMapper.debitIfSufficient(userId, symbolId, amount) == 0) {
            throw new TradeException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient " + currencyRole + " balance");
        }
    }

    private void credit(Long userId, Long symbolId, BigDecimal amount) {
        if (userWalletMapper.credit(userId, symbolId, amount) == 0) {
            userWalletMapper.insertWallet(userId, symbolId, amount);
        }
    }

    private TradeResponse toResponse(Trade trade, String pairName, TradeType tradeType) {
        TradeResponse response = new TradeResponse();
        response.setTradeId(trade.getId());
        response.setUserId(trade.getUserId());
        response.setPairName(pairName);
        response.setTradeType(tradeType);
        response.setQuantity(trade.getQuantity());
        response.setPrice(trade.getPrice());
        response.setTotalAmount(trade.getTotalAmount());
        response.setTradeTime(trade.getTradeTime());
        return response;
    }
}
