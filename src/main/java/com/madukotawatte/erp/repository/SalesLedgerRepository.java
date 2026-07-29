package com.madukotawatte.erp.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cross-table read queries over the 5 sales_* tables (sales_latex, sales_rubber_solid,
 * sales_manioc, sales_coconut, sales_banana). These tables have no shared entity/superclass,
 * so a plain native-SQL UNION ALL is used instead of Specification-based filtering.
 */
@Repository
public class SalesLedgerRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String UNION_ROWS = """
            SELECT sale_id, 'latex' AS category, load_id, CAST(created_at AS date) AS sale_date,
                   total_amount AS amount,
                   CASE WHEN is_payment_received THEN 'paid' ELSE 'pending' END AS status,
                   NULL AS payment_type
            FROM sales_latex
            WHERE (CAST(:category AS varchar) IS NULL OR CAST(:category AS varchar) = 'latex')
              AND (CAST(:status AS varchar) IS NULL OR (CAST(:status AS varchar) = 'paid') = is_payment_received)
              AND (CAST(:from AS date) IS NULL OR CAST(created_at AS date) >= CAST(:from AS date))
              AND (CAST(:to AS date) IS NULL OR CAST(created_at AS date) <= CAST(:to AS date))
            UNION ALL
            SELECT sale_id, 'rubber-solid', load_id, sale_date, (mass * unit_price), status, payment_type
            FROM sales_rubber_solid
            WHERE (CAST(:category AS varchar) IS NULL OR CAST(:category AS varchar) = 'rubber-solid')
              AND (CAST(:status AS varchar) IS NULL OR status = CAST(:status AS varchar))
              AND (CAST(:from AS date) IS NULL OR sale_date >= CAST(:from AS date))
              AND (CAST(:to AS date) IS NULL OR sale_date <= CAST(:to AS date))
            UNION ALL
            SELECT sale_id, 'manioc', load_id, sale_date, (mass * unit_price), status, payment_type
            FROM sales_manioc
            WHERE (CAST(:category AS varchar) IS NULL OR CAST(:category AS varchar) = 'manioc')
              AND (CAST(:status AS varchar) IS NULL OR status = CAST(:status AS varchar))
              AND (CAST(:from AS date) IS NULL OR sale_date >= CAST(:from AS date))
              AND (CAST(:to AS date) IS NULL OR sale_date <= CAST(:to AS date))
            UNION ALL
            SELECT sale_id, 'coconut', load_id, sale_date, (mass * unit_price), status, payment_type
            FROM sales_coconut
            WHERE (CAST(:category AS varchar) IS NULL OR CAST(:category AS varchar) = 'coconut')
              AND (CAST(:status AS varchar) IS NULL OR status = CAST(:status AS varchar))
              AND (CAST(:from AS date) IS NULL OR sale_date >= CAST(:from AS date))
              AND (CAST(:to AS date) IS NULL OR sale_date <= CAST(:to AS date))
            UNION ALL
            SELECT sale_id, 'banana', load_id, sale_date, (mass * unit_price), status, payment_type
            FROM sales_banana
            WHERE (CAST(:category AS varchar) IS NULL OR CAST(:category AS varchar) = 'banana')
              AND (CAST(:status AS varchar) IS NULL OR status = CAST(:status AS varchar))
              AND (CAST(:from AS date) IS NULL OR sale_date >= CAST(:from AS date))
              AND (CAST(:to AS date) IS NULL OR sale_date <= CAST(:to AS date))
            """;

    @SuppressWarnings("unchecked")
    public List<Object[]> findLedgerRows(String category, String status, LocalDate from, LocalDate to, int limit, int offset) {
        Query query = entityManager.createNativeQuery(UNION_ROWS + " ORDER BY sale_date DESC, sale_id LIMIT :limit OFFSET :offset");
        bindFilters(query, category, status, from, to);
        query.setParameter("limit", limit);
        query.setParameter("offset", offset);
        return query.getResultList();
    }

    public long countLedgerRows(String category, String status, LocalDate from, LocalDate to) {
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + UNION_ROWS + ") ledger");
        bindFilters(query, category, status, from, to);
        return ((Number) query.getSingleResult()).longValue();
    }

    private void bindFilters(Query query, String category, String status, LocalDate from, LocalDate to) {
        query.setParameter("category", category);
        query.setParameter("status", status);
        query.setParameter("from", from);
        query.setParameter("to", to);
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> sumByCategory() {
        Query query = entityManager.createNativeQuery("""
                SELECT 'latex', COALESCE(SUM(total_amount), 0) FROM sales_latex
                UNION ALL
                SELECT 'rubber-solid', COALESCE(SUM(mass * unit_price), 0) FROM sales_rubber_solid
                UNION ALL
                SELECT 'manioc', COALESCE(SUM(mass * unit_price), 0) FROM sales_manioc
                UNION ALL
                SELECT 'coconut', COALESCE(SUM(mass * unit_price), 0) FROM sales_coconut
                UNION ALL
                SELECT 'banana', COALESCE(SUM(mass * unit_price), 0) FROM sales_banana
                """);
        return query.getResultList();
    }

    /** Returns a single row: [receivedTotal, pendingTotal]. */
    public Object[] sumByPaidStatus() {
        Query query = entityManager.createNativeQuery("""
                SELECT
                    COALESCE(SUM(CASE WHEN paid THEN amount ELSE 0 END), 0) AS received,
                    COALESCE(SUM(CASE WHEN NOT paid THEN amount ELSE 0 END), 0) AS pending
                FROM (
                    SELECT total_amount AS amount, is_payment_received AS paid FROM sales_latex
                    UNION ALL
                    SELECT mass * unit_price, is_paid FROM sales_rubber_solid
                    UNION ALL
                    SELECT mass * unit_price, is_paid FROM sales_manioc
                    UNION ALL
                    SELECT mass * unit_price, is_paid FROM sales_coconut
                    UNION ALL
                    SELECT mass * unit_price, is_paid FROM sales_banana
                ) t
                """);
        return (Object[]) query.getSingleResult();
    }

    /** Returns [sale_date, amount] rows across all 5 tables within the given window, for in-Java bucketing. */
    @SuppressWarnings("unchecked")
    public List<Object[]> findAmountsInRange(LocalDate from, LocalDate to) {
        Query query = entityManager.createNativeQuery("""
                SELECT CAST(created_at AS date) AS sale_date, total_amount AS amount
                FROM sales_latex WHERE CAST(created_at AS date) BETWEEN :from AND :to
                UNION ALL
                SELECT sale_date, mass * unit_price FROM sales_rubber_solid WHERE sale_date BETWEEN :from AND :to
                UNION ALL
                SELECT sale_date, mass * unit_price FROM sales_manioc WHERE sale_date BETWEEN :from AND :to
                UNION ALL
                SELECT sale_date, mass * unit_price FROM sales_coconut WHERE sale_date BETWEEN :from AND :to
                UNION ALL
                SELECT sale_date, mass * unit_price FROM sales_banana WHERE sale_date BETWEEN :from AND :to
                """);
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }
}
