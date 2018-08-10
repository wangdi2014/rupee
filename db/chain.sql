
CREATE TABLE chain
(
    chain_sid SERIAL,
    chain_id VARCHAR NOT NULL,
    pdb_id VARCHAR NOT NULL,
    residue_count INTEGER NOT NULL
);

CREATE UNIQUE INDEX idx_chain_unique ON chain (chain_sid);
