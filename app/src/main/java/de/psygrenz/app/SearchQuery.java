package de.psygrenz.app;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SearchQuery {
    private static final Pattern TOKEN = Pattern.compile("(-?)\\\"([^\\\"]+)\\\"|(\\S+)");
    private final List<List<String>> groups = new ArrayList<>();
    private final List<String> excluded = new ArrayList<>();

    static SearchQuery parse(String raw) {
        SearchQuery query = new SearchQuery();
        List<String> group = new ArrayList<>();
        query.groups.add(group);
        Matcher matcher = TOKEN.matcher(raw == null ? "" : raw.trim());
        while (matcher.find()) {
            String token = matcher.group(3) != null ? matcher.group(3) : matcher.group(2);
            boolean negative = "-".equals(matcher.group(1)) || (matcher.group(3) != null && token.startsWith("-") && token.length() > 1);
            if (negative && token.startsWith("-")) token = token.substring(1);
            String upper = token.toUpperCase(Locale.GERMAN);
            if (!negative && (upper.equals("ODER") || upper.equals("OR"))) {
                group = new ArrayList<>(); query.groups.add(group); continue;
            }
            if (!negative && (upper.equals("UND") || upper.equals("AND"))) continue;
            token = token.trim().toLowerCase(Locale.GERMAN);
            if (token.isEmpty()) continue;
            if (negative) query.excluded.add(token); else group.add(token);
        }
        return query;
    }

    boolean matches(String text) {
        String lower = text.toLowerCase(Locale.GERMAN);
        for (String term : excluded) if (lower.contains(term)) return false;
        boolean hasPositive = false;
        for (List<String> group : groups) {
            if (group.isEmpty()) continue;
            hasPositive = true;
            boolean all = true;
            for (String term : group) if (!lower.contains(term)) { all = false; break; }
            if (all) return true;
        }
        return !hasPositive && !excluded.isEmpty();
    }

    List<String> highlightTerms() {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        for (List<String> group : groups) terms.addAll(group);
        return new ArrayList<>(terms);
    }
}
