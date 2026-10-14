package edu.grinnell.psychMAP26.AGLV3;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;


/**
 *
 * @author fui
 *
 * The Driver of the program. One run = one participant's full session for one day:
 * setup, Block 1, experimenter pause, Block 2.
 */
public class AGLV3 {
    //timing (ms)
    private static final int TIME_ON = 500;          // each symbol on screen
    private static final int SENTENCE_PAUSE = 2000;  // between sentences
    private static final int LEARNING_REPETITIONS = 2;  // 50 sentences x 2 = 100 per block

    //where the symbols are drawn
    private static final int X_START = 675;
    private static final int Y = 200;

    //typed by the participant or experimenter to abort mid-session
    private static final String QUIT = "quit";

    //to clear the console
    public static void clearConsole() {
        try {
            String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                // Try ANSI first
                System.out.print("\033[H\033[2J");
                System.out.flush();

                // Then fall back to 'cls' if ANSI fails to clear
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // Unix-like systems
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Just print empty lines if nothing else works
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }

    // Shows a setup prompt and reads a line; 'q' quits (nothing has been recorded yet)
    private static String setupPrompt(Scanner scanner, String prompt) {
        clearConsole();
        System.out.println(prompt);
        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("q")) {
            System.out.println("Exiting...");
            System.exit(0);
        }
        return input;
    }

    // Helper to get a number between min and max during setup
    private static int askChoice(Scanner scanner, String prompt, int min, int max) {
        String error = "";
        while (true) {
            String input = setupPrompt(scanner, error + prompt + "\n\nEnter " + min + "-" + max + " (or 'q' to quit):");
            try {
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                }
            } catch (NumberFormatException ignored) {}
            error = "Invalid input \"" + input + "\".\n\n";
        }
    }

    // Helper to get a positive integer (participant number)
    private static int askPositiveInt(Scanner scanner, String prompt) {
        String error = "";
        while (true) {
            String input = setupPrompt(scanner, error + prompt);
            try {
                int result = Integer.parseInt(input);
                if (result > 0) {
                    return result;
                }
            } catch (NumberFormatException ignored) {}
            error = "Invalid input \"" + input + "\". Please enter a positive number.\n\n";
        }
    }

    private static String askCondition(Scanner scanner) {
        int c = askChoice(scanner, "Stress condition:\n1) SECPT (stress)\n2) Control", 1, 2);
        return c == 1 ? "SECPT" : "Control";
    }

    private static int askCounterbalanceCode(Scanner scanner) {
        StringBuilder prompt = new StringBuilder("Counterbalance code:\n");
        for (int code = 1; code <= 4; code++) {
            prompt.append(code).append(") ").append(SessionConfig.describeCode(code)).append("\n");
        }
        return askChoice(scanner, prompt.toString().trim(), 1, 4);
    }

    // Day 1: enter the condition and code; the test bank is picked at random
    private static SessionConfig newConfig(Scanner scanner, int participant) {
        String condition = askCondition(scanner);
        int code = askCounterbalanceCode(scanner);
        int day1Bank = (Math.random() < 0.5) ? 1 : 2;
        return new SessionConfig(participant, condition, code, day1Bank);
    }

    // Day 2: reuse the Day 1 setup, or enter it by hand if it can't be found
    private static SessionConfig day2Config(Scanner scanner, int participant) throws IOException {
        SessionConfig saved = SessionConfig.load(participant);
        if (saved != null) {
            return saved;
        }
        setupPrompt(scanner, "WARNING: No Day 1 setup found for participant " + participant + "\n"
                + "(looked for " + SessionConfig.configFile(participant).getPath() + ")\n\n"
                + "Check the participant number. To continue anyway, enter the Day 1 settings by hand.\n\n"
                + "Press Enter to continue (or 'q' to quit).");
        String condition = askCondition(scanner);
        int code = askCounterbalanceCode(scanner);
        int day1Bank = askChoice(scanner, "Which test bank did this participant get on Day 1?\n"
                + "(It is in the TestBank column of their Day 1 results. Day 2 uses the other bank.)", 1, 2);
        return new SessionConfig(participant, condition, code, day1Bank);
    }

    private static String summary(SessionConfig config, int day) {
        StringBuilder s = new StringBuilder("Please confirm the session setup:\n\n");
        s.append("  Participant:       ").append(config.participant).append("\n");
        s.append("  Day:               ").append(day).append(day == 1 ? " (learning + test)" : " (delayed test only)").append("\n");
        s.append("  Condition:         ").append(config.condition).append("\n");
        s.append("  Counterbalance:    code ").append(config.code).append("\n");
        for (int block = 1; block <= 2; block++) {
            int group = config.symbolGroup(block);
            s.append("    Block ").append(block).append(": Group ").append(group)
                    .append(" (").append(SessionConfig.imageFolder(group)).append("), ")
                    .append(config.speed(block)).append(" (").append(config.isi(block)).append(" ms ISI)\n");
        }
        s.append("  Test bank:         ").append(config.testBank(day)).append("\n");
        s.append("  Results file:      ").append(SessionConfig.resultsFile(config.participant, day).getPath()).append("\n");
        s.append("\nIs this correct? (y/n)");
        return s.toString();
    }

    // Draws one sentence a symbol at a time
    private static void showSequence(SymbolWindow window, Graphics g, List<BufferedImage> sequence, int isi)
            throws InterruptedException {
        window.clear();
        for (BufferedImage img : sequence) {
            //scaling the stimuli
            int newWidth = img.getWidth() / 2;
            int newHeight = img.getHeight() / 2;
            g.drawImage(img, X_START, Y, newWidth, newHeight, null);
            //time on each symbol
            Thread.sleep(TIME_ON);
            window.clear();
            //time between symbols
            Thread.sleep(isi);
        }
        //time between sentences
        Thread.sleep(SENTENCE_PAUSE);
    }

    // Returns true if the experimenter typed "quit" (checked between sentences)
    private static boolean quitRequested(Scanner scanner) throws IOException {
        while (System.in.available() > 0) {
            if (scanner.nextLine().trim().equalsIgnoreCase(QUIT)) {
                return true;
            }
        }
        return false;
    }

    // Asks the participant to choose 1 or 2; returns -1 if "quit" was typed
    private static int askTestChoice(Scanner scanner, int questionNumber) {
        String error = "";
        while (true) {
            clearConsole();
            System.out.println(error + "Question " + questionNumber + ": Which sequence is more similar to the sequences you saw earlier?\n"
                    + "(Enter \"1\" for the first sequence or \"2\" for the second sequence,\n"
                    + "then press Enter to continue to the next question)");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase(QUIT)) {
                return -1;
            }
            if (input.equals("1") || input.equals("2")) {
                return Integer.parseInt(input);
            }
            error = "Please enter 1 or 2.\n\n";
        }
    }

    private static void quit(FileWriter logWriter, File resultsFile) throws IOException {
        logWriter.close();
        clearConsole();
        System.out.println("Session aborted. Trials completed so far are saved in " + resultsFile.getPath());
        System.exit(0);
    }

    /**
     * Runs one block: learning (Day 1 only) followed by the test.
     * Each test trial is written to the results file as soon as it is answered.
     */
    private static void runBlock(Scanner scanner, FileWriter logWriter, File resultsFile, SessionConfig config,
                                 int day, int block, SymbolSentenceGenerator generator)
            throws IOException, InterruptedException {
        int isi = config.isi(block);

        clearConsole();
        if (block == 1) {
            System.out.println("We will begin with the first set of symbols.");
        } else {
            System.out.println("Next, we will move on to a second set of symbols.\nThe task will be the same as before.");
        }
        System.out.println("Please attend to the screen.\n\nPress Enter when you are ready to begin.");
        scanner.nextLine();

        if (day == 1) {
            SymbolWindow window = new SymbolWindow();
            Graphics g = window.getDrawGraphics();
            window.bringToFront();
            //1 sec pause at the start
            Thread.sleep(1000);

            //while the learning queue has sentences and quit is not typed
            while (generator.hasNextLearning()) {
                if (quitRequested(scanner)) {
                    window.close();
                    quit(logWriter, resultsFile);
                }
                showSequence(window, g, generator.getNextLearning(), isi);
            }
            //close panel after learning phase
            window.close();
        }

        clearConsole();
        System.out.println("You will now see pairs of sequences, separated by a pause.\n"
                + "For each pair, select the sequence that looks more familiar or follows the same pattern as what you saw"
                + (day == 1 ? " just now." : " yesterday.") + "\n"
                + "If you are unsure, please make your best guess.\n\n"
                + "Press Enter to begin.");
        scanner.nextLine();

        int questionNumber = 1;
        while (generator.hasNextTest()) {
            //retrieve the pair of labeled sentences and the same sentences as images
            Pair<List<LabeledSentence>, List<List<BufferedImage>>> testPair = generator.getNextTestPair();
            List<LabeledSentence> labeledSentences = testPair.first;
            List<List<BufferedImage>> imagePairs = testPair.second;

            SymbolWindow window = new SymbolWindow();
            Graphics g = window.getDrawGraphics();
            window.bringToFront();
            //1 sec pause at the start
            Thread.sleep(1000);
            for (List<BufferedImage> sequence : imagePairs) {
                showSequence(window, g, sequence, isi);
            }
            window.close();

            // Ask for participant to choose 1 or 2, timing from when the question appears
            long askedAt = System.nanoTime();
            int choice = askTestChoice(scanner, questionNumber);
            long rt = (System.nanoTime() - askedAt) / 1_000_000;
            if (choice == -1) {
                quit(logWriter, resultsFile);
            }

            int correctIndex = labeledSentences.get(0).isCorrect == 1 ? 1 : 2;
            int chosenIsCorrect = labeledSentences.get(choice - 1).isCorrect;
            int violation = labeledSentences.get(0).violatedRule;

            logWriter.write(config.participant + "," + day + "," + config.condition + "," + config.code + ","
                    + config.testBank(day) + "," + block + "," + config.symbolGroup(block) + ","
                    + config.speed(block) + "," + isi + "," + questionNumber + "," + choice + ","
                    + chosenIsCorrect + "," + correctIndex + "," + violation + "," + rt + "\n");
            logWriter.flush();

            questionNumber++;
            Thread.sleep(1000);
        }
    }

    //main function does not take any command line arguments
    public static void main(String[] args) throws IOException, InterruptedException {

        //Scanner to read in user input from the command line
        Scanner scanner = new Scanner(System.in);

        // Setup: repeat until the experimenter confirms the summary
        int day;
        SessionConfig config;
        File resultsFile;
        while (true) {
            day = askChoice(scanner, "Select session day:\n1) Day 1 (learning + test)\n2) Day 2 (delayed test only)", 1, 2);
            int participant = askPositiveInt(scanner, "Please enter participant number (without the day digit):");

            resultsFile = SessionConfig.resultsFile(participant, day);
            if (resultsFile.exists()) {
                setupPrompt(scanner, "Results already exist: " + resultsFile.getPath() + "\n"
                        + "The program will not overwrite them. Move or rename that file, or use a different participant number.\n\n"
                        + "Press Enter to start over (or 'q' to quit).");
                continue;
            }

            config = (day == 1) ? newConfig(scanner, participant) : day2Config(scanner, participant);
            if (setupPrompt(scanner, summary(config, day)).equalsIgnoreCase("y")) {
                break;
            }
        }

        //load both symbol groups now so a missing image stops the program before the participant starts
        int learningRepetitions = (day == 1) ? LEARNING_REPETITIONS : 0;
        SymbolSentenceGenerator[] generators = new SymbolSentenceGenerator[2];
        for (int block = 1; block <= 2; block++) {
            SymbolLoader loader = new SymbolLoader(SessionConfig.imageFolder(config.symbolGroup(block)));
            generators[block - 1] = new SymbolSentenceGenerator(loader, config.testBank(day), learningRepetitions);
        }

        //save the setup for Day 2 (on Day 2, only if it had to be entered by hand)
        if (day == 1 || !SessionConfig.configFile(config.participant).exists()) {
            config.save();
        }

        //creates the output file, one row per test trial
        new File(SessionConfig.RESULTS_DIR).mkdirs();
        FileWriter logWriter = new FileWriter(resultsFile);
        logWriter.write("Participant,Day,Condition,CounterbalanceCode,TestBank,Block,SymbolGroup,Speed,ISI_ms,"
                + "Trial,Chosen,Correct,CorrectIndex,Violation,RT_ms\n");
        logWriter.flush();

        clearConsole();
        if (day == 1) {
            System.out.println("Welcome! In this part of the experiment, you will see sequences of symbols on the screen.\n"
                    + "Please pay attention to them; you will be asked about them afterwards.");
        } else {
            System.out.println("Welcome back! In this task, you will see pairs of sequences of symbols and choose\n"
                    + "which one looks more familiar or more consistent with what you saw yesterday.\n"
                    + "Please respond based on your intuition, even if you are not sure.");
        }
        System.out.println("\nPlease wait for the experimenter's instructions, then press Enter.");
        scanner.nextLine();

        runBlock(scanner, logWriter, resultsFile, config, day, 1, generators[0]);

        //experimenter pause between blocks (third saliva sample on Day 1)
        String input = "";
        while (!input.equalsIgnoreCase("next")) {
            clearConsole();
            System.out.println("You have finished the first set. Please notify the experimenter.\n\n\n"
                    + "(Experimenter: type next and press Enter to start Block 2.)");
            input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase(QUIT)) {
                quit(logWriter, resultsFile);
            }
        }

        runBlock(scanner, logWriter, resultsFile, config, day, 2, generators[1]);

        //close the file editor
        logWriter.close();
        clearConsole();
        System.out.print("Thank you, you have completed the computer portion of the experiment.\n"
                + "Please notify the experimenter!");
        scanner.nextLine();
        clearConsole();
        System.out.println("Session complete. Results saved to " + resultsFile.getPath());
        System.exit(0);
    }
}
