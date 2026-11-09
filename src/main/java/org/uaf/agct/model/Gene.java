package org.uaf.agct.model;

import java.util.ArrayList;
import java.util.List;

/** Mutable during parsing; treated as read-only afterwards. */
public final class Gene {
    public String geneId;
    public String chrom;
    public int    start;
    public int    end;
    public String strand;
    public String name        = "";
    public String description = "";
    public String biotype     = "";
    public String source      = "";
    public String score       = "";
    public final List<String> transcripts = new ArrayList<>();
}