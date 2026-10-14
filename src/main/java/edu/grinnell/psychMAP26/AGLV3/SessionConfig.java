package edu.grinnell.psychMAP26.AGLV3;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * A participant's session settings. Day 1 saves them to the results folder
 * so Day 2 can load the same setup from the participant number alone.
 *
 * Counterbalance codes (Block 1 / Block 2):
 *   1 = Group 1 Fast / Group 2 Slow
 *   2 = Group 1 Slow / Group 2 Fast
 *   3 = Group 2 Fast / Group 1 Slow
 *   4 = Group 2 Slow / Group 1 Fast
 */
public class SessionConfig {
    public static final String RESULTS_DIR = "results";

    //inter-stimulus intervals for the two presentation rates
    public static final int FAST_ISI = 100;
    public static final int SLOW_ISI = 2000;

    //symbol group and speed of each block, indexed by [code - 1][block - 1]
    private static final int[][] GROUPS = {{1, 2}, {1, 2}, {2, 1}, {2, 1}};
    private static final boolean[][] FAST = {{true, false}, {false, true}, {true, false}, {false, true}};

    public final String participant;  // three digits, e.g. "005"
    public final String condition;   // "SECPT" or "Control"
    public final int code;           // counterbalance code 1-4
    public final int day1Bank;       // test bank used on Day 1; Day 2 uses the other one

    public SessionConfig(String participant, String condition, int code, int day1Bank) {
        this.participant = participant;
        this.condition = condition;
        this.code = code;
        this.day1Bank = day1Bank;
    }

    /**
     * Lab ID for one block, e.g. 11101005:
     *   1st digit: day (1/2)
     *   2nd digit: block (1 = first phase, 2 = second phase)
     *   3rd digit: condition (0 = Control, 1 = SECPT)
     *   4th digit: presentation rate (0 = Fast, 1 = Slow)
     *   5th digit: symbol group / image bank (1/2)
     *   last three: participant number
     */
    public String fullId(int day, int block) {
        return "" + day + block
                + (condition.equals("SECPT") ? 1 : 0)
                + (isi(block) == FAST_ISI ? 0 : 1)
                + symbolGroup(block)
                + participant;
    }

    public int testBank(int day) {
        return day == 1 ? day1Bank : 3 - day1Bank;
    }

    public int symbolGroup(int block) {
        return GROUPS[code - 1][block - 1];
    }

    public String speed(int block) {
        return FAST[code - 1][block - 1] ? "Fast" : "Slow";
    }

    public int isi(int block) {
        return FAST[code - 1][block - 1] ? FAST_ISI : SLOW_ISI;
    }

    //the image folder holding a symbol group
    public static String imageFolder(int group) {
        return group == 1 ? "img" : "img2";
    }

    //one-line description of a counterbalance code, e.g. "Block 1: Group 1 Fast -> Block 2: Group 2 Slow"
    public static String describeCode(int code) {
        SessionConfig c = new SessionConfig("", "", code, 1);
        return "Block 1: Group " + c.symbolGroup(1) + " " + c.speed(1)
                + "  ->  Block 2: Group " + c.symbolGroup(2) + " " + c.speed(2);
    }

    public static File configFile(String participant) {
        return new File(RESULTS_DIR, "participant_" + participant + "_config.properties");
    }

    public static File resultsFile(String participant, int day) {
        return new File(RESULTS_DIR, "participant_" + participant + "_day" + day + ".csv");
    }

    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("participant", participant);
        p.setProperty("condition", condition);
        p.setProperty("counterbalanceCode", String.valueOf(code));
        p.setProperty("day1TestBank", String.valueOf(day1Bank));
        new File(RESULTS_DIR).mkdirs();
        try (Writer w = new FileWriter(configFile(participant))) {
            p.store(w, "AGLV3 session setup (written on Day 1, read on Day 2)");
        }
    }

    //returns null if no saved setup exists for this participant
    public static SessionConfig load(String participant) throws IOException {
        File f = configFile(participant);
        if (!f.exists()) {
            return null;
        }
        Properties p = new Properties();
        try (Reader r = new FileReader(f)) {
            p.load(r);
        }
        return new SessionConfig(participant,
                p.getProperty("condition"),
                Integer.parseInt(p.getProperty("counterbalanceCode")),
                Integer.parseInt(p.getProperty("day1TestBank")));
    }
}
