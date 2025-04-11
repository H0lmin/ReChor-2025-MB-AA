package ch.epfl.rechor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Character.isLetter;

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

    public List<String> stopsMatching(String query, int maxCount) {
        Objects.requireNonNull(query);
        if (maxCount <= 0) {
            return List.of();
        }

        String[] parts = query.trim().split("\\s+");
        if (parts.length == 0) {
            return List.of();
        }

        List<QueryInfo> subQueries = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            boolean hasUppercase = p.chars().anyMatch(Character::isUpperCase);
            Pattern regex = buildRegex(p, !hasUppercase);
            subQueries.add(new QueryInfo(regex, p.length()));
        }

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

        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < maxCount; i++) {
            result.add(scored.get(i).name);
        }
        return result;
    }

    private record QueryInfo(Pattern pattern, int length) {
    }

    private record ScoredName(String name, int score) {
    }
}
