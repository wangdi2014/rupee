package edu.umkc.rupee.lib;

import org.postgresql.util.PGobject;

public class Residue extends PGobject {

    private String pdbId;
    private String chainId;
    private int atomNumber;
    private int residueNumber;
    private String insertCode;
    private String residueCode;
    private String ssa;
    private String sse;
    private double phi;
    private double psi;
    private int descriptor;
    private int runFactor;
    private int gram;
    private boolean breakBefore;
    private boolean breakAfter;

    public String getPdbId() {
        return pdbId;
    }

    public void setPdbId(String pdbId) {
        this.pdbId = pdbId;
    }
    
    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public int getAtomNumber() {
        return atomNumber;
    }

    public void setAtomNumber(int atomNumber) {
        this.atomNumber = atomNumber;
    }

    public int getResidueNumber() {
        return residueNumber;
    }

    public void setResidueNumber(int residueNumber) {
        this.residueNumber = residueNumber;
    }

    public String getInsertCode() {
        return insertCode;
    }

    public void setInsertCode(String insertCode) {
        this.insertCode = insertCode;
    }
    
    public String getResidueCode() {
        return residueCode;
    }

    public void setResidueCode(String residueCode) {
        this.residueCode = residueCode;
    }

    public String getSSA() {
        return ssa;
    }

    public void setSSA(String ssa) {
        this.ssa = ssa;
    }

    public String getSSE() {
        return sse;
    }

    public void setSSE(String sse) {
        this.sse = sse;
    }

    public double getPhi() {
        return phi;
    }

    public void setPhi(double phi) {
        this.phi = phi;
    }

    public double getPsi() {
        return psi;
    }

    public void setPsi(double psi) {
        this.psi = psi;
    }

    public int getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(int region) {
        this.descriptor = region;
    }

    public int getRunFactor() {
        return runFactor;
    }

    public void setRunFactor(int runFactor) {
        this.runFactor = runFactor;
    }

    public int getGram() {
        return gram;
    }

    public void setGram(int gram) {
        this.gram = gram;
    }

    public boolean getBreakBefore() {
        return breakBefore;
    }

    public void setBreakBefore(boolean breakBefore) {
        this.breakBefore = breakBefore;
    }

    public boolean getBreakAfter() {
        return breakAfter;
    }

    public void setBreakAfter(boolean breakAfter) {
        this.breakAfter = breakAfter;
    }

    public boolean isHelix() {
        return sse.equals("Helix");
    }
    
    public boolean isStrand() {
        return sse.equals("Strand");
    }
    
    public boolean isTurn() {
        return sse.equals("Turn");
    }
   
    public boolean isBridge() {
        return sse.equals("Bridge");
    }

    @Override
    public String getValue() {
        String row = "(" 
            + pdbId + "," + chainId + "," + atomNumber + "," 
            + residueNumber + "," + (insertCode == null || insertCode.equals("null") ? "" : insertCode) + "," + residueCode + "," 
            + ssa + "," + sse + "," + phi + "," + psi + "," 
            + descriptor + "," + runFactor + "," + gram + "," 
            + (breakBefore ? 1 : 0) + "," + (breakAfter ? 1 : 0)
            + ")";
        return row;
    }
}

