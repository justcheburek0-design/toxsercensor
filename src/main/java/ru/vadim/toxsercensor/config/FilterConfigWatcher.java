package ru.vadim.toxsercensor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vadim.toxsercensor.ToxserCensorMod;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Background file watcher for auto-reloading the config on external changes.
 * Runs as a daemon thread, checks the config directory for modifications.
 */
public final class FilterConfigWatcher implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToxserCensorMod.MOD_ID);
    private static final long DEBOUNCE_MS = 500;

    private final Path configDir;
    private final String configFileName;
    private final Thread watcherThread;
    private volatile boolean running;

    public FilterConfigWatcher() {
        Path configPath = FilterConfigManager.getConfigPath();
        this.configDir = configPath.getParent();
        this.configFileName = configPath.getFileName().toString();
        this.running = true;

        watcherThread = new Thread(this::run, "toxsercensor-watcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void run() {
        LOGGER.info("File watcher started for {}", configDir.resolve(configFileName));

        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            // Ensure directory exists before registering
            Files.createDirectories(configDir);
            configDir.register(watcher, ENTRY_MODIFY);

            while (running) {
                WatchKey key = watcher.poll(10, TimeUnit.SECONDS);
                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() != ENTRY_MODIFY) continue;

                    Path changed = (Path) event.context();
                    if (!configFileName.equals(changed.toString())) {
                        continue;
                    }

                    // Debounce — skip if we just saved ourselves
                    long lastSave = FilterConfigManager.getLastSaveTime();
                    long now = System.currentTimeMillis();
                    if (now - lastSave < DEBOUNCE_MS) {
                        continue;
                    }

                    // Small extra delay to ensure file is fully written
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    LOGGER.info("Config file changed externally, reloading...");
                    FilterConfigManager.reload();
                }

                if (!key.reset()) {
                    LOGGER.warn("Watch key invalid, stopping file watcher");
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error("File watcher error for {}: {}", configDir, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LOGGER.info("File watcher stopped");
    }

    @Override
    public void close() {
        running = false;
        watcherThread.interrupt();
        try {
            watcherThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
