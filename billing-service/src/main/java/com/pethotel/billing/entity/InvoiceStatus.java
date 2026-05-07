package com.pethotel.billing.entity;

// Жизненный цикл счёта: UNPAID (создан) → PAID (оплачен рецепцией при выезде).
// Обратный переход невозможен — оплата необратима.
public enum InvoiceStatus {
    UNPAID,  // счёт существует, ещё не оплачен
    PAID     // оплачен; повторный вызов pay() бросает IllegalStateException
}
