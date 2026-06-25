package ru.vadim.toxsercensor.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vadim.toxsercensor.filter.ViolationTracker;

import java.util.UUID;

/**
 * Prevents muted players from joining the server.
 * MC 26.1.2: placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)
 */
@Mixin(PlayerList.class)
public abstract class PlayerLoginMixin {
    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", at = @At("HEAD"), cancellable = true)
    private void toxsercensor$checkMuted(net.minecraft.network.Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
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
