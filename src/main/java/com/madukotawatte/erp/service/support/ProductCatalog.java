package com.madukotawatte.erp.service.support;

import com.madukotawatte.erp.exception.BadRequestException;

import java.util.List;

/** Allowed products/crops/units for general sales and crop harvests (mirrors the V19 CHECK constraints). */
public final class ProductCatalog {
    private ProductCatalog() {}

    public static final List<String> PRODUCTS =
            List.of("Cocoa", "Pepper", "Cinnamon", "Coffee", "Arecanut", "Cloves", "Timber", "Firewood", "Other");
    public static final List<String> CROPS =
            List.of("Cocoa", "Pepper", "Cinnamon", "Coffee", "Arecanut", "Cloves", "Other");
    public static final List<String> UNITS =
            List.of("kg", "g", "nuts", "bunches", "litres", "units", "cubic_ft", "other");

    public static void require(List<String> allowed, String value, String field) {
        if (value == null || !allowed.contains(value)) {
            throw new BadRequestException("Invalid " + field + " '" + value + "'. Allowed: " + String.join(", ", allowed));
        }
    }
}
