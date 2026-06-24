package ru.vadim.toxsercensor;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vadim.toxsercensor.config.FilterConfigManager;
import ru.vadim.toxsercensor.config.FilterConfigWatcher;

public final class ToxserCensorMod implements ModInitializer {
    public static final String MOD_ID = "toxsercensor";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static FilterConfigWatcher configWatcher;

    @Override
    public void onInitialize() {
        FilterConfigManager.load();
        configWatcher = new FilterConfigWatcher();
        LOGGER.info(
                "ToxserCensor initialized: {} banwords, {} partial, {} roots, {} whitelisted",
                FilterConfigManager.get().banwords.size(),
                FilterConfigManager.get().partialWords.size(),
                FilterConfigManager.get().roots.size(),
                FilterConfigManager.get().whitelist.size()
        );
    }

    public static void onShutdown() {
        if (configWatcher != null) {
            configWatcher.close();
        }
    }
}
