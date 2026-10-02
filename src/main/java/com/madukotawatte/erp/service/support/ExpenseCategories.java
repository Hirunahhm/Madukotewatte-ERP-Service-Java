package com.madukotawatte.erp.service.support;

import com.madukotawatte.erp.exception.BadRequestException;

import java.util.List;

/** The standard expense categories (plan section 2.6). Single source of truth for validation and the dropdown. */
public final class ExpenseCategories {
    private ExpenseCategories() {}

    public static final List<String> ALL = List.of(
            "Fertilizer", "Agro-chemicals", "Ammonia & Latex Chemicals", "Fuel", "Transport",
            "Repairs & Maintenance", "Utilities", "Tools & Equipment", "Planting Material",
            "Salaries & Wages", "Labour", "Estate Supplies", "Insurance", "Taxes & Licences",
            "Professional Fees", "Interest", "Bank Charges", "Rent & Lease", "Other");

    /** Returns the canonical spelling of the category (case-insensitive match) or throws a 400. */
    public static String require(String type) {
        if (type != null) {
            String trimmed = type.trim();
            for (String category : ALL) {
                if (category.equalsIgnoreCase(trimmed)) {
                    return category;
                }
            }
        }
        throw new BadRequestException("Unknown expense category \"" + type + "\". Allowed: " + String.join(", ", ALL));
    }
}
