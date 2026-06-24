package ru.vadim.toxsercensor.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vadim.toxsercensor.filter.ViolationTracker;

import java.util.UUID;

/**
 * Prevents muted players from joining the server.
 */
@Mixin(PlayerList.class)
public abstract class PlayerLoginMixin {
    @Inject(method = "placeNewPlayer", at = @At("HEAD"), cancellable = true)
    private void toxsercensor$checkMuted(ServerGamePacketListenerImpl connection, ServerPlayer player, CallbackInfo ci) {
        UUID uuid = player.getUUID();
        if (ViolationTracker.getInstance().isMuted(uuid)) {
            long remaining = ViolationTracker.getInstance().getRemainingMuteMs(uuid);
            int seconds = (int) (remaining / 1000);
            int minutes = seconds / 60;
            int secs = seconds % 60;
            String msg = "§cВы получили авто-мут за нарушения."
                    + "\n§7Осталось: §e" + minutes + " мин " + secs + " сек";
            connection.disconnect(Component.literal(msg));
            ci.cancel();
        }
    }
}
