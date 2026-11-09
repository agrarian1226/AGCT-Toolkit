package org.uaf.agct.core;

import org.uaf.agct.model.FastaRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FastaParser {

    private static final Pattern TRANSCRIPT_SUFFIX = Pattern.compile("\\.\\d+$");
    private static final Pattern GENE_ID_PATTERN   =
            Pattern.compile("AT[0-9CM]G\\d+|Solyc\\d+g\\d+", Pattern.CASE_INSENSITIVE);

    public static final class FastaData {
        public final Map<String, String> byGene  = new LinkedHashMap<>();
        public final Map<String, String> byTx    = new LinkedHashMap<>();
        public final Set<String>         duplicateGenes = new LinkedHashSet<>();
    }

    private FastaParser() {}

    public static FastaData parse(Path fastaPath) throws IOException {
        FastaData data = new FastaData();

        String currentTx = null;
        String currentGene = null;
        StringBuilder chunk = new StringBuilder();

        try (BufferedReader r = openUtf8StrippingBom(fastaPath)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;

                if (line.charAt(0) == '>') {
                    flush(data, currentTx, currentGene, chunk);
                    String[] ids = extractIdsFromHeader(line);
                    currentTx   = ids[0];
                    currentGene = ids[1];
                    chunk.setLength(0);
                } else {
                    chunk.append(line);
                }
            }
            flush(data, currentTx, currentGene, chunk);
        }
        return data;
    }

    /** @return [transcriptId, geneId] */
    static String[] extractIdsFromHeader(String headerLine) {
        String header = headerLine.substring(1).strip();
        String first  = header.isEmpty() ? "" : header.split("\\s+")[0];
        if (first.endsWith("|")) first = first.substring(0, first.length() - 1);

        String tx  = first;
        String gid = TRANSCRIPT_SUFFIX.matcher(first).replaceFirst("");

        if (!GENE_ID_PATTERN.matcher(gid).matches()) {
            Matcher m = GENE_ID_PATTERN.matcher(header);
            if (m.find()) gid = m.group();
        }
        return new String[]{tx, gid};
    }

    private static void flush(FastaData d, String tx, String gene,
                              StringBuilder seq) {
        if (tx == null || tx.isEmpty()) return;
        String s = seq.toString();
        d.byTx.put(tx, s);
        
        if (gene != null && !gene.isEmpty()) {
            if (d.byGene.containsKey(gene)) {
                d.duplicateGenes.add(gene);
            } else {
                d.byGene.put(gene, s);
                // Also map the base ID without version suffix (e.g., Solyc00g500001)
                String baseGid = gene.replaceAll("\\.\\d+$", "");
                if (!baseGid.equals(gene) && !d.byGene.containsKey(baseGid)) {
                    d.byGene.put(baseGid, s);
                }
            }
        }
    }

    private static BufferedReader openUtf8StrippingBom(Path p) throws IOException {
        BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8);
        r.mark(1);
        int c = r.read();
        if (c != 0xFEFF) r.reset();
        return r;
    }
}