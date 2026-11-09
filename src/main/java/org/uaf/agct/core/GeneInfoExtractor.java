package org.uaf.agct.core;

import org.uaf.agct.model.Column;
import org.uaf.agct.model.Gene;
import org.uaf.agct.model.Transcript;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class GeneInfoExtractor {

    public record Stats(int total, int found, List<String> missing) {}

    private GeneInfoExtractor() {}

    public static Stats extract(GffParser.GffData gff, List<String> queryIds,
                                List<Column> requestedColumns, Path outputPath,
                                Consumer<String> log) throws IOException {

        List<Column> columns = normalise(requestedColumns);
        Files.createDirectories(outputPath.getParent());
        List<String> missing = new ArrayList<>();
        int found = 0;

        try (BufferedWriter w = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            StringBuilder h = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) h.append('\t');
                h.append(columns.get(i).getTsvHeader());
            }
            w.write(h.toString());
            w.write('\n');

            for (String qid : queryIds) {
                String[] row = resolveRow(gff, qid, columns);
                if (row == null) {
                    missing.add(qid);
                    StringBuilder sb = new StringBuilder("NOT_FOUND:").append(qid);
                    for (int i = 1; i < columns.size(); i++) sb.append("\tNA");
                    w.write(sb.toString());
                    w.write('\n');
                } else {
                    found++;
                    w.write(String.join("\t", row));
                    w.write('\n');
                }
            }
        }

        if (log != null) {
            log.accept(String.format("Found: %d / %d", found, queryIds.size()));
            if (!missing.isEmpty()) {
                log.accept("Missing: " + missing.size());
                missing.forEach(m -> log.accept("  - " + m));
            }
            log.accept("Wrote " + queryIds.size() + " rows to " + outputPath);
        }
        return new Stats(queryIds.size(), found, missing);
    }

    private static List<Column> normalise(List<Column> requested) {
        List<Column> result = new ArrayList<>();
        for (Column c : requested) if (c.isMandatory() && !result.contains(c))
            result.add(c);
        for (Column c : requested) if (!result.contains(c)) result.add(c);
        for (Column c : Column.values())
            if (c.isMandatory() && !result.contains(c)) result.add(c);
        return result;
    }

    private static String[] resolveRow(GffParser.GffData gff, String qid, List<Column> cols) {
        Gene gene = null;
        Transcript tx = null;

        if (gff.genes.containsKey(qid)) {
            gene = gff.genes.get(qid);
            tx = pickTranscript(qid, gene, gff);
        } else if (gff.transcripts.containsKey(qid)) {
            tx = gff.transcripts.get(qid);
            gene = gff.genes.get(tx.geneId);
        } else {
            return null;
        }

        String[] row = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            row[i] = valueFor(cols.get(i), gene, tx);
        }
        return row;
    }

    private static Transcript pickTranscript(String qid, Gene g, GffParser.GffData gff) {
        // Look for the representative transcript based on the actual Gene ID
        Transcript preferred = gff.transcripts.get(g.geneId + ".1");
        if (preferred != null) return preferred;
        
        // Fallback: just take the first available transcript
        List<String> tids = new ArrayList<>(g.transcripts);
        tids.sort(String::compareTo);
        for (String t : tids) {
            Transcript tx = gff.transcripts.get(t);
            if (tx != null) return tx;
        }
        return null;
    }

    private static String valueFor(Column c, Gene g, Transcript t) {
        switch (c) {
            case GENE_ID:         return g != null ? g.geneId : "NA";
            case TRANSCRIPT_ID:   return t != null ? t.transcriptId : "NA";
            case CHROM:           return g != null ? g.chrom : "NA";
            case GENE_START:      return g != null ? String.valueOf(g.start) : "NA";
            case GENE_END:        return g != null ? String.valueOf(g.end)   : "NA";
            case STRAND:          return g != null ? g.strand : "NA";
            case EXON_COUNT:      return t != null ? String.valueOf(t.exons.size()) : "0";
            case INTRON_COUNT:    return t != null ? String.valueOf(Math.max(0, t.exons.size() - 1)) : "0";
            case GENE_LENGTH:     return g != null ? String.valueOf(g.end - g.start + 1) : "NA";
            case GENE_NAME:       return g != null ? g.name        : "";
            case DESCRIPTION:     return g != null ? g.description : "";
            case BIOTYPE:         return g != null ? g.biotype     : "";
            case NUM_TRANSCRIPTS: return g != null ? String.valueOf(g.transcripts.size()) : "0";
            case SOURCE:          return g != null ? g.source      : "";
            case SCORE:           return g != null ? g.score       : ".";
            default:              return "";
        }
    }
}