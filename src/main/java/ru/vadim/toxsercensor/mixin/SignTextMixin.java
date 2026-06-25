package ru.vadim.toxsercensor.mixin;

import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vadim.toxsercensor.config.FilterConfigManager;
import ru.vadim.toxsercensor.filter.ChatSanitizer;

import java.util.List;

/**
 * Filters text placed on signs.
 * MC 26.1.2 signature: updateSignText(Player, boolean, List<FilteredText>)
 */
@Mixin(SignBlockEntity.class)
public abstract class SignTextMixin {

    @Inject(
            method = "updateSignText(Lnet/minecraft/world/entity/player/Player;ZLjava/util/List;)V",
            at = @At("HEAD")
    )
    private void toxsercensor$filterSignText(Player player, boolean isFront, List<FilteredText> lines, CallbackInfo ci) {
        if (player != null && FilterConfigManager.isWhitelisted(player.getUUID().toString())) {
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            FilteredText current = lines.get(i);
            if (current == null) continue;

            String original = current.raw();
            if (original == null || original.isEmpty()) continue;

            String filtered = ChatSanitizer.sanitize(original, FilterConfigManager.get());
            if (!filtered.equals(original)) {
                lines.set(i, FilteredText.passThrough(filtered));
            }
        }
    }
}
