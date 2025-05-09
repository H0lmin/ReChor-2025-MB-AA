package ch.epfl.rechor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Character.isLetter;
import static java.util.Objects.requireNonNull;

/**
 * Immutable index of stop names supporting search with sub-queries.
 * <p>
 * Indexes main stop names and their alternatives, and returns matching mains sorted by descending
 * relevance based on coverage and word boundaries.
 * </p>
 */
public final class StopIndex {

    private static final Map<Character, String> ACCENT_EXPANSIONS = Map.of(
            'c', "cç",
            'a', "aáàâä",
            'e', "eéèêë",
            'i', "iíìîï",
            'o', "oóòôö",
            'u', "uúùûü"
    );

    private final Map<String, Set<String>> index;

    /**
     * Constructs the index from main stops and their alternative names.
     *
     * @param mainStops the list of primary stop names
     * @param altNames  map of alternative name -> primary name
     * @throws NullPointerException if any argument or contained value is null
     */
    public StopIndex(List<String> mainStops, Map<String, String> altNames) {
        requireNonNull(mainStops, "mainStops");
        requireNonNull(altNames, "altNames");

        Map<String, Set<String>> tmp = new HashMap<>();
        // include each main stop
        mainStops.forEach(main -> {
            requireNonNull(main, "stop name");
            tmp.put(main, new HashSet<>(List.of(main)));
        });
        // map alternatives to their main
        altNames.forEach((alt, main) -> {
            requireNonNull(alt, "alt name");
            requireNonNull(main, "main name");
            tmp.computeIfAbsent(main, _ -> new HashSet<>(List.of(main)))
                    .add(alt);
        });
        this.index = Collections.unmodifiableMap(tmp);
    }

    // build a regex for one sub-query, expanding accents and setting case flag
    private static Pattern buildRegex(String sub, boolean caseInsensitive) {
        StringBuilder sb = new StringBuilder();
        for (char c : sub.toCharArray()) {
            String exp = ACCENT_EXPANSIONS.get(Character.toLowerCase(c));
            if (exp != null && !Character.isUpperCase(c)) {
                sb.append('[').append(exp).append(']');
            } else {
                sb.append(Pattern.quote(Character.toString(c)));
            }
        }
        int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        return Pattern.compile(sb.toString(), flags);
    }

    // compute total match score or -1 if any sub-query fails
    private static int matchScore(String name, List<QueryInfo> subs) {
        if (subs.isEmpty()) {
            return 0;
        }
        return subs.stream().mapToInt(qi -> {
                    Matcher m = qi.pattern().matcher(name);
                    if (!m.find()) {
                        return -1;
                    }
                    return computeSubScore(name, m.start(), m.end(), qi.length());
                })
                .reduce((a, b) -> (a < 0 || b < 0) ? -1 : a + b)
                .orElse(-1);
    }

    // compute sub-score: base*4 at start of word, *2 at end of word
    private static int computeSubScore(String name, int start, int end, int subQueryLen) {
        int base = (subQueryLen * 100) / name.length();
        int factor = 1;
        if (start == 0 || !isLetter(name.charAt(start - 1))) {
            factor *= 4;
        }
        if (end == name.length() || !isLetter(name.charAt(end))) {
            factor *= 2;
        }
        return base * factor;
    }

    /**
     * Returns up to maxCount main stop names matching the query. Sub-queries are separated by
     * whitespace; each must match in a synonym. Results sorted by relevance score descending, then
     * name ascending.
     *
     * @param query    user search string
     * @param maxCount maximum number of results
     * @return list of matching main stop names
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


        // split into non-empty sub-queries
        List<QueryInfo> subQueries = Arrays.stream(query.trim().split("\\s+"))
                .filter(p -> !p.isEmpty())
                .map(p -> {
                    boolean hasUpper = p.chars().anyMatch(Character::isUpperCase);
                    Pattern pat = buildRegex(p, !hasUpper);
                    return new QueryInfo(pat, p.length());
                })
                .collect(Collectors.toList());

        // compute best score for each main name via its synonyms
        return index.entrySet().stream()
                .map(e -> {
                    String main = e.getKey();
                    int best = e.getValue().stream()
                            .mapToInt(name -> matchScore(name, subQueries))
                            .max()
                            .orElse(-1);
                    return new ScoredName(main, best);
                })
                .filter(sn -> sn.score >= 0)
                .sorted(Comparator.comparingInt(ScoredName::score).reversed()
                        .thenComparing(ScoredName::name)
                )
                .limit(maxCount)
                .map(ScoredName::name)
                .toList();
    }

    // holds pattern and original sub-query length
    private record QueryInfo(Pattern pattern, int length) {}

    // pairs a main name with its score
    private record ScoredName(String name, int score) {}
}
