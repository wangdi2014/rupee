
CREATE TABLE ecod_hashes
(
    db_id VARCHAR NOT NULL,
    min_hashes INTEGER ARRAY NOT NULL,
    band_hashes INTEGER ARRAY NOT NULL,
    exact_hash BIGINT NOT NULL
);

CREATE UNIQUE INDEX idx_ecod_hashes_unique ON ecod_hashes (db_id);

