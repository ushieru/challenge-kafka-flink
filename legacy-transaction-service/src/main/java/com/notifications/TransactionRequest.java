package com.notifications;

public record TransactionRequest(
    Double amount,
    String merchant,
    String account
) {}
