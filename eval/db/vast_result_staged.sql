
CREATE TABLE vast_result_staged
(
    version VARCHAR NOT NULL,
    search_type VARCHAR NOT NULL,
    n INTEGER NOT NULL,
    db_id_1 VARCHAR NOT NULL,
    db_id_2 VARCHAR NOT NULL,
    vast_score NUMERIC NOT NULL,
    vast_rmsd NUMERIC NOT NULL
); 

CREATE UNIQUE INDEX idx_vast_result_staged_unique ON vast_result_staged (version, search_type, n, db_id_1);
