package ru.vadim.toxsercensor.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.vadim.toxsercensor.config.FilterConfigManager;
import ru.vadim.toxsercensor.filter.ChatSanitizer;

@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {
    @ModifyVariable(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private PlayerChatMessage toxsercensor$filterChatMessage(PlayerChatMessage message) {
        String original = message.signedBody().content();
        String filtered = ChatSanitizer.sanitize(original, FilterConfigManager.get());
        if (filtered.equals(original)) {
            return message;
        }
        return PlayerChatMessage.unsigned(message.link().sender(), filtered);
    }
}
