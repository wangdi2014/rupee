
TRUNCATE TABLE ssm_result;

COPY ssm_result (version, n, db_id_1, db_id_2) FROM '/home/ayoub/git/rupee/results/ssm/ssm_results.txt' WITH (DELIMITER ',');
