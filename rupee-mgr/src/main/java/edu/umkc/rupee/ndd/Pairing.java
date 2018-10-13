package edu.umkc.rupee.ndd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.postgresql.PGConnection;
import org.postgresql.ds.PGSimpleDataSource;

import edu.umkc.rupee.lib.Db;
import edu.umkc.rupee.lib.Hashes;
import edu.umkc.rupee.lib.Similarity;

public class Pairing {

    private final static double DEFAULT_SIMILARITY_THRESHOLD = 0.75;
    private final static int DEFAULT_MAX_BAND_INDEX = 20;
    private final static int MAX_CACHED_PAIRS = 500;

    private double similarityThreshold;
    private int maxBandIndex;
    
    // *********************************************************************
    // Pairing Methods 
    // *********************************************************************
    
    public Pairing () {

        this.similarityThreshold = DEFAULT_SIMILARITY_THRESHOLD;
        this.maxBandIndex = DEFAULT_MAX_BAND_INDEX;
    }
   
    public Pairing (double similarityThreshold, int maxBandIndex) {

        this.similarityThreshold = similarityThreshold;
        this.maxBandIndex = maxBandIndex;
    }

    public void pair() {

        IntStream
            .range(0, maxBandIndex)
            .boxed()
            .parallel()
            .forEach(bandIndex -> pairOnBand(bandIndex));
    }

    private void pairOnBand(int bandIndex) {
        
        try {
            
            PGSimpleDataSource ds = Db.getDataSource();
            
            Connection conn;
            PreparedStatement stmt;
            
            conn = ds.getConnection();
            conn.setAutoCommit(true);
            
            //the order by is critical - we only store pairs for which db_id_1 < db_id_2
            stmt = conn.prepareStatement("SELECT db_id, min_hashes, band_hashes FROM scop_hashes ORDER BY band_hashes[?], db_id;");
            stmt.setInt(1, bandIndex + 1); // bands are one-based in DB
            
            int lastBandHash = -1;
            List<Hashes> tile = new ArrayList<>();
            List<NddPair> pairs = new ArrayList<>();
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                  
                    Hashes hashes = new Hashes();
                    
                    hashes.setDbId(rs.getString("db_id"));
                    hashes.setMinHashes((Integer[]) rs.getArray("min_hashes").getArray());
                    hashes.setBandHashes((Integer[]) rs.getArray("band_hashes").getArray());
                    
                    if (lastBandHash != -1 && hashes.getBandHashes()[bandIndex] != lastBandHash) {
                        unwindTile(tile, bandIndex, pairs);
                    }
                    
                    if (pairs.size() >= MAX_CACHED_PAIRS) {
                        savePairs(pairs, conn);
                        pairs.clear();
                    }
                    
                    tile.add(hashes);
                    
                    lastBandHash = hashes.getBandHashes()[bandIndex];
                }
            }
            
            // now unwind the trailing pairs
            unwindTile(tile, bandIndex, pairs);
            
            if (pairs.size() > 0) {
                savePairs(pairs, conn);
            }
            
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Logger.getLogger(Pairing.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void unwindTile(List<Hashes> tile, int band, List<NddPair> pairs) {

        //unwind tile
        if(tile.size() >= 2){
            for (int i = 0; i < tile.size() - 1; i++) {
                for (int j = i + 1; j < tile.size(); j++) {
                    if (!lowerBandMatch(tile.get(i).getBandHashes(), tile.get(j).getBandHashes(), band)) {
                        double similarity = Similarity.getEstimatedSimilarity(tile.get(i).getMinHashes(), tile.get(j).getMinHashes());
                        if(similarity >= similarityThreshold){
                            NddPair pair = new NddPair();
                            pair.setDbId1(tile.get(i).getDbId());
                            pair.setDbId2(tile.get(j).getDbId());
                            pair.setSimilarity(similarity);
                            pairs.add(pair);
                        }
                    }
                }
            }
        }
        tile.clear();
    }

    private boolean lowerBandMatch(Integer[] bandHashes1, Integer[] bandHashes2, int band) {

        for (int i = 0; i < band; i++) {
            if (Objects.equals(bandHashes1[i], bandHashes2[i])) {
                return true;
            }
        }
        return false;
    }
            
    private void savePairs(List<NddPair> pairs, Connection conn) throws SQLException {
        
        PreparedStatement updt;

        ((PGConnection)conn).addDataType("scop_pair", NddPair.class);
        
        updt = conn.prepareStatement("SELECT insert_scop_pairs(?);");

        NddPair a[] = new NddPair[pairs.size()];
        pairs.toArray(a);
        updt.setArray(1, conn.createArrayOf("scop_pair", a));
        updt.execute();
        
        updt.close();
    }
}
