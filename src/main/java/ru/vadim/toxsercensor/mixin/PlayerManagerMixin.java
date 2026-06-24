package ru.vadim.toxsercensor.mixin;

import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.vadim.toxsercensor.config.FilterConfigManager;
import ru.vadim.toxsercensor.filter.ChatSanitizer;
import ru.vadim.toxsercensor.filter.FilterLogger;
import ru.vadim.toxsercensor.filter.ViolationTracker;

import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {
    @ModifyVariable(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private PlayerChatMessage toxsercensor$filterChatMessage(PlayerChatMessage message, ServerPlayer player) {
        // Check whitelist via message sender UUID
        UUID senderUuid = null;
        if (message.link() != null && message.link().sender() != null) {
            senderUuid = message.link().sender();
            if (FilterConfigManager.isWhitelisted(senderUuid.toString())) {
                return message;
            }
        }

        String original = message.signedBody().content();
        String filtered = ChatSanitizer.sanitize(original, FilterConfigManager.get());
        if (filtered.equals(original)) {
            return message;
        }

        // Log violation
        String playerName = player.getScoreboardName();
        String uuidStr = senderUuid != null ? senderUuid.toString() : "?";
        FilterLogger.log(playerName, uuidStr, original, filtered);

        // Track for auto-mute
        if (senderUuid != null) {
            ViolationTracker.getInstance().recordViolation(senderUuid);
        }

        return PlayerChatMessage.unsigned(message.link().sender(), filtered);
    }
}
