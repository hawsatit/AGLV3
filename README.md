# AGL Speed Reader (AGLV3)
[Tanadanai (Fui) Hawsatitam]

***

## Description
This program is a stimuli presenter for the 2026 Psychology MAP investigating the effects of acute stress and presentation rate on visual statistical learning.

Each run of the program is one participant's whole session for one day:

- **Day 1:** Block 1 (100 learning sentences, then 24 test trials), an experimenter pause, then Block 2.
- **Day 2:** the same two blocks as a delayed test (test trials only).

## Running
**On another person's Windows PC (no Java needed):** run `./build-windows.sh` on a computer with a JDK. It creates `dist/AGLV3-windows.zip`, which contains the program, the images and its own Java runtime. Unzip it and double-click `Start AGLV3.bat`. Instructions for experimenters are in `HOW TO RUN.txt` inside the zip.

**From the source code:** double-click `AGLV3.bat`. It compiles and starts the program from whatever folder it is in. Java (JDK 21 or newer) must be installed.

Setup prompts:

1. **Day:** 1 or 2.
2. **Participant number:** 1–999 (e.g. `5` or `005`), the same on both days. The program builds the full 8-digit ID.
3. **Day 1 only:**
   - Stress condition (SECPT or Control).
   - Counterbalance code:

   | Code | Block 1 | Block 2 |
   |---|---|---|
   | 1 | Group 1 Fast | Group 2 Slow |
   | 2 | Group 1 Slow | Group 2 Fast |
   | 3 | Group 2 Fast | Group 1 Slow |
   | 4 | Group 2 Slow | Group 1 Fast |

4. **Day 2:** the Day 1 setup is loaded automatically from `results/participant_<N>_config.properties`.

Check the summary screen, then type `y`.

Fast = 100 ms between symbols, Slow = 2000 ms. Each symbol is shown for 500 ms, with a 2000 ms pause between sentences. Group 1 symbols are in `img/`, Group 2 in `img2/`.

The test bank (1 or 2) for Day 1 is chosen at random, and Day 2 always uses the other bank. In each test, exactly half the trials show the correct sentence first.

Between blocks the program waits for the experimenter to type `next`. Typing `quit` during a block aborts the session; scores of finished blocks stay saved, and the unfinished block is not recorded.

## Results
`results/participant_<N>_day<D>.csv` has one row per block:

```
FullID,Score
11101005,17
12112005,15
```

- `FullID` is the 8-digit lab ID for that block, built by the program. It is also shown on the setup summary.

  | Digit | Meaning |
  |---|---|
  | 1 | Day (1 or 2) |
  | 2 | Block (1 = first phase, 2 = second phase) |
  | 3 | Condition (0 = Control, 1 = SECPT) |
  | 4 | Rate (0 = Fast, 1 = Slow) |
  | 5 | Symbol group / image bank (1 or 2) |
  | 6–8 | Participant number |

  For example, `11101005` is Day 1, Block 1, SECPT, Fast, Group 1, participant 5.
- `Score` is the number of test trials answered correctly, out of 24.

### Combined spreadsheet
`results/all_results.csv` has every block score in one file, in the same `FullID,Score` format, grouped by participant. The program rebuilds it at the end of each session from all the per-day files. The per-day files are only ever read, so they stay the real record.

- If `all_results.csv` is open in Excel when a session ends, it can't be updated. The program warns and carries on; run `Combine results.bat` once it's closed.
- To merge data from other computers, copy their `participant_<N>_day<D>.csv` files into `results/` and run `Combine results.bat`. Duplicate IDs are reported as warnings.

The program never overwrites an existing results file.

## Sources

Java 21 (JDK)

Apache Net Beans IDE

VS Code

Custom JFrame wrapper for stimulus display
Based on standard Java Swing practices; no external source used.

Stimuli images were from "Lukics, K.S., Lukács, Á. Modality, presentation, domain and training effects in statistical learning. Sci Rep 12, 20878 (2022). https://doi.org/10.1038/s41598-022-24951-7", but re designed using Chat GPT
