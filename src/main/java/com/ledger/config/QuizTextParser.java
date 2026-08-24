package com.ledger.config;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class QuizTextParser {

    public static class ParsedQ {
        public String text, a, b, c, d, ans;
        public ParsedQ(String text, String a, String b, String c, String d, String ans) {
            this.text = text; this.a = a; this.b = b; this.c = c; this.d = d; this.ans = ans;
        }
    }

    private QuizTextParser() {}

    public static String extractText(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) return "";
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (name.endsWith(".pdf")) {
            try (InputStream in = file.getInputStream(); PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                return stripper.getText(doc);
            }
        }
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    public static List<ParsedQ> parse(String content) {
        List<ParsedQ> list = new ArrayList<>();
        if (content == null || content.isBlank()) return list;
        content = content.replace("\r\n", "\n").replace("\r", "\n");

        // Format A: Q: / Q1: with A B C D ANS
        if (content.toLowerCase().contains("q:") || Pattern.compile("(?im)^Q\\d+\\s*:").matcher(content).find()) {
            content = content.replaceAll("(?im)^Q\\d+\\s*:", "Q:");
            content = content.replaceAll("(?im)^ANS(?:WER)?\\s*:", "ANS:");
            for (String block : content.split("(?im)(?=^Q:)")) {
                ParsedQ q = parseMcqBlock(block);
                if (q != null) list.add(q);
            }
            if (!list.isEmpty()) return list;
        }

        // Format B: numbered 1. 2. with optional A) B) C) D)
        Pattern numStart = Pattern.compile("(?m)^\\s*(\\d+)[.)]\\s+(.+)$");
        String[] lines = content.split("\n");
        StringBuilder curQ = null;
        String oa = "", ob = "", oc = "", od = "", ans = "A";
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            Matcher nm = numStart.matcher(line);
            if (nm.matches()) {
                if (curQ != null && curQ.length() > 0) {
                    list.add(new ParsedQ(curQ.toString().trim(),
                            empty(oa, "Option A"), empty(ob, "Option B"),
                            empty(oc, "Option C"), empty(od, "Option D"), ans));
                }
                curQ = new StringBuilder(nm.group(2).trim());
                oa = ob = oc = od = ""; ans = "A";
            } else if (curQ != null) {
                String low = line.toLowerCase();
                if (low.matches("^[a][).:\\-\\s].*")) oa = line.replaceFirst("(?i)^a[).:\\-\\s]+", "").trim();
                else if (low.matches("^[b][).:\\-\\s].*")) ob = line.replaceFirst("(?i)^b[).:\\-\\s]+", "").trim();
                else if (low.matches("^[c][).:\\-\\s].*")) oc = line.replaceFirst("(?i)^c[).:\\-\\s]+", "").trim();
                else if (low.matches("^[d][).:\\-\\s].*")) od = line.replaceFirst("(?i)^d[).:\\-\\s]+", "").trim();
                else if (low.startsWith("ans")) {
                    String a = line.replaceFirst("(?i)^ans(?:wer)?\\s*:?\\s*", "").trim().toUpperCase();
                    if (!a.isEmpty()) ans = String.valueOf(a.charAt(0));
                } else {
                    curQ.append(" ").append(line);
                }
            }
        }
        if (curQ != null && curQ.length() > 0) {
            list.add(new ParsedQ(curQ.toString().trim(),
                    empty(oa, "Option A"), empty(ob, "Option B"),
                    empty(oc, "Option C"), empty(od, "Option D"), ans));
        }

        // Format C: plain lines as short-answer style questions
        if (list.isEmpty()) {
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.length() < 5) continue;
                if (line.startsWith("#")) continue;
                list.add(new ParsedQ(line, "True", "False", "Option C", "Option D", "A"));
            }
        }
        return list;
    }

    private static ParsedQ parseMcqBlock(String block) {
        if (block == null || block.isBlank()) return null;
        String qText = "", oa = "", ob = "", oc = "", od = "", ans = "A";
        for (String raw : block.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String low = line.toLowerCase();
            if (low.startsWith("q:")) qText = line.substring(2).trim().replaceFirst("^\\d+\\s*:\\s*", "");
            else if (low.matches("^a\\s*[:.)].*")) oa = line.replaceFirst("(?i)^a\\s*[:.)]\\s*", "").trim();
            else if (low.matches("^b\\s*[:.)].*")) ob = line.replaceFirst("(?i)^b\\s*[:.)]\\s*", "").trim();
            else if (low.matches("^c\\s*[:.)].*")) oc = line.replaceFirst("(?i)^c\\s*[:.)]\\s*", "").trim();
            else if (low.matches("^d\\s*[:.)].*")) od = line.replaceFirst("(?i)^d\\s*[:.)]\\s*", "").trim();
            else if (low.startsWith("ans")) {
                String a = line.replaceFirst("(?i)^ans(?:wer)?\\s*:?\\s*", "").trim().toUpperCase();
                if (!a.isEmpty()) ans = String.valueOf(a.charAt(0));
            }
        }
        if (qText.isBlank()) return null;
        return new ParsedQ(qText, empty(oa, "Option A"), empty(ob, "Option B"),
                empty(oc, "Option C"), empty(od, "Option D"), ans);
    }

    private static String empty(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }
}
