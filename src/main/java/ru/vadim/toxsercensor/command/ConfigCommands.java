package ru.vadim.toxsercensor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import ru.vadim.toxsercensor.config.FilterConfigManager;

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
                        .then(listEditor("banwords", true))
                        .then(listEditor("partial", false))));
    }

    private static int create(CommandSourceStack source) {
        boolean created = FilterConfigManager.create();
        if (created) {
            feedback(source, "Конфиг создан: " + FilterConfigManager.getConfigPath());
        } else {
            feedback(source, "Конфиг уже существует: " + FilterConfigManager.getConfigPath());
        }
        return created ? 1 : 0;
    }

    private static int reload(CommandSourceStack source) {
        FilterConfigManager.reload();
        feedback(source, "Конфиг перечитан из файла.");
        return 1;
    }

    private static int remove(CommandSourceStack source) {
        boolean deleted = FilterConfigManager.delete();
        if (deleted) {
            feedback(source, "Конфиг удалён: " + FilterConfigManager.getConfigPath());
        } else {
            feedback(source, "Конфига не было: " + FilterConfigManager.getConfigPath());
        }
        return deleted ? 1 : 0;
    }

    private static int status(CommandSourceStack source) {
        Path path = FilterConfigManager.getConfigPath();
        List<String> banwords = FilterConfigManager.getWords(true);
        List<String> partialWords = FilterConfigManager.getWords(false);
        feedback(source, "Файл: " + path);
        feedback(source, "banwords: " + banwords.size() + " шт.");
        feedback(source, "partialWords: " + partialWords.size() + " шт.");
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> listEditor(String label, boolean banwords) {
        return Commands.literal(label)
                .then(Commands.literal("add")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> addWord(context.getSource(), banwords, StringArgumentType.getString(context, "text")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> removeWord(context.getSource(), banwords, StringArgumentType.getString(context, "text")))))
                .then(Commands.literal("list")
                        .executes(context -> listWords(context.getSource(), banwords)));
    }

    private static int addWord(CommandSourceStack source, boolean banwords, String word) {
        boolean added = FilterConfigManager.addWord(banwords, word);
        if (added) {
            feedback(source, "Добавлено: " + word);
        } else {
            feedback(source, "Уже есть: " + word);
        }
        return added ? 1 : 0;
    }

    private static int removeWord(CommandSourceStack source, boolean banwords, String word) {
        boolean removed = FilterConfigManager.removeWord(banwords, word);
        if (removed) {
            feedback(source, "Удалено: " + word);
        } else {
            feedback(source, "Не найдено: " + word);
        }
        return removed ? 1 : 0;
    }

    private static int listWords(CommandSourceStack source, boolean banwords) {
        List<String> words = FilterConfigManager.getWords(banwords);
        String label = banwords ? "banwords" : "partialWords";
        if (words.isEmpty()) {
            feedback(source, label + ": пусто");
        } else {
            String list = words.stream().collect(Collectors.joining(", "));
            feedback(source, label + " (" + words.size() + "): " + list);
        }
        return 1;
    }

    private static boolean isOperator(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    private static void feedback(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
}
