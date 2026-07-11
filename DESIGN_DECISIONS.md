# Trade Execution Design

## API contract

`POST /api/trades/execute` accepts a market order:

```json
{
  "userId": 1,
  "pairName": "BTCUSDT",
  "tradeType": "BUY",
  "quantity": 0.1
}
```

`pairName` is case-insensitive and normalized to uppercase. `quantity` is the amount of the pair's base currency and must be positive with no more than eight decimal places.

## Execution assumptions

- BUY executes at the latest best ask, debits `quantity * ask` from the quote wallet, and credits `quantity` to the base wallet.
- SELL executes at the latest best bid, debits `quantity` from the base wallet, and credits `quantity * bid` to the quote wallet.
- Monetary totals are rounded to the database's eight-decimal scale using `HALF_UP`. Fees and slippage are out of scope.
- A wallet is created only when a successful trade first credits that asset, matching the business requirement.
- A missing user or pair returns `404`; an inactive pair returns `409`; insufficient balance returns `422`; and absence of a market price returns `503`. Invalid request bodies return `400` with field-level errors.

## Consistency

Trade execution is one database transaction. The user row is locked while a trade is processed, and debits only succeed when enough balance remains. Wallet changes and the trade record therefore commit or roll back together.
