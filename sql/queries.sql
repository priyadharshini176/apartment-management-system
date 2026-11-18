-- Report and analysis queries (SQLite)

-- 1. Pending payments with resident details
SELECT r.name, a.apartment_no, p.month, p.amount
FROM maintenance_payment p
JOIN apartment a ON p.apartment_id = a.apartment_id
JOIN resident r  ON r.apartment_id = a.apartment_id
WHERE p.status = 'Pending'
ORDER BY p.month, a.apartment_no;

-- 2. Total pending amount per apartment
SELECT a.apartment_no, SUM(p.amount) AS total_pending
FROM maintenance_payment p
JOIN apartment a ON p.apartment_id = a.apartment_id
WHERE p.status = 'Pending'
GROUP BY a.apartment_no
ORDER BY total_pending DESC;

-- 3. Monthly collection summary
SELECT month,
       SUM(CASE WHEN status = 'Paid'    THEN amount ELSE 0 END) AS collected,
       SUM(CASE WHEN status = 'Pending' THEN amount ELSE 0 END) AS pending
FROM maintenance_payment
GROUP BY month
ORDER BY month;

-- 4. Category-wise complaints
SELECT category, COUNT(*) AS total
FROM complaint
GROUP BY category
ORDER BY total DESC;

-- 5. Complaints by status
SELECT status, COUNT(*) AS total
FROM complaint
GROUP BY status;

-- 6. Average resolution time (days) per category
SELECT category,
       ROUND(AVG(julianday(resolved_date) - julianday(complaint_date)), 1) AS avg_days
FROM complaint
WHERE status = 'Resolved'
GROUP BY category;

-- 7. Complaints per block and category (finds problem areas)
SELECT a.block_no, c.category, COUNT(*) AS total
FROM complaint c
JOIN resident r  ON c.resident_id = r.resident_id
JOIN apartment a ON r.apartment_id = a.apartment_id
GROUP BY a.block_no, c.category
ORDER BY total DESC;

-- 8. Unresolved complaints (Open / In Progress)
SELECT c.complaint_id, r.name, c.category, c.complaint_date, c.status
FROM complaint c
JOIN resident r ON c.resident_id = r.resident_id
WHERE c.status != 'Resolved'
ORDER BY c.complaint_date;
