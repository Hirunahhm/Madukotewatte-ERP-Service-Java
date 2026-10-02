package com.madukotawatte.erp.service;

import com.madukotawatte.erp.entity.AccountType;
import com.madukotawatte.erp.entity.FinancialAccount;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.repository.FinancialAccountRepository;
import com.madukotawatte.erp.repository.MonetaryAssetTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LedgerServiceTest {

    @Mock private FinancialAccountRepository accountRepository;
    @Mock private MonetaryAssetTransactionRepository ledgerRepository;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(accountRepository, ledgerRepository);
        when(ledgerRepository.saveAndFlush(any(MonetaryAssetTransaction.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void account(String code, AccountType type, BigDecimal limit, boolean active, BigDecimal balance) {
        FinancialAccount account = FinancialAccount.builder()
                .code(code).name(code).type(type).creditLimit(limit)
                .openingBalance(BigDecimal.ZERO).openingDate(LocalDate.of(2026, 1, 1)).isActive(active).build();
        when(accountRepository.findByCodeForUpdate(code)).thenReturn(Optional.of(account));
        when(accountRepository.findById(code)).thenReturn(Optional.of(account));
        MonetaryAssetTransaction last = MonetaryAssetTransaction.builder().newAmount(balance).build();
        when(ledgerRepository.findFirstByAccountCodeOrderBySeqDesc(code)).thenReturn(Optional.of(last));
    }

    private MonetaryAssetTransaction post(String code, LedgerDirection dir, String amount) {
        return ledgerService.post(code, dir, new BigDecimal(amount), LocalDate.of(2026, 9, 1),
                LedgerCategory.EXPENSE_PAYMENT, "EXPENSE", "e1", "test");
    }

    @Test
    @DisplayName("money in raises the running balance and records last/new amounts")
    void moneyIn() {
        account("Cash", AccountType.CASH, null, true, new BigDecimal("100.00"));
        MonetaryAssetTransaction tx = post("Cash", LedgerDirection.IN, "50");

        assertEquals(new BigDecimal("100.00"), tx.getLastAmount());
        assertEquals(new BigDecimal("150.00"), tx.getNewAmount());
        assertEquals("money in", tx.getTransactionType());
        assertEquals(new BigDecimal("50.00"), tx.getAmount());
    }

    @Test
    @DisplayName("cash and bank cannot go below zero")
    void cashCannotOverdraw() {
        account("Cash", AccountType.CASH, null, true, new BigDecimal("100.00"));
        assertThrows(InsufficientFundsException.class, () -> post("Cash", LedgerDirection.OUT, "100.01"));
        assertEquals(new BigDecimal("0.00"), post("Cash", LedgerDirection.OUT, "100").getNewAmount());
    }

    @Test
    @DisplayName("a card may go down to minus its credit limit but not beyond")
    void cardLimit() {
        account("CC-A", AccountType.CREDIT_CARD, new BigDecimal("1000"), true, new BigDecimal("-900.00"));
        assertEquals(new BigDecimal("-1000.00"), post("CC-A", LedgerDirection.OUT, "100").getNewAmount());
        assertThrows(InsufficientFundsException.class, () -> post("CC-A", LedgerDirection.OUT, "100.01"));
    }

    @Test
    @DisplayName("inactive and unknown accounts are rejected")
    void inactiveAndUnknown() {
        account("Old", AccountType.BANK, null, false, new BigDecimal("100"));
        assertThrows(BadRequestException.class, () -> post("Old", LedgerDirection.IN, "1"));
        when(accountRepository.findByCodeForUpdate("Nope")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> post("Nope", LedgerDirection.IN, "1"));
    }

    @Test
    @DisplayName("amount must be positive")
    void positiveAmount() {
        account("Cash", AccountType.CASH, null, true, BigDecimal.ZERO);
        assertThrows(BadRequestException.class, () -> post("Cash", LedgerDirection.IN, "0"));
        assertThrows(BadRequestException.class, () -> post("Cash", LedgerDirection.IN, "-5"));
    }

    @Test
    @DisplayName("reverse posts the opposite direction with the same category and links reversal_of_id")
    void reversal() {
        account("Cash", AccountType.CASH, null, true, new BigDecimal("500.00"));
        MonetaryAssetTransaction original = MonetaryAssetTransaction.builder()
                .id("o1").accountCode("Cash").transactionType("money out").amount(new BigDecimal("200.00"))
                .transactionDate(LocalDate.of(2026, 8, 1)).category(LedgerCategory.EXPENSE_PAYMENT)
                .referenceType("EXPENSE").referenceId("e1").build();
        when(ledgerRepository.findById("o1")).thenReturn(Optional.of(original));
        when(ledgerRepository.findByReversalOfId("o1")).thenReturn(Optional.empty());

        MonetaryAssetTransaction reversal = ledgerService.reverse("o1", "mistake");

        assertEquals("money in", reversal.getTransactionType());
        assertEquals(LedgerCategory.EXPENSE_PAYMENT, reversal.getCategory());
        assertEquals("o1", reversal.getReversalOfId());
        assertEquals(LocalDate.of(2026, 8, 1), reversal.getTransactionDate());
        assertEquals(new BigDecimal("700.00"), reversal.getNewAmount());
    }

    @Test
    @DisplayName("an entry cannot be reversed twice")
    void doubleReversal() {
        MonetaryAssetTransaction original = MonetaryAssetTransaction.builder()
                .id("o1").accountCode("Cash").transactionType("money out").amount(new BigDecimal("1"))
                .category(LedgerCategory.EXPENSE_PAYMENT).build();
        when(ledgerRepository.findById("o1")).thenReturn(Optional.of(original));
        when(ledgerRepository.findByReversalOfId("o1")).thenReturn(Optional.of(MonetaryAssetTransaction.builder().id("r1").build()));

        assertThrows(BadRequestException.class, () -> ledgerService.reverse("o1", null));
    }

    @Test
    @DisplayName("paying a card from a bank is a CREDIT_CARD_PAYMENT pair; bank to bank is TRANSFER")
    void transferCategories() {
        account("Bank-A", AccountType.BANK, null, true, new BigDecimal("1000"));
        account("Bank-B", AccountType.BANK, null, true, new BigDecimal("0"));
        account("CC-A", AccountType.CREDIT_CARD, new BigDecimal("5000"), true, new BigDecimal("-300"));

        var plain = ledgerService.transfer("Bank-A", "Bank-B", new BigDecimal("100"), null, null);
        assertEquals(LedgerCategory.TRANSFER, plain.getFrom().getCategory());
        assertEquals(plain.getFrom().getReferenceId(), plain.getTo().getReferenceId());

        var card = ledgerService.transfer("Bank-A", "CC-A", new BigDecimal("100"), null, null);
        assertEquals(LedgerCategory.CREDIT_CARD_PAYMENT, card.getFrom().getCategory());
        assertEquals(LedgerCategory.CREDIT_CARD_PAYMENT, card.getTo().getCategory());

        assertThrows(BadRequestException.class,
                () -> ledgerService.transfer("Bank-A", "Bank-A", new BigDecimal("1"), null, null));
        assertThrows(BadRequestException.class,
                () -> ledgerService.transfer("CC-A", "Bank-A", new BigDecimal("1"), null, null));
    }

    @Test
    @DisplayName("requireCashOrBank rejects credit cards")
    void cashOrBankOnly() {
        account("CC-A", AccountType.CREDIT_CARD, new BigDecimal("5000"), true, BigDecimal.ZERO);
        account("Cash", AccountType.CASH, null, true, BigDecimal.ZERO);
        assertThrows(BadRequestException.class, () -> ledgerService.requireCashOrBank("CC-A"));
        ledgerService.requireCashOrBank("Cash");
    }
}
