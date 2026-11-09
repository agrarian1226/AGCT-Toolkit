package org.uaf.agct.core;

import org.uaf.agct.model.Gene;
import org.uaf.agct.model.Transcript;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GffParser {

    public static final class GffData {
        public final Map<String, Gene>       genes       = new LinkedHashMap<>();
        public final Map<String, Transcript> transcripts = new LinkedHashMap<>();
    }

    private GffParser() {}

    public static GffData parse(Path gffPath) throws IOException {
        GffData data = new GffData();

        // Pass 1 — gene / mRNA / transcript features
        try (BufferedReader r = openUtf8StrippingBom(gffPath)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] f = line.split("\t", -1);
                if (f.length != 9) continue;

                String chrom = f[0], type = f[2], start = f[3], end = f[4],
                       strand = f[6], attrsStr = f[8];
                Map<String, String> a = parseAttributes(attrsStr);

                if ("gene".equals(type)) {
                    String rawId = a.get("ID");
                    if (rawId == null || rawId.isEmpty()) continue;
                    
                    String gid = cleanId(rawId); // e.g. Solyc04g075000.3
                    String alias = cleanId(a.get("Alias"));
                    
                    Gene g = new Gene();
                    g.geneId      = gid;
                    g.chrom       = chrom;
                    g.start       = Integer.parseInt(start);
                    g.end         = Integer.parseInt(end);
                    g.strand      = strand;
                    g.name        = a.getOrDefault("gene", a.getOrDefault("Name", ""));
                    g.description = a.getOrDefault("description", a.getOrDefault("Note", ""));
                    g.biotype     = a.getOrDefault("locus_biotype", "");
                    g.source      = f[1];
                    g.score       = ".".equals(f[5]) ? "." : f[5];
                    
                    data.genes.put(gid, g);
                    
                    // Map the version-stripped ID (e.g., Solyc04g075000) as well
                    String baseGid = gid.replaceAll("\\.\\d+$", "");
                    if (!baseGid.equals(gid) && !data.genes.containsKey(baseGid)) {
                        data.genes.put(baseGid, g);
                    }
                    
                    // Map the Alias if it exists
                    if (alias != null && !alias.isEmpty() && !data.genes.containsKey(alias)) {
                        data.genes.put(alias, g);
                    }

                } else if ("mRNA".equals(type) || "transcript".equals(type)) {
                    String tid    = cleanId(a.get("ID"));
                    String parent = cleanId(a.get("Parent"));
                    if (tid == null || parent == null) continue;
                    parent = parent.split(",")[0].trim();

                    Transcript t = new Transcript();
                    t.transcriptId = tid;
                    t.geneId       = parent;
                    t.chrom        = chrom;
                    t.start        = Integer.parseInt(start);
                    t.end          = Integer.parseInt(end);
                    t.strand       = strand;
                    data.transcripts.put(tid, t);

                    Gene g = data.genes.get(parent);
                    if (g != null) g.transcripts.add(tid);
                }
            }
        }

        // Pass 2 — exon features
        try (BufferedReader r = openUtf8StrippingBom(gffPath)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] f = line.split("\t", -1);
                if (f.length != 9 || !"exon".equals(f[2])) continue;

                Map<String, String> a = parseAttributes(f[8]);
                String parent = cleanId(a.get("Parent"));
                if (parent == null) continue;
                int s = Integer.parseInt(f[3]);
                int e = Integer.parseInt(f[4]);
                for (String p : parent.split(",")) {
                    Transcript t = data.transcripts.get(p.trim());
                    if (t != null) t.exons.add(new int[]{s, e});
                }
            }
        }

        return data;
    }

    /** Removes GFF3 prefixes like "gene:", "mRNA:", "exon:" */
    private static String cleanId(String id) {
        if (id == null) return null;
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    private static BufferedReader openUtf8StrippingBom(Path p) throws IOException {
        BufferedReader r = Files.newBufferedReader(p, StandardCharsets.UTF_8);
        r.mark(1);
        int c = r.read();
        if (c != 0xFEFF) r.reset();
        return r;
    }

    private static Map<String, String> parseAttributes(String s) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String field : s.strip().replaceAll(";$", "").split(";")) {
            field = field.strip();
            int eq = field.indexOf('=');
            if (eq <= 0) continue;
            map.put(field.substring(0, eq).strip(), field.substring(eq + 1).strip());
        }
        return map;
    }
}