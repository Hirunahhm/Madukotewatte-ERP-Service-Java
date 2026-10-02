package com.madukotawatte.erp.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Cross-table read queries over the 6 sales_* tables (sales_latex, sales_rubber_solid, sales_manioc,
 * sales_coconut, sales_banana, sales_general). These tables have no shared entity/superclass, so one
 * normalised native UNION ALL ("BASE") is wrapped by each query.
 *
 * BASE columns: sale_id, category, load_id, sale_date, amount, paid, account_code, sale_type, buyer_name,
 * due_date, payment_date. Latex has no sale_date column, so its creation date is used.
 */
@Repository
public class SalesLedgerRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String BASE = """
            SELECT sale_id, 'latex' AS category, load_id, CAST(created_at AS date) AS sale_date,
                   total_amount AS amount, COALESCE(is_payment_received, FALSE) AS paid, account_code,
                   'latex' AS sale_type, buyer_name, due_date, payment_date
            FROM sales_latex
            UNION ALL
            SELECT sale_id, 'rubber-solid', load_id, sale_date, (mass * unit_price), is_paid, account_code,
                   'rubber-solid', buyer_name, due_date, payment_date
            FROM sales_rubber_solid
            UNION ALL
            SELECT sale_id, 'manioc', load_id, sale_date, (mass * unit_price), is_paid, account_code,
                   'manioc', buyer_name, due_date, payment_date
            FROM sales_manioc
            UNION ALL
            SELECT sale_id, 'coconut', load_id, sale_date, (mass * unit_price), is_paid, account_code,
                   'coconut', buyer_name, due_date, payment_date
            FROM sales_coconut
            UNION ALL
            SELECT sale_id, 'banana', load_id, sale_date, (mass * unit_price), is_paid, account_code,
                   'banana', buyer_name, due_date, payment_date
            FROM sales_banana
            UNION ALL
            SELECT sale_id, product, CAST(NULL AS varchar), sale_date, total_amount, is_paid, account_code,
                   'general', buyer_name, due_date, payment_date
            FROM sales_general
            """;

    private static final String FILTER = """
            WHERE (CAST(:category AS varchar) IS NULL OR lower(category) = lower(CAST(:category AS varchar))
                   OR (lower(CAST(:category AS varchar)) = 'general' AND sale_type = 'general'))
              AND (CAST(:status AS varchar) IS NULL
                   OR (CASE WHEN paid THEN 'paid' ELSE 'pending' END) = CAST(:status AS varchar))
              AND (CAST(:from AS date) IS NULL OR sale_date >= CAST(:from AS date))
              AND (CAST(:to AS date) IS NULL OR sale_date <= CAST(:to AS date))
            """;

    /**
     * Rows: [sale_id, category, load_id, sale_date, amount, status, account_code, sale_type, buyer_name,
     * due_date, payment_date].
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findLedgerRows(String category, String status, LocalDate from, LocalDate to, int limit, int offset) {
        Query query = entityManager.createNativeQuery(
                "SELECT sale_id, category, load_id, sale_date, amount, "
                        + "CASE WHEN paid THEN 'paid' ELSE 'pending' END AS status, account_code, sale_type, "
                        + "buyer_name, due_date, payment_date FROM (" + BASE + ") t " + FILTER
                        + " ORDER BY sale_date DESC, sale_id LIMIT :limit OFFSET :offset");
        bindFilters(query, category, status, from, to);
        query.setParameter("limit", limit);
        query.setParameter("offset", offset);
        return query.getResultList();
    }

    public long countLedgerRows(String category, String status, LocalDate from, LocalDate to) {
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + BASE + ") t " + FILTER);
        bindFilters(query, category, status, from, to);
        return ((Number) query.getSingleResult()).longValue();
    }

    private void bindFilters(Query query, String category, String status, LocalDate from, LocalDate to) {
        query.setParameter("category", category);
        query.setParameter("status", status);
        query.setParameter("from", from);
        query.setParameter("to", to);
    }

    /** Category totals ([category, total]), optionally scoped to [from,to]. General sales use their product name. */
    @SuppressWarnings("unchecked")
    public List<Object[]> sumByCategory(LocalDate from, LocalDate to) {
        Query query = entityManager.createNativeQuery(
                "SELECT category, COALESCE(SUM(amount), 0) FROM (" + BASE + ") t "
                        + "WHERE (CAST(:from AS date) IS NULL OR sale_date >= CAST(:from AS date)) "
                        + "AND (CAST(:to AS date) IS NULL OR sale_date <= CAST(:to AS date)) "
                        + "GROUP BY category ORDER BY category");
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }

    /** Returns a single row: [receivedTotal, pendingTotal], optionally scoped to [from,to]. */
    public Object[] sumByPaidStatus(LocalDate from, LocalDate to) {
        Query query = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(CASE WHEN paid THEN amount ELSE 0 END), 0) AS received, "
                        + "COALESCE(SUM(CASE WHEN NOT paid THEN amount ELSE 0 END), 0) AS pending FROM (" + BASE + ") t "
                        + "WHERE (CAST(:from AS date) IS NULL OR sale_date >= CAST(:from AS date)) "
                        + "AND (CAST(:to AS date) IS NULL OR sale_date <= CAST(:to AS date))");
        query.setParameter("from", from);
        query.setParameter("to", to);
        return (Object[]) query.getSingleResult();
    }

    /** Returns [sale_date, amount] rows across all sales tables within the window, for in-Java bucketing. */
    @SuppressWarnings("unchecked")
    public List<Object[]> findAmountsInRange(LocalDate from, LocalDate to) {
        Query query = entityManager.createNativeQuery(
                "SELECT sale_date, amount FROM (" + BASE + ") t WHERE sale_date BETWEEN :from AND :to");
        query.setParameter("from", from);
        query.setParameter("to", to);
        return query.getResultList();
    }

    /**
     * Sales outstanding on {@code asOf}: sold on/before that day and (unpaid or paid after it).
     * Rows: [sale_type, sale_id, category, buyer_name, sale_date, due_date, amount].
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findReceivableRows(LocalDate asOf) {
        Query query = entityManager.createNativeQuery(
                "SELECT sale_type, sale_id, category, buyer_name, sale_date, due_date, amount FROM (" + BASE + ") t "
                        + "WHERE sale_date <= CAST(:asOf AS date) AND (NOT paid OR payment_date > CAST(:asOf AS date)) "
                        + "ORDER BY sale_date, sale_id");
        query.setParameter("asOf", asOf);
        return query.getResultList();
    }
}
