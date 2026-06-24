package ru.vadim.toxsercensor.filter;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vadim.toxsercensor.ToxserCensorMod;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs chat violations to {@code logs/toxsercensor.log}.
 * Thread-safe, bounded to ~10MB then trimmed to last 5MB.
 */
public final class FilterLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToxserCensorMod.MOD_ID);
    private static final long MAX_SIZE = 10L * 1024 * 1024;   // 10 MB
    private static final long TRIM_SIZE = 5L * 1024 * 1024;   // keep last 5 MB
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String HEADER = "[TIMESTAMP] PLAYER (UUID) -> \"ORIGINAL\" -> \"FILTERED\"";

    private static final Path LOG_PATH = FabricLoader.getInstance()
            .getGameDir()
            .resolve("logs")
            .resolve("toxsercensor.log");

    private FilterLogger() {
    }

    /** Write one violation line to the log file. Thread-safe. */
    public static synchronized void log(String playerName, String uuid, String original, String filtered) {
        try {
            Files.createDirectories(LOG_PATH.getParent());

            String line = String.format("[%s] %s (%s) -> \"%s\" -> \"%s\"%n",
                    LocalDateTime.now().format(TIMESTAMP),
                    playerName, uuid,
                    escape(original), escape(filtered));

            // Write append
            try (BufferedWriter w = Files.newBufferedWriter(LOG_PATH, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(line);
            }

            // Trim if too large
            if (Files.size(LOG_PATH) > MAX_SIZE) {
                trimLog();
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to write violation log", e);
        }
    }

    /** Simple header when log is first created. */
    public static synchronized void ensureHeader() {
        try {
            if (!Files.exists(LOG_PATH) || Files.size(LOG_PATH) == 0) {
                Files.createDirectories(LOG_PATH.getParent());
                Files.writeString(LOG_PATH, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to write log header", e);
        }
    }

    private static void trimLog() throws IOException {
        byte[] all = Files.readAllBytes(LOG_PATH);
        if (all.length <= TRIM_SIZE) return;
        // Keep only the last TRIM_SIZE bytes, preserving line integrity
        String content = new String(all, StandardCharsets.UTF_8);
        int start = content.length() - (int) TRIM_SIZE;
        // Find next newline after start
        int cut = content.indexOf('\n', start);
        if (cut < 0 || cut >= content.length()) cut = content.length() - 1;
        String kept = HEADER + System.lineSeparator() + content.substring(cut + 1);
        Files.writeString(LOG_PATH, kept, StandardCharsets.UTF_8);
        LOGGER.info("Trimmed violation log to {} bytes", kept.length());
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
