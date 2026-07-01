package br.com.bankApi.transaction;

/**
 * Defines the strict, permitted types of transactions in the banking system.
 */
public enum TransactionType {
    TRANSFER,   // Movement between two internal accounts
    DEPOSIT,    // Money entering the system (External / ATM)
    WITHDRAWAL, // Money leaving the system (External / ATM)
    REFUND,     // Reversal of a previous transaction
    FEE         // Bank administrative charges
}
