package ru.vadim.toxsercensor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import ru.vadim.toxsercensor.config.FilterConfigManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class ConfigCommands {
    private ConfigCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerCommands(dispatcher);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("toxsercensor")
                .requires(ConfigCommands::isOperator)
                .then(Commands.literal("create").executes(context -> create(context.getSource())))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource())))
                .then(Commands.literal("remove").executes(context -> remove(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("edit")
                        .then(listEditor("banwords", FilterConfigManager::addBanword, FilterConfigManager::removeBanword, FilterConfigManager::getBanwords, FilterConfigManager::clearBanwords, "banwords"))
                        .then(listEditor("partial", FilterConfigManager::addPartialWord, FilterConfigManager::removePartialWord, FilterConfigManager::getPartialWords, FilterConfigManager::clearPartialWords, "partialWords"))
                        .then(listEditor("roots", FilterConfigManager::addRoot, FilterConfigManager::removeRoot, FilterConfigManager::getRoots, FilterConfigManager::clearRoots, "roots"))
                        .then(listEditor("regex", FilterConfigManager::addRegexRoot, FilterConfigManager::removeRegexRoot, FilterConfigManager::getRegexRoots, FilterConfigManager::clearRegexRoots, "regexRoots")))
                .then(Commands.literal("export")
                        .executes(context -> exportConfig(context.getSource())))
                .then(Commands.literal("import")
                        .then(Commands.argument("yaml", StringArgumentType.greedyString())
                                .executes(context -> importConfig(context.getSource(), StringArgumentType.getString(context, "yaml")))))

                .then(Commands.literal("whitelist")
                        .then(Commands.literal("add")
                                .then(Commands.argument("uuid", StringArgumentType.word())
                                        .executes(context -> whitelistAdd(context.getSource(), StringArgumentType.getString(context, "uuid")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("uuid", StringArgumentType.word())
                                        .executes(context -> whitelistRemove(context.getSource(), StringArgumentType.getString(context, "uuid")))))
                        .then(Commands.literal("list").executes(context -> whitelistList(context.getSource())))
                        .then(Commands.literal("clear").executes(context -> whitelistClear(context.getSource()))))
        );
    }

    private static int create(CommandSourceStack source) {
        // attempt to create default config file
        boolean exists = FilterConfigManager.getConfigPath().toFile().exists();
        if (!exists) {
            FilterConfigManager.reload();
            feedback(source, "§aConfig created: " + FilterConfigManager.getConfigPath());
        } else {
            feedback(source, "§eConfig already exists: " + FilterConfigManager.getConfigPath());
        }
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        FilterConfigManager.reload();
        feedback(source, "§aConfig reloaded from file.");
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        boolean deleted = FilterConfigManager.getConfigPath().toFile().delete();
        if (deleted) {
            // Reset to default
            FilterConfigManager.reload();
            feedback(source, "§aConfig deleted, defaults restored.");
        } else {
            feedback(source, "§eNo config file found.");
        }
        return deleted ? 1 : 0;
    }

    private static int status(CommandSourceStack source) {
        Path path = FilterConfigManager.getConfigPath();
        int banwords = FilterConfigManager.getBanwords().size();
        int partialWords = FilterConfigManager.getPartialWords().size();
        int roots = FilterConfigManager.getRoots().size();
        int regexRoots = FilterConfigManager.getRegexRoots().size();
        int whitelist = FilterConfigManager.getWhitelist().size();
        String mode = FilterConfigManager.get().autoMute.enabled ? "§aON" : "§cOFF";

        feedback(source, "§6=== ToxserCensor Status ===");
        feedback(source, "§7File: §f" + path);
        feedback(source, "§7banwords: §f" + banwords + " шт.");
        feedback(source, "§7partialWords: §f" + partialWords + " шт.");
        feedback(source, "§7roots: §f" + roots + " шт.");
        feedback(source, "§7regex: §f" + regexRoots + " шт.");
        feedback(source, "§7whitelist: §f" + whitelist + " шт.");
        feedback(source, "§7autoMute: " + mode);
        return 1;
    }

    // -- Whitelist commands --

    private static int whitelistAdd(CommandSourceStack source, String uuid) {
        String normalized = uuid.toLowerCase().trim();
        if (FilterConfigManager.addWhitelist(normalized)) {
            feedback(source, "§aAdded to whitelist: " + normalized);
        } else {
            feedback(source, "§eAlready in whitelist or invalid: " + normalized);
        }
        return 1;
    }

    private static int whitelistRemove(CommandSourceStack source, String uuid) {
        String normalized = uuid.toLowerCase().trim();
        if (FilterConfigManager.removeWhitelist(normalized)) {
            feedback(source, "§aRemoved from whitelist: " + normalized);
        } else {
            feedback(source, "§eNot found in whitelist: " + normalized);
        }
        return 1;
    }

    private static int whitelistList(CommandSourceStack source) {
        List<String> list = FilterConfigManager.getWhitelist();
        if (list.isEmpty()) {
            feedback(source, "§7whitelist: пусто");
        } else {
            feedback(source, "§6whitelist (" + list.size() + "):");
            for (String uuid : list) {
                feedback(source, " §7- §f" + uuid);
            }
        }
        return 1;
    }

    private static int whitelistClear(CommandSourceStack source) {
        if (FilterConfigManager.clearWhitelist()) {
            feedback(source, "§aWhitelist cleared.");
        } else {
            feedback(source, "§eWhitelist already empty.");
        }
        return 1;
    }

    // -- Export / Import --

    private static int exportConfig(CommandSourceStack source) {
        Path path = FilterConfigManager.getConfigPath();
        try {
            String content = Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
            // Split into lines and send
            String[] lines = content.split("\n");
            feedback(source, "§6=== Config export (" + path.getFileName() + ") ===");
            for (String line : lines) {
                feedback(source, "§7" + line);
            }
        } catch (IOException e) {
            feedback(source, "§cFailed to read config: " + e.getMessage());
        }
        return 1;
    }

    private static int importConfig(CommandSourceStack source, String yaml) {
        Path path = FilterConfigManager.getConfigPath();
        try {
            Files.writeString(path, yaml, java.nio.charset.StandardCharsets.UTF_8);
            FilterConfigManager.reload();
            feedback(source, "§aConfig imported and reloaded (" + yaml.length() + " bytes).");
        } catch (IOException e) {
            feedback(source, "§cFailed to write config: " + e.getMessage());
        }
        return 1;
    }

    // -- List editor --

    private static LiteralArgumentBuilder<CommandSourceStack> listEditor(
            String label,
            Adder adder,
            Remover remover,
            Lister lister,
            Clearer clearer,
            String displayName
    ) {
        return Commands.literal(label)
                .then(Commands.literal("add")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> addWord(context.getSource(), adder, StringArgumentType.getString(context, "text")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> removeWord(context.getSource(), remover, StringArgumentType.getString(context, "text")))))
                .then(Commands.literal("list")
                        .executes(context -> listWords(context.getSource(), lister, displayName)))
                .then(Commands.literal("clear")
                        .executes(context -> clearWords(context.getSource(), clearer, displayName)));
    }

    private static int addWord(CommandSourceStack source, Adder adder, String word) {
        if (adder.add(word)) {
            feedback(source, "§aAdded: " + word);
        } else {
            feedback(source, "§eAlready exists or invalid: " + word);
        }
        return 1;
    }

    private static int removeWord(CommandSourceStack source, Remover remover, String word) {
        if (remover.remove(word)) {
            feedback(source, "§aRemoved: " + word);
        } else {
            feedback(source, "§eNot found: " + word);
        }
        return 1;
    }

    private static int listWords(CommandSourceStack source, Lister lister, String label) {
        List<String> words = lister.list();
        if (words.isEmpty()) {
            feedback(source, "§7" + label + ": пусто");
        } else {
            String list = words.stream().collect(Collectors.joining("§7, §f"));
            feedback(source, "§6" + label + " (§f" + words.size() + "§6): §f" + list);
        }
        return 1;
    }

    private static int clearWords(CommandSourceStack source, Clearer clearer, String label) {
        if (clearer.clear()) {
            feedback(source, "§a" + label + " cleared.");
        } else {
            feedback(source, "§e" + label + " already empty.");
        }
        return 1;
    }

    private static boolean isOperator(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static void feedback(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), true);
    }

    @FunctionalInterface
    interface Adder {
        boolean add(String value);
    }

    @FunctionalInterface
    interface Remover {
        boolean remove(String value);
    }

    @FunctionalInterface
    interface Lister {
        List<String> list();
    }

    @FunctionalInterface
    interface Clearer {
        boolean clear();
    }
}
