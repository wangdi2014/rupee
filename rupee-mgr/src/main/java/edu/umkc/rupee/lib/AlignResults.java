package edu.umkc.rupee.lib;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;
import org.biojava.nbio.structure.io.PDBFileReader;
import org.postgresql.ds.PGSimpleDataSource;

public class AlignResults
{
    public static void alignMtmResults() {

        try {

            PGSimpleDataSource ds = Db.getDataSource();

            Connection conn = ds.getConnection();
            conn.setAutoCommit(false);
            
            PreparedStatement stmt = conn.prepareCall(
                    "SELECT * FROM mtm_result WHERE version = 'dom_v08_03_2018' AND db_id_2 LIKE 'd%' AND n <= ? ORDER BY n;");
            
            stmt.setInt(1, 5);

            ResultSet rs = stmt.executeQuery();

            PreparedStatement updt = conn.prepareStatement(
                    "UPDATE mtm_result SET ce_rmsd = ?, ce_tm_score = ? " +
                    "WHERE version = 'dom_v08_03_2018' AND db_id_1 = ? AND db_id_2 = ?;");

            PDBFileReader reader = new PDBFileReader();
            reader.setFetchBehavior(FetchBehavior.LOCAL_ONLY);

            while (rs.next()) {

                String dbId1 = rs.getString("db_id_1").toLowerCase();
                String dbId2 = rs.getString("db_id_2").toLowerCase();
                
                FileInputStream queryFile = new FileInputStream(DbTypeCriteria.SCOP.getImportPath() + dbId1 + ".pdb.gz");
                GZIPInputStream queryFileGz = new GZIPInputStream(queryFile);
                FileInputStream targetFile = new FileInputStream(DbTypeCriteria.SCOP.getImportPath() + dbId2 + ".pdb.gz");
                GZIPInputStream targetFileGz = new GZIPInputStream(targetFile);

                Structure queryStructure = reader.getStructure(queryFileGz);
                Structure targetStructure = reader.getStructure(targetFileGz);
                
                AlignRecord ce = Aligning.align(queryStructure, targetStructure, AlignCriteria.CE);

                updt.setDouble(1,ce.afps.getTotalRmsdOpt());
                updt.setDouble(2,ce.afps.getTMScore());
                updt.setString(3,dbId1);
                updt.setString(4,dbId2);

                updt.execute();
            }

            conn.commit();
            
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } catch (StructureException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } 
    }
/*
    public static void batchAlignCathResults() {

        try {

            PGSimpleDataSource ds = Db.getDataSource();

            Connection conn = ds.getConnection();
            conn.setAutoCommit(false);
            
            PreparedStatement stmt = conn.prepareCall(
                    "WITH results AS (SELECT ROW_NUMBER() OVER (ORDER BY ssap_score DESC) AS n, cath_id_1, cath_id_2 FROM cath_result) " + 
                    "SELECT * FROM results WHERE n <= ? ORDER BY n;");
            
            stmt.setInt(1, 50);

            ResultSet rs = stmt.executeQuery();

            PreparedStatement updt = conn.prepareStatement(
                    "UPDATE cath_result SET cecp_rmsd = ?, cecp_tm_score = ?, fatcat_rmsd = ?, fatcat_tm_score = ? " +
                    "WHERE cath_id_1 = ? AND cath_id_2 = ?;");

            PDBFileReader reader = new PDBFileReader();
            reader.setFetchBehavior(FetchBehavior.LOCAL_ONLY);

            while (rs.next()) {

                String cathId1 = rs.getString("cath_id_1");
                String cathId2 = rs.getString("cath_id_2");
                
                Structure queryStructure = reader.getStructure(Constants.PDB_PATH + cathId1 + ".pdb");
                Structure structure = reader.getStructure(Constants.PDB_PATH + cathId2 + ".pdb");
                
                AlignRecord cecp = Aligning.align(queryStructure, structure, AlignCriteria.CECP);
                AlignRecord fatcat = Aligning.align(queryStructure, structure, AlignCriteria.FATCAT_FLEXIBLE);

                updt.setDouble(1,cecp.afps.getTotalRmsdOpt());
                updt.setDouble(2,cecp.afps.getTMScore());
                updt.setDouble(3,fatcat.afps.getTotalRmsdOpt());
                updt.setDouble(4,fatcat.afps.getTMScore());
                updt.setString(5,cathId1);
                updt.setString(6,cathId2);

                updt.execute();
            }

            conn.commit();
            
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } catch (StructureException e) {
            Logger.getLogger(Aligning.class.getName()).log(Level.SEVERE, null, e);
        } 
    }
    */
}