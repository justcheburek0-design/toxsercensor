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

    /** Regex patterns for flexible matching in roots mode */
    public List<String> regexRoots = new ArrayList<>();

    /** UUIDs of players exempt from filtering */
    public List<String> whitelist = new ArrayList<>();

    /** Auto-mute settings */
    public AutoMuteConfig autoMute = new AutoMuteConfig();

    public static final class AutoMuteConfig {
        public boolean enabled = false;
        public int maxViolations = 3;
        public int windowMinutes = 5;
        public int muteDurationMinutes = 10;
    }
}
