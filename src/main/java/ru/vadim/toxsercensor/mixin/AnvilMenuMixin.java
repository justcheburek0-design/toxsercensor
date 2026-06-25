package ru.vadim.toxsercensor.mixin;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.vadim.toxsercensor.config.FilterConfigManager;
import ru.vadim.toxsercensor.filter.ChatSanitizer;

/**
 * Filters item names typed in anvil rename field.
 * MC 26.1.2: setItemName(String) returns boolean.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Shadow
    private String itemName;

    @Inject(method = "setItemName", at = @At("HEAD"), cancellable = true)
    private void toxsercensor$filterAnvilName(String newName, CallbackInfoReturnable<Boolean> cir) {
        if (newName == null || newName.isBlank()) return;

        String filtered = ChatSanitizer.sanitize(newName.trim(), FilterConfigManager.get());
        if (!filtered.equals(newName.trim())) {
            this.itemName = filtered;
            cir.setReturnValue(true);
        }
    }
}
