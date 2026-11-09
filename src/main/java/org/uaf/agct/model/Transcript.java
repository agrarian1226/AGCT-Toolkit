package org.uaf.agct.model;

import java.util.ArrayList;
import java.util.List;

/** Mutable during parsing; exons are 1-based inclusive [start, end]. */
public final class Transcript {
    public String transcriptId;
    public String geneId;
    public String chrom;
    public int    start;
    public int    end;
    public String strand;
    public final List<int[]> exons = new ArrayList<>();
}