package me.milaky.hospital.tab;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TabCompliter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("heal");
            completions.add("set");
            completions.add("accept");
            completions.add("decline");
            completions.add("myself");
            completions.add("reload");
            completions.add("callAccept");
            completions.add("call");
            completions.add("radio");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("heal")) {
            // Если первый аргумент - "heal", добавляем список игроков, которые нуждаются в лечении
            String partialName = args[1];
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        }

        // Возвращаем список возможных аргументов для команды
        return completions;
    }
}


