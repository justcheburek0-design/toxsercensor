package ru.vadim.toxsercensor;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vadim.toxsercensor.config.FilterConfigManager;

public final class ToxserCensorMod implements ModInitializer {
    public static final String MOD_ID = "toxsercensor";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        FilterConfigManager.load();
        LOGGER.info(
                "Loaded chat filter config: {} banwords, {} partial words",
                FilterConfigManager.get().banwords.size(),
                FilterConfigManager.get().partialWords.size()
        );
    }
}
