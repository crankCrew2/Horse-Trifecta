import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * DEDICATION
 * 
 * To my dear friend Peter Ponte (10-Dec-1949 - 10-Nov-2020), the sharpest punter I ever knew—
 * whose laughter echoed louder than a photo-finish roar, and whose picks
 * turned longshots into legends. You taught me that every race is a story,
 * every bet a bond. Now, in the eternal paddock of heaven, may you ride
 * the clouds on steeds of lightning, always one length ahead, cashing
 * tickets on golden sunsets and claiming every purse with that unbeatable grin.
 * This code's for you, Pete—may your spirit forever win big.
 * 
 * Rest easy, brother. We'll meet at the rail someday.
 *
 * Horse.java: Optimizes a boxed trifecta bet in horse racing.
 * Purpose: For a full wheel (all 3-horse combos), calculates variable bet amounts on each straight
 * permutation (exact order) to ensure a fixed net payout (user-specified desiredWin, e.g., $100)
 * from the winning bet alone, no matter the order. This hedges a box: bet more on low-odds perms,
 * less on high-odds ones (e.g., $1 on longshots, $121 on favorites).
 * 
 * Input:
 * - Number of horses (min 3).
 * - Desired net win amount ($).
 * - Decimal odds for each horse (payout per $1 bet, e.g., 2.0 = $2 total/$1 net).
 * 
 * Math (Standard Approximation):
 * - For each perm (x=1st, y=2nd, z=3rd): straight trifecta odds ≈ (ox * oy * oz * 0.85),
 *   where 0.85 adjusts for ~15% track takeout (optimistic estimate).
 * - Bet amount = ceil(desiredWin / (odds - 1.0)) to guarantee net desiredWin from that bet.
 * - Skips if odds ≤2.0 (net <1, unprofitable). All 6 perms per combo use same odds (commutative).
 * - Total cost: Sum of all valid bets across C(n,3) combos * 6 perms.
 * 
 * Output:
 * - Disclaimer: Results are estimates, as actual payouts depend on final pari-mutuel pool.
 * - Lists each valid perm with odds and bet (sorted by odds desc).
 * - Total cost to place all bets.
 * - Optional filter: By min/max odds to focus on value (e.g., skip expensive favorites).
 * 
 * Note: Real payouts are pari-mutuel (pool-based), so this is an approximation. Use for strategy.
 */
public class Horse {

    // Inner class to store permutation data for output/sorting
    static class Bet {
        int horse1, horse2, horse3;  // Horses in order (1st, 2nd, 3rd)
        double odds;  // Combined trifecta odds
        int betAmount;  // Dollars to bet for desired net win

        Bet(int h1, int h2, int h3, double o, int b) {
            this.horse1 = h1;
            this.horse2 = h2;
            this.horse3 = h3;
            this.odds = o;
            this.betAmount = b;
        }

        @Override
        public String toString() {
            BigDecimal roundedOdds = new BigDecimal(odds).setScale(2, RoundingMode.HALF_UP);
            double frac = Math.max(0, odds - 1);  // Ensure non-negative fractional
            BigDecimal fracOdds = new BigDecimal(frac).setScale(2, RoundingMode.HALF_UP);
            return String.format("Horses %d-%d-%d: %.2f or %s:1 | Bet $%d to win $%d",
                    horse1, horse2, horse3, roundedOdds, fracOdds, betAmount, DESIRED_WIN);
        }
    }

    private static final int MIN_HORSES = 3;
    private static int DESIRED_WIN;  // User-specified fixed net payout

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Print disclaimer at start
        System.out.println("DISCLAIMER: All odds and bet amounts are estimates only. Actual payouts " +
                           "depend on the final pari-mutuel pool, which is only known after betting closes.");

        // Input: Number of horses with validation
        int numHorses;
        do {
            System.out.print("How many horses in the race? (minimum " + MIN_HORSES + "): ");
            numHorses = getIntInput(scanner);
            if (numHorses < MIN_HORSES) {
                System.out.println("Invalid: Must be at least " + MIN_HORSES + ".");
            }
        } while (numHorses < MIN_HORSES);

        // Input: Desired net win amount
        System.out.print("How much would you like to win in $? ");
        DESIRED_WIN = getIntInput(scanner);
        if (DESIRED_WIN <= 0) {
            System.out.println("Invalid: Win amount must be positive. Defaulting to $100.");
            DESIRED_WIN = 100;
        }

        // Input: Odds for each horse
        double[] horseOdds = new double[numHorses];
        System.out.println("Enter horse odds in $ value (payout per $1, e.g., 2.0):");
        for (int i = 0; i < numHorses; i++) {
            System.out.print("Horse #" + (i + 1) + ": ");
            horseOdds[i] = getDoubleInput(scanner);
            if (horseOdds[i] <= 1.0) {  // Odds <=1 impossible for profit
                System.out.println("Invalid: Odds must be >1.0. Retrying...");
                i--;
            }
        }

        // Generate and display all bets
        List<Bet> allBets = generateBets(horseOdds, numHorses);
        displayBets(allBets, "All Trifecta Box Bets");
        int totalCost = allBets.stream().mapToInt(b -> b.betAmount).sum();
        System.out.printf("Total cost to cover all valid combinations: $%d%n", totalCost);

        // Optional filtering loop
        char choice;
        do {
            System.out.print("Filter bets by odds range? (y/n): ");
            String input = scanner.next().toLowerCase();
            choice = input.isEmpty() ? 'n' : input.charAt(0);
            if (choice == 'y') {
                System.out.print("Minimum odds: ");
                int minOdds = getIntInput(scanner);
                System.out.print("Maximum odds: ");
                int maxOdds = getIntInput(scanner);
                if (minOdds >= maxOdds) {
                    System.out.println("Invalid: Minimum must be less than maximum. Skipping filter.");
                    continue;
                }

                List<Bet> filtered = allBets.stream()
                        .filter(b -> b.odds > minOdds && b.odds < maxOdds)
                        .sorted((a, b) -> Double.compare(b.odds, a.odds))  // Sort by odds descending
                        .toList();
                displayBets(filtered, String.format("Filtered Bets (%.0f < odds < %.0f)", (double) minOdds, (double) maxOdds));
                totalCost = filtered.stream().mapToInt(b -> b.betAmount).sum();
                System.out.printf("Total cost for filtered bets: $%d%n", totalCost);
            }
        } while (choice == 'y');

        scanner.close();
    }

    // Calculate straight trifecta odds for one permutation (x=1st, y=2nd, z=3rd)
    private static double calculateTrifectaOdds(double oddsX, double oddsY, double oddsZ) {
        // Approximate: product * 0.85 (15% takeout for optimistic estimate)
        return oddsX * oddsY * oddsZ * 0.85;
    }

    // Generate all bets for unique 3-horse combos (all 6 perms per combo)
    private static List<Bet> generateBets(double[] horseOdds, int numHorses) {
        List<Bet> bets = new ArrayList<>();
        // Pick 3 unique horses (i < j < k)
        for (int i = 0; i < numHorses - 2; i++) {
            for (int j = i + 1; j < numHorses - 1; j++) {
                for (int k = j + 1; k < numHorses; k++) {
                    // All 6 permutations
                    int[] horses = {i + 1, j + 1, k + 1};
                    int[][] perms = {
                        {0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}
                    };
                    double comboOdds = calculateTrifectaOdds(  // Same for all perms (commutative)
                            horseOdds[i], horseOdds[j], horseOdds[k]);
                    if (comboOdds <= 2.0) continue;  // Skip unprofitable combos (net <1)

                    double netOdds = comboOdds - 1.0;
                    int betAmount = (int) Math.ceil(DESIRED_WIN / netOdds);

                    for (int[] p : perms) {
                        bets.add(new Bet(horses[p[0]], horses[p[1]], horses[p[2]], comboOdds, betAmount));
                    }
                }
            }
        }
        // Sort by odds descending for readability
        Collections.sort(bets, (a, b) -> Double.compare(b.odds, a.odds));
        return bets;
    }

    // Display bets with a title
    private static void displayBets(List<Bet> bets, String title) {
        if (bets.isEmpty()) {
            System.out.println(title + ": No valid bets (all odds ≤2.0).");
            return;
        }
        System.out.println("\n--- " + title + " ---");
        bets.forEach(System.out::println);
    }

    // Safe integer input
    private static int getIntInput(Scanner scanner) {
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input. Enter an integer: ");
            scanner.next();
        }
        return scanner.nextInt();
    }

    // Safe double input
    private static double getDoubleInput(Scanner scanner) {
        while (!scanner.hasNextDouble()) {
            System.out.print("Invalid input. Enter a decimal number: ");
            scanner.next();
        }
        return scanner.nextDouble();
    }
}