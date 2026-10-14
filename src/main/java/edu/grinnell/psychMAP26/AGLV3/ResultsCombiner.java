package edu.grinnell.psychMAP26.AGLV3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Rebuilds results/all_results.csv from every per-day results file.
 *
 * The per-day files are the real record and are only read, never changed.
 * The combined file can always be rebuilt, so if it can't be written
 * (e.g. it is open in Excel) nothing is lost.
 *
 * Run on its own (Combine results.bat) after copying other computers'
 * per-day files into the results folder.
 */
public class ResultsCombiner {
    public static final String COMBINED_NAME = "all_results.csv";

    //per-day files written by AGLV3, e.g. participant_005_day1.csv
    private static final String DAY_FILE = "participant_\\d{3}_day[12]\\.csv";
    //one block's row: 8-digit FullID and score
    private static final String ROW = "\\d{8},\\d{1,2}";

    /**
     * Rebuilds the combined file. Returns a short report of what was combined
     * and any problems found; throws IOException if the combined file can't be written.
     */
    public static String combine(File resultsDir) throws IOException {
        File[] files = resultsDir.listFiles((dir, name) -> name.matches(DAY_FILE));
        if (files == null) {
            files = new File[0];
        }
        Arrays.sort(files);

        List<String> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (File f : files) {
            for (String line : Files.readAllLines(f.toPath())) {
                line = line.trim();
                if (line.isEmpty() || line.equals("FullID,Score")) {
                    continue;
                }
                if (!line.matches(ROW)) {
                    warnings.add("Skipped unreadable line in " + f.getName() + ": " + line);
                    continue;
                }
                String id = line.substring(0, 8);
                if (!seen.add(id)) {
                    warnings.add("Duplicate ID " + id + " (also in " + f.getName() + ") - check the participant numbers");
                }
                rows.add(line);
            }
        }

        //group by participant (last three digits), then day, then block
        rows.sort(Comparator.comparing((String r) -> r.substring(5, 8))
                .thenComparing(r -> r.substring(0, 2)));

        //write to a temporary file first so a failed write never leaves a half-written combined file
        File combined = new File(resultsDir, COMBINED_NAME);
        File temp = new File(resultsDir, COMBINED_NAME + ".tmp");
        List<String> out = new ArrayList<>();
        out.add("FullID,Score");
        out.addAll(rows);
        try {
            Files.write(temp.toPath(), out);
            Files.move(temp.toPath(), combined.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            temp.delete();
            throw new IOException("Could not update " + combined.getPath()
                    + " (is it open in Excel? Close it and run Combine results again).", e);
        }

        StringBuilder report = new StringBuilder("Combined " + rows.size() + " block scores from "
                + files.length + " files into " + combined.getPath());
        for (String w : warnings) {
            report.append("\nWARNING: ").append(w);
        }
        return report.toString();
    }

    public static void main(String[] args) {
        try {
            System.out.println(combine(new File(SessionConfig.RESULTS_DIR)));
        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
