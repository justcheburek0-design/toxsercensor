package ru.vadim.toxsercensor.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.vadim.toxsercensor.command.ConfigCommands;

@Mixin(Commands.class)
public abstract class CommandManagerMixin {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>(Lnet/minecraft/commands/Commands$CommandSelection;Lnet/minecraft/commands/CommandBuildContext;)V", at = @At("TAIL"))
    private void toxsercensor$registerCommands(Commands.CommandSelection environment, CommandBuildContext commandBuildContext, CallbackInfo ci) {
        ConfigCommands.register(this.dispatcher);
    }
}
