
package edu.grinnell.psychMAP26.AGLV3;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * SymbolSentenceGenerator generates symbol sequences for both the learning and test phases.
 */
public class SymbolSentenceGenerator {
    private final SymbolLoader loader;
    private final Queue<List<BufferedImage>> sequenceQueue;
    private final Queue<Pair<List<LabeledSentence>, List<List<BufferedImage>>>> testQueue;

    /**
     * @param loader               the symbol group to draw the sentences with
     * @param testBank             which test bank (1 or 2) to load
     * @param learningRepetitions  how many times each learning sentence is shown (0 = no learning phase)
     */
    public SymbolSentenceGenerator(SymbolLoader loader, int testBank, int learningRepetitions) {
        this.loader = loader;
        this.sequenceQueue = new LinkedList<>();
        this.testQueue = new LinkedList<>();
        preloadSequences(testBank, learningRepetitions);
    }

    //converts a sentence into the images of this loader's symbol group
    private List<BufferedImage> toImages(List<SymbolPair> sentence) {
        List<BufferedImage> images = new ArrayList<>();
        for (SymbolPair p : sentence) {
            images.add(loader.getFromCategory(p.category, p.index));
        }
        return images;
    }

    //Loads the images into the queues for learning and test phase
    private void preloadSequences(int testBank, int learningRepetitions) {
        PreLoadSentence preload = new PreLoadSentence();

        // Learning phase: every sentence repeated, then all repetitions shuffled together
        List<List<BufferedImage>> learning = new ArrayList<>();
        for (int r = 0; r < learningRepetitions; r++) {
            for (List<SymbolPair> sentence : preload.sentences) {
                learning.add(toImages(sentence));
            }
        }
        Collections.shuffle(learning);
        sequenceQueue.addAll(learning);

        List<List<LabeledSentence>> testSentences =
                (testBank == 1) ? preload.testSentences1 : preload.testSentences2;

        // Exactly half the trials show the correct sentence first, in random order
        List<Boolean> correctFirst = new ArrayList<>();
        for (int i = 0; i < testSentences.size(); i++) {
            correctFirst.add(i % 2 == 0);
        }
        Collections.shuffle(correctFirst);

        /**
         * Load test phase sentences in pairs (correct + incorrect).
         * Each pair is put in its assigned order, converted to images, and the
         * sentence objects and their images are stored together in testQueue.
         */
        List<Pair<List<LabeledSentence>, List<List<BufferedImage>>>> tests = new ArrayList<>();
        for (int i = 0; i < testSentences.size(); i++) {
            List<LabeledSentence> pair = new ArrayList<>(testSentences.get(i));
            boolean firstIsCorrect = pair.get(0).isCorrect == 1;
            if (firstIsCorrect != correctFirst.get(i)) {
                Collections.reverse(pair);
            }
            List<List<BufferedImage>> imagesList = new ArrayList<>();
            for (LabeledSentence labeled : pair) {
                imagesList.add(toImages(labeled.sentence));
            }
            tests.add(new Pair<>(pair, imagesList));
        }

        // Shuffle test question order but keep pairs together
        Collections.shuffle(tests);
        testQueue.addAll(tests);
    }

    /**
     * Retrieves the next sentence in the learning phase.
     * @return a list of BufferedImages for the next sentence
     */
    public List<BufferedImage> getNextLearning() {
        return sequenceQueue.poll();
    }

    //checks if the learning queue is empty
    public boolean hasNextLearning() {
        return !sequenceQueue.isEmpty();
    }

    /**
     * Retrieves the next test question pair (correct + incorrect).
     * @return a pair containing the labeled sentences and their corresponding images
     */
    public Pair<List<LabeledSentence>, List<List<BufferedImage>>> getNextTestPair() {
        return testQueue.poll();
    }

    //checks if the test queue is empty
    public boolean hasNextTest() {
        return !testQueue.isEmpty();
    }
}
