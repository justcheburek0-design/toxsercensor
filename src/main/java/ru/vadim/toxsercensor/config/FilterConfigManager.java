package ru.vadim.toxsercensor.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import ru.vadim.toxsercensor.ToxserCensorMod;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FilterConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToxserCensorMod.MOD_ID);
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(ToxserCensorMod.MOD_ID + ".yml");
    private static final String DEFAULT_CONFIG_RESOURCE = "/default-config.yml";

    private static final Yaml YAML;

    static {
        LoaderOptions loaderOpts = new LoaderOptions();
        loaderOpts.setAllowDuplicateKeys(false);

        DumperOptions dumperOpts = new DumperOptions();
        dumperOpts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOpts.setPrettyFlow(true);
        dumperOpts.setIndent(2);
        dumperOpts.setAllowUnicode(true);

        Representer representer = new Representer(dumperOpts);
        representer.getPropertyUtils().setSkipMissingProperties(true);

        Constructor constructor = new Constructor(FilterConfig.class, loaderOpts);
        YAML = new Yaml(constructor, representer, dumperOpts, loaderOpts);
    }

    private static volatile FilterConfig config = new FilterConfig();
    private static volatile boolean loaded;
    private static volatile long lastSaveTime;

    private FilterConfigManager() {
    }

    public static FilterConfig get() {
        if (!loaded) {
            load();
        }
        return config;
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        reload();
    }

    public static synchronized void reload() {
        FilterConfig loadedConfig = readConfig();
        config = sanitizeConfig(loadedConfig);
        save();
        loaded = true;
        LOGGER.info("Config loaded: {} banwords, {} partial, {} roots, {} whitelisted",
                config.banwords.size(), config.partialWords.size(),
                config.roots.size(), config.whitelist.size());
    }

    private static FilterConfig readConfig() {
        // Migrate from old JSON if present
        Path oldPath = CONFIG_PATH.resolveSibling("toxsercensor.json");
        if (Files.exists(oldPath) && !Files.exists(CONFIG_PATH)) {
            LOGGER.info("Old JSON config found at {}, migrating would drop comments — start fresh YAML", oldPath);
            // The old JSON is left in place; new YAML will be created from default template
        }

        if (!Files.exists(CONFIG_PATH)) {
            return createDefault();
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            FilterConfig loadedConfig = YAML.loadAs(reader, FilterConfig.class);
            return loadedConfig == null ? new FilterConfig() : loadedConfig;
        } catch (Exception e) {
            LOGGER.warn("Config file {} is broken, falling back to defaults", CONFIG_PATH, e);
            return new FilterConfig();
        }
    }

    private static FilterConfig createDefault() {
        try (InputStream is = FilterConfigManager.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (is != null) {
                FilterConfig defaultConfig = YAML.loadAs(is, FilterConfig.class);
                if (defaultConfig != null) {
                    return defaultConfig;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not load default config from resources", e);
        }
        return new FilterConfig();
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                YAML.dump(config, writer);
            }
            lastSaveTime = System.currentTimeMillis();
        } catch (IOException e) {
            LOGGER.error("Failed to save config file {}", CONFIG_PATH, e);
        }
    }

    public static Path getConfigPath() {
        return CONFIG_PATH;
    }

    public static long getLastSaveTime() {
        return lastSaveTime;
    }

    // -- Banwords management --

    public static synchronized boolean addBanword(String value) {
        String normalized = normalizeWord(value);
        if (normalized == null || config.banwords.contains(normalized)) {
            return false;
        }
        config.banwords.add(normalized);
        save();
        return true;
    }

    public static synchronized boolean removeBanword(String value) {
        String normalized = normalizeWord(value);
        if (normalized == null) return false;
        boolean removed = config.banwords.removeIf(w -> w.equalsIgnoreCase(normalized));
        if (removed) save();
        return removed;
    }

    public static synchronized List<String> getBanwords() {
        return List.copyOf(config.banwords);
    }

    // -- Partial words management --

    public static synchronized boolean addPartialWord(String value) {
        String normalized = normalizeWord(value);
        if (normalized == null || config.partialWords.contains(normalized)) {
            return false;
        }
        config.partialWords.add(normalized);
        save();
        return true;
    }

    public static synchronized boolean removePartialWord(String value) {
        String normalized = normalizeWord(value);
        if (normalized == null) return false;
        boolean removed = config.partialWords.removeIf(w -> w.equalsIgnoreCase(normalized));
        if (removed) save();
        return removed;
    }

    public static synchronized List<String> getPartialWords() {
        return List.copyOf(config.partialWords);
    }

    // -- Roots management --

    public static synchronized boolean addRoot(String value) {
        String normalized = normalizeWord(value);
        if (normalized == null || config.roots.contains(normalized)) {
            return false;
        }
        config.roots.add(normalized);
        save();
        return true;
    }

    public static synchronized boolean removeRoot(String value) {
        String normalized = normalizeWord(value);
        if (normalized == null) return false;
        boolean removed = config.roots.removeIf(r -> r.equalsIgnoreCase(normalized));
        if (removed) save();
        return removed;
    }

    public static synchronized List<String> getRoots() {
        return List.copyOf(config.roots);
    }

    // -- Regex roots management --

    public static synchronized boolean addRegexRoot(String value) {
        String trimmed = normalizeWord(value);
        if (trimmed == null || config.regexRoots.contains(trimmed)) {
            return false;
        }
        // Validate it's a valid regex
        try {
            java.util.regex.Pattern.compile(trimmed);
        } catch (Exception e) {
            return false;
        }
        config.regexRoots.add(trimmed);
        save();
        return true;
    }

    public static synchronized boolean removeRegexRoot(String value) {
        String trimmed = normalizeWord(value);
        if (trimmed == null) return false;
        boolean removed = config.regexRoots.removeIf(r -> r.equals(trimmed));
        if (removed) save();
        return removed;
    }

    public static synchronized List<String> getRegexRoots() {
        return List.copyOf(config.regexRoots);
    }

    public static synchronized boolean clearRegexRoots() {
        if (config.regexRoots.isEmpty()) return false;
        config.regexRoots.clear();
        save();
        return true;
    }

    // -- Whitelist management --

    public static synchronized boolean addWhitelist(String uuid) {
        String normalized = normalizeWord(uuid);
        if (normalized == null || config.whitelist.contains(normalized)) {
            return false;
        }
        config.whitelist.add(normalized);
        save();
        return true;
    }

    public static synchronized boolean removeWhitelist(String uuid) {
        String normalized = normalizeWord(uuid);
        if (normalized == null) return false;
        boolean removed = config.whitelist.removeIf(w -> w.equalsIgnoreCase(normalized));
        if (removed) save();
        return removed;
    }

    public static synchronized List<String> getWhitelist() {
        return List.copyOf(config.whitelist);
    }

    public static boolean isWhitelisted(String uuid) {
        return uuid != null && config.whitelist.contains(uuid);
    }

    // -- Legacy methods (backward compat) --

    /** @deprecated use per-list methods */
    @Deprecated
    public static synchronized boolean addWord(boolean banwords, String value) {
        return banwords ? addBanword(value) : addPartialWord(value);
    }

    /** @deprecated use per-list methods */
    @Deprecated
    public static synchronized boolean removeWord(boolean banwords, String value) {
        return banwords ? removeBanword(value) : removePartialWord(value);
    }

    /** @deprecated use per-list methods */
    @Deprecated
    public static synchronized List<String> getWords(boolean banwords) {
        return banwords ? getBanwords() : getPartialWords();
    }

    // -- Clear methods --

    public static synchronized boolean clearBanwords() {
        if (config.banwords.isEmpty()) return false;
        config.banwords.clear();
        save();
        return true;
    }

    public static synchronized boolean clearPartialWords() {
        if (config.partialWords.isEmpty()) return false;
        config.partialWords.clear();
        save();
        return true;
    }

    public static synchronized boolean clearRoots() {
        if (config.roots.isEmpty()) return false;
        config.roots.clear();
        save();
        return true;
    }

    public static synchronized boolean clearWhitelist() {
        if (config.whitelist.isEmpty()) return false;
        config.whitelist.clear();
        save();
        return true;
    }

    // -- Internal helpers --

    private static FilterConfig sanitizeConfig(FilterConfig source) {
        FilterConfig sanitized = new FilterConfig();
        sanitized.banwords = normalize(source == null ? null : source.banwords);
        sanitized.partialWords = normalize(source == null ? null : source.partialWords);
        sanitized.roots = normalize(source == null ? null : source.roots);
        sanitized.regexRoots = normalize(source == null ? null : source.regexRoots);
        sanitized.whitelist = normalize(source == null ? null : source.whitelist);
        if (source != null && source.autoMute != null) {
            sanitized.autoMute.enabled = source.autoMute.enabled;
            sanitized.autoMute.maxViolations = source.autoMute.maxViolations;
            sanitized.autoMute.windowMinutes = source.autoMute.windowMinutes;
            sanitized.autoMute.muteDurationMinutes = source.autoMute.muteDurationMinutes;
        }
        return sanitized;
    }

    private static List<String> normalize(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> cleaned = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null) continue;
            String trimmed = entry.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return new ArrayList<>(cleaned);
    }

    private static String normalizeWord(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
