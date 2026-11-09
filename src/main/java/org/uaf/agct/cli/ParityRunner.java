package org.uaf.agct.cli;

import org.uaf.agct.core.*;
import org.uaf.agct.model.Column;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Temporary CLI used only to verify byte-for-byte parity with the Python
 * reference scripts. Not shipped in the final GUI app.
 *
 * Usage:
 *   java -cp target/classes org.uaf.agct.cli.ParityRunner info  <gff> <genes> <out.tsv>
 *   java -cp target/classes org.uaf.agct.cli.ParityRunner seq   <fasta> <genes> <out.tsv>
 */
public final class ParityRunner {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: ParityRunner <info|seq> <input> <genes> <out.tsv>");
            System.exit(2);
        }
        String mode   = args[0];
        Path   input  = Path.of(args[1]);
        Path   genes  = Path.of(args[2]);
        Path   out    = Path.of(args[3]);
        List<String> ids = readList(genes);

        switch (mode) {
            case "info" -> {
                GffParser.GffData gff = GffParser.parse(input);
                // Match the 9-column default output from the Python script
                List<Column> cols = new ArrayList<>(Arrays.asList(
                        Column.GENE_ID, Column.TRANSCRIPT_ID, Column.CHROM,
                        Column.GENE_START, Column.GENE_END, Column.STRAND,
                        Column.EXON_COUNT, Column.INTRON_COUNT, Column.GENE_LENGTH));
                GeneInfoExtractor.extract(gff, ids, cols, out, System.err::println);
            }
            case "seq" -> {
                FastaParser.FastaData fa = FastaParser.parse(input);
                SequenceExtractor.extract(fa, ids, out, null, System.err::println);
            }
            default -> {
                System.err.println("Unknown mode: " + mode);
                System.exit(2);
            }
        }
    }

    private static List<String> readList(Path p) throws Exception {
        List<String> ids = new ArrayList<>();
        for (String line : Files.readAllLines(p)) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            ids.add(line.split("\\s+")[0]);
        }
        return ids;
    }
}