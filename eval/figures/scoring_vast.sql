
DO $$

    DECLARE p_benchmark VARCHAR := 'casp_vast_d199';  
    DECLARE p_version VARCHAR := 'casp_chain_v08_28_2020'; 
    DECLARE p_search_type VARCHAR := 'full_length'; 
    DECLARE p_sort_by INTEGER := 2; -- tm_avg_tm_score
    DECLARE p_limit INTEGER := 100; 

BEGIN

    DROP TABLE IF EXISTS figure_table;
   
    CREATE TABLE figure_table AS 
        WITH rupee_all_aligned AS
        (
            SELECT * FROM get_rupee_results(p_benchmark, p_version, 'all_aligned', p_search_type, p_sort_by, p_limit)
        ),
        rupee_top_aligned AS
        (
            SELECT * FROM get_rupee_results(p_benchmark, p_version, 'top_aligned', p_search_type, p_sort_by, p_limit)
        ),
        rupee_fast AS
        (
            SELECT * FROM get_rupee_results(p_benchmark, p_version, 'fast', p_search_type, p_sort_by, p_limit)
        ),
        vast AS
        (   
            SELECT * FROM get_vast_results(p_benchmark, p_version, p_search_type, p_limit) 
        ),
        ranked AS
        (
            SELECT 
                n,
                'All' AS app,
                db_id_1,
                tm_avg_tm_score AS score
            FROM
                rupee_all_aligned
            UNION ALL
            SELECT 
                n,
                'Top' AS app,
                db_id_1,
                tm_avg_tm_score AS score
            FROM
                rupee_top_aligned
            UNION ALL
            SELECT 
                n,
                'Fast' AS app,
                db_id_1,
                tm_avg_tm_score AS score
            FROM
                rupee_fast
            UNION ALL
            SELECT 
                n,
                'VAST' AS app,
                db_id_1,
                tm_avg_tm_score AS score
            FROM
                vast
        ),
        accumulated AS
        (
            SELECT 
                n,
                app,
                AVG(score) OVER (PARTITION BY app, db_id_1 ORDER BY n ROWS UNBOUNDED PRECEDING) AS cume_score
            FROM
                ranked
        ),
        averaged AS 
        (
            SELECT
                n,
                app,
                AVG(cume_score) AS avg_cume_score
            FROM
                accumulated 
            GROUP BY
                n,
                app
        )
        SELECT
            *
        FROM
            averaged
        ORDER BY
            app,
            n;

END $$;
    
SELECT * FROM figure_table;

