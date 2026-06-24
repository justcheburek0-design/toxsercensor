package ru.vadim.toxsercensor.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vadim.toxsercensor.config.FilterConfigManager;
import ru.vadim.toxsercensor.filter.ChatSanitizer;

import java.util.List;

/**
 * Filters text placed on signs.
 */
@Mixin(SignBlockEntity.class)
public abstract class SignTextMixin {

    @Inject(
            method = "updateText(Lnet/minecraft/server/level/ServerPlayer;ZLjava/util/List;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void toxsercensor$filterSignText(ServerPlayer player, boolean isFront, List<Component> lines, CallbackInfoReturnable<Boolean> cir) {
        // Check if player is whitelisted
        String uuid = player.getUUID().toString();
        if (FilterConfigManager.isWhitelisted(uuid)) {
            return;
        }

        boolean hadViolation = false;
        for (int i = 0; i < lines.size(); i++) {
            String original = lines.get(i).getString();
            if (original.isEmpty()) continue;
            String filtered = ChatSanitizer.sanitize(original, FilterConfigManager.get());
            if (!filtered.equals(original)) {
                hadViolation = true;
                lines.set(i, Component.literal(filtered));
            }
        }

        if (hadViolation) {
            player.sendSystemMessage(Component.literal("§7[ToxserCensor] §eSign text filtered."));
        }
    }
}
