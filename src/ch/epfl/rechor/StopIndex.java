package ch.epfl.rechor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.isLetter;

/**
 * Represents an index of stop names in which we can perform non-strict searches as specified: -
 * Alternate names are treated like main names for searching, but only the main name is returned in
 * results. - Sub-queries are expanded with accent variations for a, e, i, o, u, c. - If no
 * uppercase letters appear in a sub-query, the match is case-insensitive. - The score of each
 * sub-query depends on how many characters it matches relative to the entire name length,
 * potentially multiplied by: *4 if it starts a word (or the name), *2 if it ends a word (or the
 * name). - Only the first occurrence of each sub-query is used for scoring. - The final score is
 * the sum of sub-scores for all sub-queries.
 */
public final class StopIndex {

    /**
     * For building expansions for certain letters, e.g. 'a' -> [aáàâä].
     */
    private static final Map<Character, String> ACCENT_EXPANSIONS = Map.of(
            'c', "cç",
            'a', "aáàâä",
            'e', "eéèêë",
            'i', "iíìîï",
            'o', "oóòôö",
            'u', "uúùûü"
    );
    // For each main name, store all synonyms (including the main name itself).
    // So mainName -> { mainName, altName1, altName2, ... }
    private final Map<String, Set<String>> index;

    /**
     * Builds a new stop index.
     *
     * @param mainStops the list of main stop names to index
     * @param altNames  a map of alternate name -> main name
     * @throws NullPointerException if any argument or contained value is null
     */
    public StopIndex(List<String> mainStops, Map<String, String> altNames) {
        Objects.requireNonNull(mainStops);
        Objects.requireNonNull(altNames);

        // Build the index: mainName -> set of synonyms
        Map<String, Set<String>> map = new HashMap<>();
        for (String m : mainStops) {
            Objects.requireNonNull(m);
            map.put(m, new HashSet<>(List.of(m)));
        }
        for (var entry : altNames.entrySet()) {
            String alt = Objects.requireNonNull(entry.getKey());
            String main = Objects.requireNonNull(entry.getValue());
            map.computeIfAbsent(main, k -> new HashSet<>(List.of(main)))
                    .add(alt);
        }
        this.index = Collections.unmodifiableMap(map);
    }

    /**
     * Builds a regex Pattern for one sub-query, expanding certain letters and applying
     * case-insensitivity if requested. E.g. "mez" -> "m[eéèêë]z" with CASE_INSENSITIVE if no
     * uppercase in "mez".
     */
    private static Pattern buildRegex(String subQuery, boolean caseInsensitive) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < subQuery.length(); i++) {
            char c = subQuery.charAt(i);
            String expansions = ACCENT_EXPANSIONS.get(Character.toLowerCase(c));
            if (expansions != null) {
                if (Character.isUpperCase(c)) {
                    sb.append(Pattern.quote(Character.toString(c)));
                } else {
                    sb.append('[').append(expansions).append(']');
                }
            } else {
                sb.append(Pattern.quote(Character.toString(c)));
            }
        }
        int flags = 0;
        if (caseInsensitive) {
            flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        return Pattern.compile(sb.toString(), flags);
    }

    /**
     * Computes a "match score" for the given candidate name against all sub-queries. If any
     * sub-query is not found, returns -1. Otherwise, sums the sub-scores of each sub-query's first
     * occurrence.
     *
     * @param candidate  the candidate stop name
     * @param subQueries the list of query patterns to match
     * @return the total match score or -1 if any sub-query is not found
     */
    private static int matchScore(String candidate, List<QueryInfo> subQueries) {
        int totalScore = 0;
        for (QueryInfo qi : subQueries) {
            Matcher m = qi.pattern().matcher(candidate);
            if (!m.find()) {
                return -1;
            }
            int subScore = computeSubScore(candidate, m.start(), m.end(), qi.length());
            totalScore += subScore;
        }
        return totalScore;
    }

    /**
     * Computes the sub-score as follows: base = (subQueryLength * 100) / candidate.length (integer
     * division), multiplied by 4 if at the start of a word, multiplied by 2 if at the end of a
     * word.
     *
     * @param candidate   the candidate stop name
     * @param start       the starting index of the match
     * @param end         the ending index of the match
     * @param subQueryLen the length of the sub-query
     * @return the computed sub-score
     */
    private static int computeSubScore(String candidate, int start, int end, int subQueryLen) {
        int base = (subQueryLen * 100) / candidate.length();
        int factor = 1;
        if (start == 0 || !isLetter(candidate.charAt(start - 1))) {
            factor *= 4;
        }
        if (end == candidate.length() || (end < candidate.length() && !isLetter(candidate.charAt(end)))) {
            factor *= 2;
        }
        return base * factor;
    }

    /**
     * Splits the query into sub-queries (by whitespace), creates a Pattern for each sub-query,
     * checks each main name (and its synonyms) for matches, and computes a final score. Returns up
     * to maxCount best results in descending order of score.
     *
     * @param query    the user input
     * @param maxCount maximum number of results
     * @return a list of main names sorted by descending score, length up to maxCount
     */
    public List<String> stopsMatching(String query, int maxCount) {
        Objects.requireNonNull(query);
        if (maxCount <= 0) {
            return List.of();
        }

        // 1) Split the query into sub-queries on whitespace.
        String[] parts = query.trim().split("\\s+");
        if (parts.length == 0) {
            return List.of();
        }

        // 2) Build a list of QueryInfo for each sub-query.
        List<QueryInfo> subQueries = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) continue; // Skip empty parts.
            boolean hasUppercase = p.chars().anyMatch(Character::isUpperCase);
            Pattern regex = buildRegex(p, !hasUppercase); // case-insensitive if no uppercase
            subQueries.add(new QueryInfo(regex, p.length()));
        }

        // 3) For each main name, find the best score among all synonyms.
        List<ScoredName> scored = new ArrayList<>();
        for (var entry : index.entrySet()) {
            String mainName = entry.getKey();
            Set<String> synonyms = entry.getValue();

            int bestScore = -1;
            for (String candidate : synonyms) {
                int s = matchScore(candidate, subQueries);
                if (s > bestScore) {
                    bestScore = s;
                }
            }
            if (bestScore >= 0) {
                scored.add(new ScoredName(mainName, bestScore));
            }
        }

        // 4) Sort in descending order of score, tie-breaking alphabetically for equal scores.
        scored.sort((a, b) -> {
            int cmp = Integer.compare(b.score, a.score);
            if (cmp != 0) return cmp;
            return a.name.compareTo(b.name);
        });

        // 5) Take up to maxCount main names.
        List<String> result = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < maxCount; i++) {
            result.add(scored.get(i).name);
        }
        return result;
    }

    /**
     * A private record to hold the regex Pattern and the original sub-query length.
     */
    private record QueryInfo(Pattern pattern, int length) {
    }

    /**
     * Helper record to store a mainName with its final score.
     */
    private record ScoredName(String name, int score) {
    }
}
