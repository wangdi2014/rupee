
TRUNCATE scop_domain;

COPY scop_domain (scop_id, pdb_id, sunid, cl, cf, sf, fa) FROM '/home/ayoub/git/rupee/scop/domains.txt' WITH (DELIMITER ' ');

-- aggregate some values for convenience
UPDATE scop_domain
SET 
    cl_cf = cl || '.' || cf,
    cl_cf_sf = cl || '.' || cf || '.' || sf,
    cl_cf_sf_fa = cl || '.' || cf || '.' || sf || '.' || fa;






