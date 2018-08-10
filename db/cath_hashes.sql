
CREATE TABLE cath_hashes
(
    cath_id VARCHAR NOT NULL,
    min_hashes INTEGER ARRAY NOT NULL,
    band_hashes INTEGER ARRAY NOT NULL
);

CREATE UNIQUE INDEX idx_cath_hashes_unique ON cath_hashes (cath_id);

