package ru.vadim.toxsercensor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vadim.toxsercensor.ToxserCensorMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FilterConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToxserCensorMod.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve(ToxserCensorMod.MOD_ID + ".json");

    private static volatile FilterConfig config = new FilterConfig();
    private static volatile boolean loaded;

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
    }

    private static FilterConfig readConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            return new FilterConfig();
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            FilterConfig loadedConfig = GSON.fromJson(reader, FilterConfig.class);
            return loadedConfig == null ? new FilterConfig() : loadedConfig;
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Config file {} is broken, falling back to defaults", CONFIG_PATH, e);
            return new FilterConfig();
        } catch (IOException e) {
            LOGGER.warn("Failed to read config file {}, falling back to defaults", CONFIG_PATH, e);
            return new FilterConfig();
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config file {}", CONFIG_PATH, e);
        }
    }

    public static synchronized boolean create() {
        if (Files.exists(CONFIG_PATH)) {
            return false;
        }

        config = new FilterConfig();
        loaded = true;
        save();
        return true;
    }

    public static synchronized boolean delete() {
        boolean existed = false;
        try {
            existed = Files.deleteIfExists(CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to delete config file {}", CONFIG_PATH, e);
        }

        config = new FilterConfig();
        loaded = true;
        return existed;
    }

    public static Path getConfigPath() {
        return CONFIG_PATH;
    }

    public static synchronized boolean addWord(boolean banwords, String value) {
        String normalized = normalizeWord(value);
        if (normalized == null) {
            return false;
        }

        List<String> target = banwords ? config.banwords : config.partialWords;
        if (target.contains(normalized)) {
            return false;
        }

        target.add(normalized);
        save();
        return true;
    }

    public static synchronized boolean removeWord(boolean banwords, String value) {
        String normalized = normalizeWord(value);
        if (normalized == null) {
            return false;
        }

        List<String> target = banwords ? config.banwords : config.partialWords;
        boolean removed = target.removeIf(entry -> entry.equalsIgnoreCase(normalized));
        if (removed) {
            save();
        }
        return removed;
    }

    public static synchronized boolean clearList(boolean banwords) {
        List<String> target = banwords ? config.banwords : config.partialWords;
        if (target.isEmpty()) {
            return false;
        }

        target.clear();
        save();
        return true;
    }

    public static synchronized List<String> getWords(boolean banwords) {
        return List.copyOf(banwords ? config.banwords : config.partialWords);
    }

    private static FilterConfig sanitizeConfig(FilterConfig source) {
        FilterConfig sanitized = new FilterConfig();
        sanitized.banwords = normalize(source == null ? null : source.banwords);
        sanitized.partialWords = normalize(source == null ? null : source.partialWords);
        return sanitized;
    }

    private static List<String> normalize(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> cleaned = new LinkedHashSet<>();
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return new ArrayList<>(cleaned);
    }

    private static String normalizeWord(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
