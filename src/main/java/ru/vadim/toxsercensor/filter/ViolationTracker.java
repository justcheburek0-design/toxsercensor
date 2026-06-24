package ru.vadim.toxsercensor.filter;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vadim.toxsercensor.ToxserCensorMod;
import ru.vadim.toxsercensor.config.FilterConfigManager;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tracks chat violations and manages auto-mute state.
 * Thread-safe via synchronized + immutable snapshots.
 */
public final class ViolationTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToxserCensorMod.MOD_ID);

    // UUID -> mute expiration (cleaned on access)
    private final Cache<UUID, ViolationWindow> violations = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    // UUID -> muted until
    private final Cache<UUID, Instant> mutedUntil = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private static final ViolationTracker INSTANCE = new ViolationTracker();

    public static ViolationTracker getInstance() {
        return INSTANCE;
    }

    private ViolationTracker() {
    }

    /**
     * Record a violation for the given player UUID.
     * @return mute duration in ms if auto-muted, 0 if not muted, -1 if auto-mute disabled
     */
    public long recordViolation(UUID uuid) {
        var cfg = FilterConfigManager.get().autoMute;
        if (!cfg.enabled) {
            return -1;
        }

        long now = Instant.now().toEpochMilli();
        long windowMs = cfg.windowMinutes * 60_000L;
        long muteMs = cfg.muteDurationMinutes * 60_000L;
        int maxViolations = cfg.maxViolations;

        ViolationWindow wnd = violations.getIfPresent(uuid);
        if (wnd == null) {
            wnd = new ViolationWindow();
            violations.put(uuid, wnd);
        }

        wnd.add(now);
        wnd.prune(now - windowMs);

        if (wnd.count() >= maxViolations) {
            // Mute
            Instant until = Instant.now().plusMillis(muteMs);
            mutedUntil.put(uuid, until);
            violations.invalidate(uuid); // reset counter
            LOGGER.info("Auto-muted {} for {} min", uuid, cfg.muteDurationMinutes);
            return muteMs;
        }

        return 0;
    }

    /** Check if a player is currently muted. */
    public boolean isMuted(UUID uuid) {
        Instant until = mutedUntil.getIfPresent(uuid);
        if (until == null) return false;
        if (Instant.now().isAfter(until)) {
            mutedUntil.invalidate(uuid);
            return false;
        }
        return true;
    }

    /** Get remaining mute time in ms, 0 if not muted. */
    public long getRemainingMuteMs(UUID uuid) {
        Instant until = mutedUntil.getIfPresent(uuid);
        if (until == null) return 0;
        long remaining = until.toEpochMilli() - Instant.now().toEpochMilli();
        if (remaining <= 0) {
            mutedUntil.invalidate(uuid);
            return 0;
        }
        return remaining;
    }

    // -- In-memory violation window --

    private static final class ViolationWindow {
        private static final int MAX_RECORDS = 100;
        private final long[] timestamps = new long[MAX_RECORDS];
        private int size = 0;

        void add(long epochMs) {
            if (size < MAX_RECORDS) {
                timestamps[size++] = epochMs;
            } else {
                // Shift left by 1 (discard oldest)
                System.arraycopy(timestamps, 1, timestamps, 0, MAX_RECORDS - 1);
                timestamps[MAX_RECORDS - 1] = epochMs;
            }
        }

        void prune(long olderThan) {
            int kept = 0;
            for (int i = 0; i < size; i++) {
                if (timestamps[i] >= olderThan) {
                    timestamps[kept++] = timestamps[i];
                }
            }
            size = kept;
        }

        int count() {
            return size;
        }
    }
}
