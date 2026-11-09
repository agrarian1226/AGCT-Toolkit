package org.uaf.agct.model;

/**
 * Every extractable GFF column. The declaration order here is the
 * canonical default TSV column order used by GeneInfoExtractor when no
 * custom order is supplied.
 */
public enum Column {

    GENE_ID          ("Gene ID",        "gene_id",         true ),
    TRANSCRIPT_ID    ("Transcript ID",  "transcript_id",   true ),
    CHROM            ("Chromosome",     "chrom",           false),
    GENE_START       ("Gene start",     "gene_start",      false),
    GENE_END         ("Gene end",       "gene_end",        false),
    STRAND           ("Strand",         "strand",          false),
    EXON_COUNT       ("Exon count",     "exon_count",      false),
    INTRON_COUNT     ("Intron count",   "intron_count",    false),
    GENE_LENGTH      ("Gene length",    "gene_length",     false),
    GENE_NAME        ("Gene name",      "gene_name",       false),
    DESCRIPTION      ("Description",    "description",     false),
    BIOTYPE          ("Biotype",        "biotype",         false),
    NUM_TRANSCRIPTS  ("Num transcripts","num_transcripts", false),
    SOURCE           ("Source",         "source",          false),
    SCORE            ("Score",          "score",           false);

    private final String displayName;
    private final String tsvHeader;
    private final boolean mandatory;

    Column(String displayName, String tsvHeader, boolean mandatory) {
        this.displayName = displayName;
        this.tsvHeader   = tsvHeader;
        this.mandatory   = mandatory;
    }

    public String getDisplayName() { return displayName; }
    public String getTsvHeader()   { return tsvHeader; }
    public boolean isMandatory()   { return mandatory; }

    /** Mandatory columns are always included; UI shows them locked. */
    public static boolean isOptional(Column c) { return !c.mandatory; }
}