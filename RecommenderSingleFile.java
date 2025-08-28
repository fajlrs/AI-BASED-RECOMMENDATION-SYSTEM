import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * RecommenderSingleFile.java
 *
 * A single-file, dependency-free Java program that implements a
 * user-based collaborative filtering recommendation engine.
 *
 * Features:
 *  - Loads ratings from CSV (auto-generates a sample dataset on first run)
 *  - Computes user-user cosine similarity
 *  - Chooses top-K neighbors
 *  - Predicts scores for unseen items and outputs Top-N recommendations
 *
 * How to compile:
 *   javac RecommenderSingleFile.java
 *
 * How to run (defaults shown):
 *   java RecommenderSingleFile [targetUserId=U1] [kNeighbors=3] [topN=5] [dataFile=sample_ratings.csv]
 *
 * Example:
 *   java RecommenderSingleFile U3 3 5
 *
 * CSV format:
 *   userId,itemId,rating
 *
 * Author: (Your Name)
 * Date: (Update date as needed)
 */
public class RecommenderSingleFile {

    // ======== Config ========
    private static final String DEFAULT_DATA_FILE = "sample_ratings.csv";
    private static final int DEFAULT_K = 3;
    private static final int DEFAULT_TOP_N = 5;
    private static final String DEFAULT_USER = "U1";

    // ======== Data Structures ========
    // user -> (item -> rating)
    private final Map<String, Map<String, Double>> userItemRatings = new HashMap<>();
    // item -> (user -> rating)
    private final Map<String, Map<String, Double>> itemUserRatings = new HashMap<>();

    // ======== Entry Point ========
    public static void main(String[] args) {
        String targetUser = args.length > 0 ? args[0].trim() : DEFAULT_USER;
        int k = args.length > 1 ? parseIntOrDefault(args[1], DEFAULT_K) : DEFAULT_K;
        int topN = args.length > 2 ? parseIntOrDefault(args[2], DEFAULT_TOP_N) : DEFAULT_TOP_N;
        String dataFile = args.length > 3 ? args[3].trim() : DEFAULT_DATA_FILE;

        ensureSampleData(dataFile); // create sample file if missing

        RecommenderSingleFile app = new RecommenderSingleFile();
        try {
            app.loadData(dataFile);
        } catch (IOException e) {
            System.err.println("Failed to read data file: " + e.getMessage());
            return;
        }

        if (!app.userItemRatings.containsKey(targetUser)) {
            System.out.println("⚠️ Target user '" + targetUser + "' not found in dataset. Available users: "
                    + app.userItemRatings.keySet());
            return;
        }

        System.out.println("\n===== AI-Based Recommendation System (User-Based CF) =====");
        System.out.println("Target User : " + targetUser);
        System.out.println("Neighbors K : " + k);
        System.out.println("Top-N       : " + topN);
        System.out.println("Data File   : " + dataFile);

        // Compute similarities and neighbors
        Map<String, Double> sims = app.computeUserSimilarities(targetUser);
        List<Map.Entry<String, Double>> neighbors = app.topKNeighbors(sims, k);

        // Show neighbors
        app.printNeighbors(targetUser, neighbors);

        // Generate recommendations
        List<Recommendation> recs = app.recommendForUser(targetUser, neighbors, topN);

        // Fallback if empty
        if (recs.isEmpty()) {
            System.out.println("\nNo personalized recommendations found (not enough overlap).");
            System.out.println("Showing popular items the user hasn't rated yet:");
            recs = app.popularityFallback(targetUser, topN);
        }

        // Print recommendations
        app.printRecommendations(targetUser, recs);
        System.out.println("\n✅ Done.");
    }

    // ======== Core Logic ========

    /** Load CSV data into user-item and item-user maps. */
    private void loadData(String file) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(Paths.get(file))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("userId")) continue;
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    System.err.println("Skipping malformed line " + lineNum + ": " + line);
                    continue;
                }
                String user = parts[0].trim();
                String item = parts[1].trim();
                Double rating = parseDoubleOrNull(parts[2].trim());
                if (rating == null) {
                    System.err.println("Skipping invalid rating on line " + lineNum + ": " + line);
                    continue;
                }
                userItemRatings.computeIfAbsent(user, k -> new HashMap<>()).put(item, rating);
                itemUserRatings.computeIfAbsent(item, k -> new HashMap<>()).put(user, rating);
            }
        }
    }

    /** Compute cosine similarity between targetUser and all other users (based on overlapping items). */
    private Map<String, Double> computeUserSimilarities(String targetUser) {
        Map<String, Double> sims = new HashMap<>();
        Map<String, Double> targetRatings = userItemRatings.getOrDefault(targetUser, Collections.emptyMap());

        for (String other : userItemRatings.keySet()) {
            if (other.equals(targetUser)) continue;

            Map<String, Double> otherRatings = userItemRatings.get(other);
            // Collect common items
            Set<String> common = new HashSet<>(targetRatings.keySet());
            common.retainAll(otherRatings.keySet());

            if (common.isEmpty()) {
                sims.put(other, 0.0);
                continue;
            }

            double dot = 0.0, normA = 0.0, normB = 0.0;
            for (String item : common) {
                double a = targetRatings.get(item);
                double b = otherRatings.get(item);
                dot += a * b;
            }
            for (double a : targetRatings.values()) normA += a * a;
            for (double b : otherRatings.values())   normB += b * b;
            double denom = Math.sqrt(normA) * Math.sqrt(normB);
            double sim = (denom == 0.0) ? 0.0 : (dot / denom);

            // Optional: shrinkage for small overlap (penalize tiny intersections)
            double shrinkage = 1.0 * common.size() / (common.size() + 5.0); // 0..1
            sims.put(other, sim * shrinkage);
        }
        return sims;
    }

    /** Get top-K neighbors by similarity (positive sims first). */
    private List<Map.Entry<String, Double>> topKNeighbors(Map<String, Double> sims, int k) {
        return sims.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(Math.max(k, 0))
                .collect(Collectors.toList());
    }

    /** Predict ratings for unseen items for targetUser using weighted average over neighbors. */
    private List<Recommendation> recommendForUser(String targetUser,
                                                  List<Map.Entry<String, Double>> neighbors,
                                                  int topN) {
        Map<String, Double> targetRatings = userItemRatings.get(targetUser);
        Set<String> seen = targetRatings.keySet();

        // Candidate items = items rated by neighbors but not by target
        Set<String> candidateItems = new HashSet<>();
        for (Map.Entry<String, Double> e : neighbors) {
            String nbr = e.getKey();
            candidateItems.addAll(userItemRatings.getOrDefault(nbr, Collections.emptyMap()).keySet());
        }
        candidateItems.removeAll(seen);
        if (candidateItems.isEmpty()) return Collections.emptyList();

        List<Recommendation> results = new ArrayList<>();
        for (String item : candidateItems) {
            double num = 0.0, den = 0.0;
            for (Map.Entry<String, Double> e : neighbors) {
                String nbr = e.getKey();
                double sim = e.getValue();
                Double r = userItemRatings.getOrDefault(nbr, Collections.emptyMap()).get(item);
                if (r != null && sim > 0.0) { // consider only positive similarity to avoid noise
                    num += sim * r;
                    den += Math.abs(sim);
                }
            }
            if (den > 0.0) {
                double score = num / den;
                results.add(new Recommendation(item, score));
            }
        }

        return results.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(Math.max(topN, 0))
                .collect(Collectors.toList());
    }

    /** Popularity-based fallback if personalization yields nothing. */
    private List<Recommendation> popularityFallback(String targetUser, int topN) {
        Map<String, Double> pop = new HashMap<>();
        Map<String, Integer> cnt = new HashMap<>();

        for (Map.Entry<String, Map<String, Double>> e : itemUserRatings.entrySet()) {
            String item = e.getKey();
            for (double r : e.getValue().values()) {
                pop.put(item, pop.getOrDefault(item, 0.0) + r);
                cnt.put(item, cnt.getOrDefault(item, 0) + 1);
            }
        }
        Map<String, Double> avg = new HashMap<>();
        for (String item : pop.keySet()) {
            avg.put(item, pop.get(item) / cnt.get(item));
        }

        Set<String> seen = userItemRatings.getOrDefault(targetUser, Collections.emptyMap()).keySet();
        return avg.entrySet().stream()
                .filter(e -> !seen.contains(e.getKey()))
                .map(e -> new Recommendation(e.getKey(), e.getValue()))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(Math.max(topN, 0))
                .collect(Collectors.toList());
    }

    // ======== Output Helpers ========

    private void printNeighbors(String targetUser, List<Map.Entry<String, Double>> neighbors) {
        System.out.println("\n--- Top Neighbors for " + targetUser + " ---");
        if (neighbors.isEmpty()) {
            System.out.println("(none)");
            return;
        }
        int rank = 1;
        for (Map.Entry<String, Double> e : neighbors) {
            System.out.printf("%d) %-6s  similarity = %.4f%n", rank++, e.getKey(), e.getValue());
        }
    }

    private void printRecommendations(String targetUser, List<Recommendation> recs) {
        System.out.println("\n--- Top Recommendations for " + targetUser + " ---");
        if (recs.isEmpty()) {
            System.out.println("(none)");
            return;
        }
        int rank = 1;
        for (Recommendation r : recs) {
            System.out.printf("%d) %-6s  predictedScore = %.3f%n", rank++, r.itemId, r.score);
        }
    }

    // ======== Utility ========

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static Double parseDoubleOrNull(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }

    /** If file missing, create a compact, realistic sample dataset. */
    private static void ensureSampleData(String file) {
        if (Files.exists(Paths.get(file))) return;

        // Sample users U1..U6, items I1..I8, ratings 1..5
        String[] lines = new String[] {
            "userId,itemId,rating",
            "U1,I1,5", "U1,I2,4", "U1,I3,2", "U1,I4,1",
            "U2,I1,5", "U2,I2,5", "U2,I3,1", "U2,I5,4",
            "U3,I2,4", "U3,I3,5", "U3,I4,2", "U3,I6,5",
            "U4,I1,1", "U4,I3,4", "U4,I5,5", "U4,I7,4",
            "U5,I2,5", "U5,I4,1", "U5,I6,4", "U5,I8,5",
            "U6,I1,4", "U6,I3,2", "U6,I5,5", "U6,I8,4"
        };

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(file))) {
            for (String l : lines) {
                bw.write(l); bw.newLine();
            }
            System.out.println("📝 Created sample dataset: " + file);
        } catch (IOException e) {
            System.err.println("Failed to create sample data: " + e.getMessage());
        }
    }

    // ======== Data Class ========

    private static class Recommendation {
        final String itemId;
        final double score;
        Recommendation(String itemId, double score) {
            this.itemId = itemId;
            this.score = score;
        }
    }
}
