package ru.vadim.toxsercensor.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vadim.toxsercensor.config.FilterConfigManager;
import ru.vadim.toxsercensor.filter.ChatSanitizer;

/**
 * Filters item names typed in anvil rename field.
 * Access the player via @Shadow (inherited from AbstractContainerMenu).
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow
    private Player player;

    @Shadow
    private String itemName;

    @Inject(method = "setItemName", at = @At("HEAD"), cancellable = true)
    private void toxsercensor$filterAnvilName(String newName, CallbackInfo ci) {
        if (newName == null || newName.isEmpty()) return;

        String trimmed = newName.trim();
        if (trimmed.isEmpty()) return;

        // Check whitelist
        if (player instanceof ServerPlayer sp) {
            if (FilterConfigManager.isWhitelisted(sp.getUUID().toString())) {
                return;
            }
        }

        String filtered = ChatSanitizer.sanitize(trimmed, FilterConfigManager.get());
        if (!filtered.equals(trimmed)) {
            this.itemName = filtered;
            ci.cancel();
        }
    }
}
