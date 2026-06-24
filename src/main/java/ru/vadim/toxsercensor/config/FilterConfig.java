package ru.vadim.toxsercensor.config;

import java.util.ArrayList;
import java.util.List;

public final class FilterConfig {
    /** Exact word match (word boundaries) → full mask *** */
    public List<String> banwords = new ArrayList<>();

    /** Exact word match (word boundaries) → partial mask (first+last char visible) */
    public List<String> partialWords = new ArrayList<>();

    /** Root prefixes → full mask ***. Matches any word starting with the root. */
    public List<String> roots = new ArrayList<>();
}
