package com.madukotawatte.erp.entity;

/** Direction of a ledger row, persisted as the legacy 'money in' / 'money out' text. */
public enum LedgerDirection {
    IN("money in"),
    OUT("money out");

    private final String dbValue;

    LedgerDirection(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public LedgerDirection opposite() {
        return this == IN ? OUT : IN;
    }

    public static LedgerDirection fromDbValue(String value) {
        for (LedgerDirection d : values()) {
            if (d.dbValue.equals(value)) return d;
        }
        throw new IllegalArgumentException("Unknown ledger direction: " + value);
    }
}
