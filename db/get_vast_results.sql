
-- sort by parameter
-- THRID PARTY
-- 1. ce_rmsd
-- 2. fatcat_rigid_rmsd
-- RUPEE
-- 3. tm_q_tm_score
-- 4. tm_avg_tm_score
-- 5. tm_rmsd
-- 6. tm_q_score (vs. SSM only)
-- OTHER
-- 7. ssap_score (vs. CATHEDRAL only)

-- valid sort by parameters
-- 1, 2

CREATE OR REPLACE FUNCTION get_vast_results (p_benchmark VARCHAR, p_version VARCHAR, p_sort_by INTEGER, p_limit INTEGER)
RETURNS TABLE (
    n INTEGER, 
    db_id_1 VARCHAR,
    db_id_2 VARCHAR,
    ce_rmsd NUMERIC,
    fatcat_rigid_rmsd NUMERIC
)
AS $$
BEGIN

    RETURN QUERY
    WITH results AS
    (
        SELECT
            COUNT(*) OVER (PARTITION BY r.db_id_1) AS tot,
            CASE 
                WHEN p_sort_by = 1 THEN
                    RANK(*) OVER (PARTITION BY r.db_id_1 ORDER BY s.ce_rmsd, r.db_id_2) 
                WHEN p_sort_by = 2 THEN
                    RANK(*) OVER (PARTITION BY r.db_id_1 ORDER BY s.fatcat_rigid_rmsd, r.db_id_2) 
            END AS n,
            r.db_id_1,
            r.db_id_2,
            s.ce_rmsd,
            s.fatcat_rigid_rmsd
        FROM
            vast_result r
            INNER JOIN benchmark b
                ON b.db_id = r.db_id_1
                AND b.name = p_benchmark
            INNER JOIN alignment_scores s
                ON s.db_id_1 = r.db_id_1
                AND s.db_id_2 = r.db_id_2
                AND s.version = p_version
        WHERE
            r.version = p_version 
    ),
    valid_results As
    (
        -- at least limit number of results
        SELECT
            r.db_id_1 AS db_id
        FROM
            results r
        WHERE
            r.n = 1 AND r.tot >= p_limit
    ),
    filtered_results AS
    (
        SELECT
            r.n,
            r.db_id_1,
            r.db_id_2,
            r.ce_rmsd,
            r.fatcat_rigid_rmsd
        FROM
            results r
            INNER JOIN valid_results v
                ON v.db_id = r.db_id_1 
        WHERE
            r.n <= p_limit
    )
    SELECT
        r.n::INTEGER AS n,
        r.db_id_1,
        r.db_id_2,
        r.ce_rmsd,
        r.fatcat_rigid_rmsd
    FROM 
        filtered_results r
    ORDER BY
        r.db_id_1,
        r.n;

END;
$$LANGUAGE plpgsql;


