package edu.umkc.rupee.lib;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;
import org.biojava.nbio.structure.io.PDBFileReader;
import org.postgresql.ds.PGSimpleDataSource;

import edu.umkc.rupee.defs.AlignCriteria;
import edu.umkc.rupee.defs.DbTypeCriteria;
import edu.umkc.rupee.tm.TMAlign;

public class AlignResults
{
    private static AtomicInteger counter;

    static {
        counter = new AtomicInteger();
    }

    public static void alignRupeeResults(String benchmark, String version, String sort, DbTypeCriteria dbType, int maxN) {

        List<String> dbIds = Benchmarks.get(benchmark);
            
        String command = "SELECT db_id_2 FROM rupee_result WHERE version = ? AND sort = ? AND db_id_1 = ? AND n <= ? ORDER BY n;";

        counter.set(0);
        dbIds.parallelStream().forEach(dbId -> alignResults(command, version, sort, dbType, dbId, maxN));
    }

    public static void alignMtmDomResults(String benchmark, String version, DbTypeCriteria dbType, int maxN) {

        List<String> dbIds = Benchmarks.get(benchmark);
            
        String command = "SELECT db_id_2 FROM mtm_dom_result_matched WHERE version = ? AND db_id_1 = ? AND n <= ? ORDER BY n;";

        counter.set(0);
        dbIds.parallelStream().forEach(dbId -> alignResults(command, version, "", dbType, dbId, maxN));
    }

    public static void alignCathedralResults(String benchmark, String version, DbTypeCriteria dbType, int maxN) {

        List<String> dbIds = Benchmarks.get(benchmark);

        String command = "SELECT db_id_2 FROM cathedral_result WHERE version = ? AND db_id_1 = ? AND n <= ? ORDER BY n;";

        counter.set(0);
        dbIds.parallelStream().forEach(dbId -> alignResults(command, version, "", dbType, dbId, maxN));
    }
    
    public static void alignSsmResults(String benchmark, String version, DbTypeCriteria dbType, int maxN) {

        List<String> dbIds = Benchmarks.get(benchmark);

        String command = "SELECT db_id_2 FROM ssm_result WHERE version = ? AND db_id_1 = ? AND n <= ? ORDER BY n;";

        counter.set(0);
        dbIds.parallelStream().forEach(dbId -> alignResults(command, version, "", dbType, dbId, maxN));
    }

    private static void alignResults(String command, String version, String sort, DbTypeCriteria dbType, String dbId, int maxN) {

        try {
            
            Map<String, AlignmentScores> map = Db.getAlignmentScores(version, dbId);
            List<AlignmentScores> scores = new ArrayList<>();

            PGSimpleDataSource ds = Db.getDataSource();

            Connection conn = ds.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareCall(command);

            if (sort.isEmpty()) {

                stmt.setString(1, version);
                stmt.setString(2, dbId);
                stmt.setInt(3, maxN);
            }
            else {

                stmt.setString(1, version);
                stmt.setString(2, sort);
                stmt.setString(3, dbId);
                stmt.setInt(4, maxN);
            }

            ResultSet rs = stmt.executeQuery();

            PDBFileReader reader = new PDBFileReader();
            reader.setFetchBehavior(FetchBehavior.LOCAL_ONLY);

            FileInputStream queryFile = new FileInputStream(dbType.getImportPath() + dbId + ".pdb.gz");
            GZIPInputStream queryFileGz = new GZIPInputStream(queryFile);
            Structure queryStructure = reader.getStructure(queryFileGz);

            while (rs.next()) {

                String dbId2 = rs.getString("db_id_2");
            
                if (!map.containsKey(dbId2)) {

                    FileInputStream targetFile = new FileInputStream(dbType.getImportPath() + dbId2 + ".pdb.gz");
                    GZIPInputStream targetFileGz = new GZIPInputStream(targetFile);
                    Structure targetStructure = reader.getStructure(targetFileGz);
                    
                    // gather alignment scores 
                    AlignmentScores score = new AlignmentScores();
                    score.setVersion(version);
                    score.setDbId1(dbId);
                    score.setDbId2(dbId2);

                    // perform biojava alignments
                    AlignRecord ce = Aligning.align(queryStructure, targetStructure, AlignCriteria.CE);
                    AlignRecord fatcat = Aligning.align(queryStructure, targetStructure, AlignCriteria.FATCAT_FLEXIBLE);

                    score.setCeRmsd(ce.afps.getTotalRmsdOpt());
                    score.setCeTmScore(ce.afps.getTMScore());
                    score.setFatCatRmsd(fatcat.afps.getTotalRmsdOpt());
                    score.setFatCatTmScore(fatcat.afps.getTMScore());

                    // perform tm-align alignment
                    try {
                        TMAlign tm = new TMAlign();
                        TMAlign.Results results = tm.align(queryStructure, targetStructure);

                        score.setTmQRmsd(results.getRmsd());
                        score.setTmQTmScore(results.getTmScoreQ());
                        score.setTmAvgRmsd(results.getRmsd());
                        score.setTmAvgTmScore(results.getTmScoreAvg());
                    }
                    catch (RuntimeException e) {
                        System.out.println("error comparing: " + dbId + ", " + dbId2);
                    }

                    scores.add(score);
                }
            }

            rs.close();
            stmt.close();
            conn.close();

            if (scores.size() > 0) {
                Db.saveAlignmentScores(version, dbId, scores);
            }

            int count = counter.incrementAndGet();

            System.out.println("Processed Count: " + count);

        } catch (SQLException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } catch (StructureException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static void fillInResults(String version, DbTypeCriteria dbType) {

        try {
            
            PGSimpleDataSource ds = Db.getDataSource();

            Connection conn = ds.getConnection();
            conn.setAutoCommit(false);

            String command = "SELECT db_id_1, db_id_2 FROM alignment_scores WHERE version = ? AND tm_avg_tm_score = -1;";
            PreparedStatement stmt = conn.prepareCall(command);
            stmt.setString(1, version);

            ResultSet rs = stmt.executeQuery();

            PDBFileReader reader = new PDBFileReader();
            reader.setFetchBehavior(FetchBehavior.LOCAL_ONLY);

            while (rs.next()) {

                String dbId1 = rs.getString("db_id_1");
                String dbId2 = rs.getString("db_id_2");
            
                FileInputStream queryFile = new FileInputStream(dbType.getImportPath() + dbId1 + ".pdb.gz");
                GZIPInputStream queryFileGz = new GZIPInputStream(queryFile);
                Structure queryStructure = reader.getStructure(queryFileGz);

                FileInputStream targetFile = new FileInputStream(dbType.getImportPath() + dbId2 + ".pdb.gz");
                GZIPInputStream targetFileGz = new GZIPInputStream(targetFile);
                Structure targetStructure = reader.getStructure(targetFileGz);
                    
                // perform tm-align alignment
                try {
                    TMAlign tm = new TMAlign();
                    TMAlign.Results results = tm.align(queryStructure, targetStructure);

                    saveAlignmentScores(version, dbId1, dbId2, results);
                }
                catch (RuntimeException e) {
                    System.out.println("error comparing: " + dbId1 + ", " + dbId2);
                }
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    public static void saveAlignmentScores(String version, String dbId1, String dbId2, TMAlign.Results results) {

        try {

            PGSimpleDataSource ds = Db.getDataSource();
            Connection conn = ds.getConnection();
            conn.setAutoCommit(true);

            PreparedStatement updt = conn.prepareStatement("UPDATE alignment_scores SET tm_avg_rmsd = ?, tm_avg_tm_score = ? WHERE version = ? AND db_id_1 = ? AND db_id_2 = ?;");

            updt.setDouble(1, results.getRmsd());
            updt.setDouble(2, results.getTmScoreAvg());
            updt.setString(3, version);
            updt.setString(4, dbId1);
            updt.setString(5, dbId2);

            updt.execute();
            updt.close();
        
        } catch (SQLException e) {
            Logger.getLogger(Db.class.getName()).log(Level.WARNING, null, e);
        }
    }
}
