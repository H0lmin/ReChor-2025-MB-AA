package ch.epfl.rechor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Immutable index of stop names supporting search with sub-queries.
 * Indexes primary names and their alternatives, returning matches
 * sorted by relevance (coverage and word boundaries).
 */
public final class StopIndex {
    private static final Map<Character, String> ACCENT_MAP = Map.of(
            'c', "cç",
            'a', "aàáâä",
            'e', "eèéêë",
            'i', "iìíîï",
            'o', "oòóôö",
            'u', "uùúûü"
    );

    private final Map<String, Set<String>> stops;

    /**
     * Builds an immutable stop index.
     * @param mainStops list of primary stop names
     * @param altNames  mapping of alternative names to primary names
     * @throws NullPointerException if arguments or entries are null
     */
    public StopIndex(List<String> mainStops, Map<String, String> altNames) {
        requireNonNull(mainStops, "mainStops");
        requireNonNull(altNames, "altNames");
        Map<String, Set<String>> map = new HashMap<>();
        for (String main : mainStops) {
            requireNonNull(main, "stop name");
            map.put(main, new HashSet<>(List.of(main)));
        }
        for (Map.Entry<String, String> entry : altNames.entrySet()) {
            String alt = requireNonNull(entry.getKey(), "alt name");
            String main = requireNonNull(entry.getValue(), "main name");
            map.computeIfAbsent(main, k -> new HashSet<>(List.of(main))).add(alt);
        }
        this.stops = Collections.unmodifiableMap(map);
    }

    /**
     * Returns up to maxCount primary stop names matching the query.
     * @param query    search string (sub-queries separated by whitespace)
     * @param maxCount maximum number of results (must be ≥0)
     * @return list of matching primary names, sorted by relevance, then name
     * @throws NullPointerException     if query is null
     * @throws IllegalArgumentException if maxCount is negative
     */
    public List<String> stopsMatching(String query, int maxCount) {
        requireNonNull(query, "query");
        if (maxCount < 0) {
            throw new IllegalArgumentException("maxCount must be ≥ 0");
        }
        if (maxCount == 0) {
            return List.of();
        }

        List<String> terms = Arrays.stream(query.trim().split("\\s+"))
                .filter(s -> !s.isBlank())
                .toList();

        List<Pattern> patterns = terms.stream()
                .map(term -> Pattern.compile(
                        buildRegex(term),
                        hasUpper(term) ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                ))
                .toList();

        return stops.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), scoreSet(e.getValue(), patterns)))
                .filter(e -> e.getValue() >= 0)
                .sorted(Comparator.<Map.Entry<String,Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(maxCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static String buildRegex(String term) {
        StringBuilder sb = new StringBuilder();
        for (char c : term.toCharArray()) {
            char lc = Character.toLowerCase(c);
            String alt = ACCENT_MAP.get(lc);
            if (alt != null && !Character.isUpperCase(c)) {
                sb.append('[').append(alt).append(']');
            } else {
                sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return sb.toString();
    }

    private static boolean hasUpper(String s) {
        for (char c : s.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    private static int scoreSet(Set<String> names, List<Pattern> patterns) {
        int best = -1;
        for (String name : names) {
            int sc = scoreName(name, patterns);
            if (sc > best) {
                best = sc;
            }
        }
        return best;
    }

    private static int scoreName(String name, List<Pattern> patterns) {
        int total = 0;
        for (Pattern pat : patterns) {
            Matcher m = pat.matcher(name);
            if (!m.find()) {
                return -1;
            }
            int start = m.start();
            int end = m.end();
            int base = (end - start) * 100 / name.length();
            int factor = 1;
            if (start == 0 || !Character.isLetter(name.charAt(start - 1))) {
                factor *= 4;
            }
            if (end == name.length() || !Character.isLetter(name.charAt(end))) {
                factor *= 2;
            }
            total += base * factor;
        }
        return total;
    }
}