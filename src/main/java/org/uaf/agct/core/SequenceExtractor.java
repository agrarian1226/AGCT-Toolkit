package org.uaf.agct.core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Type-agnostic FASTA sequence extractor. Same logic for CDS, protein,
 * cDNA, promoter or any custom FASTA.
 */
public final class SequenceExtractor {

    public record Stats(int total, int found, List<String> missing) {}

    private static final Pattern TRANSCRIPT_SUFFIX = Pattern.compile("\\.\\d+$");

    private SequenceExtractor() {}

    public static Stats extract(FastaParser.FastaData fasta,
                                List<String> queryIds,
                                Path tsvOut,
                                Path fastaOutOrNull,
                                Consumer<String> log) throws IOException {

        Files.createDirectories(tsvOut.getParent());
        if (fastaOutOrNull != null) Files.createDirectories(fastaOutOrNull.getParent());

        List<String> missing = new ArrayList<>();
        int found = 0;

        try (BufferedWriter wtsv  = Files.newBufferedWriter(tsvOut,
                StandardCharsets.UTF_8);
             BufferedWriter wfa = fastaOutOrNull != null
                     ? Files.newBufferedWriter(fastaOutOrNull, StandardCharsets.UTF_8)
                     : null) {

            for (String qid : queryIds) {
                String seq = lookup(fasta, qid);
                if (seq == null) {
                    missing.add(qid);
                    wtsv.write(qid + "\tNA\n");
                    continue;
                }
                found++;
                wtsv.write(qid + '\t' + seq + '\n');
                if (wfa != null) {
                    wfa.write('>' + qid + '\n');
                    wfa.write(seq + '\n');
                }
            }
        }

        if (log != null) {
            log.accept(String.format("Found: %d / %d", found, queryIds.size()));
            if (!missing.isEmpty()) {
                log.accept("Missing: " + missing.size());
                missing.forEach(m -> log.accept("  - " + m));
            }
            log.accept("Wrote " + queryIds.size() + " rows to " + tsvOut);
        }
        return new Stats(queryIds.size(), found, missing);
    }

    private static String lookup(FastaParser.FastaData f, String qid) {
        if (f.byTx.containsKey(qid))   return f.byTx.get(qid);
        if (f.byGene.containsKey(qid)) return f.byGene.get(qid);
        String guess = TRANSCRIPT_SUFFIX.matcher(qid).replaceFirst("");
        return f.byGene.get(guess);
    }
}