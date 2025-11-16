package com.github.thesilentpro.headdb.core.command.sub;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.github.thesilentpro.headdb.core.HeadDB;
import com.github.thesilentpro.headdb.core.command.HDBSubCommand;
import com.github.thesilentpro.headdb.core.menu.gui.HeadsGUI;
import com.github.thesilentpro.headdb.core.util.Compatibility;

public class HDBCommandOpen extends HDBSubCommand {

    private final HeadDB plugin;
    private List<String> completions;

    public HDBCommandOpen(HeadDB plugin) {
        super("open", "Open the database.", "[category] [player]", "o");
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        // Определяем целевого игрока
        Player targetPlayer = null;
        int categoryEndIndex = args.length;
        
        // Проверяем, указан ли игрок в конце команды
        if (args.length >= 2) {
            Player possiblePlayer = plugin.getServer().getPlayer(args[args.length - 1]);
            if (possiblePlayer != null) {
                targetPlayer = possiblePlayer;
                categoryEndIndex = args.length - 1;
                
                // Проверка прав на открытие меню другому игроку
                if (!sender.hasPermission("headdb.command.open.others")) {
                    plugin.getLocalization().sendMessage(sender, "noPermission");
                    if (sender instanceof Player) {
                        Compatibility.playSound(sender, plugin.getSoundConfig().get("noPermission"));
                    }
                    return;
                }
            }
        }
        
        // Если целевой игрок не указан, используем отправителя команды
        if (targetPlayer == null) {
            if (!(sender instanceof Player)) {
                plugin.getLocalization().sendMessage(sender, "noConsole");
                return;
            }
            targetPlayer = (Player) sender;
        }
        
        final Player finalTarget = targetPlayer;
        
        // Открытие главного меню
        if (categoryEndIndex == 1) {
            this.plugin.getMenuManager().getMainMenu().open(finalTarget);
            if (finalTarget.equals(sender)) {
                plugin.getLocalization().sendMessage(sender, "command.open.opening");
            } else {
                plugin.getLocalization().sendMessage(sender, "command.open.openingFor", 
                    msg -> msg.replaceText(builder -> builder.matchLiteral("{menu}").replacement("Main"))
                              .replaceText(builder -> builder.matchLiteral("{target}").replacement(finalTarget.getName())));
            }
            Compatibility.playSound(finalTarget, plugin.getSoundConfig().get("menu.open"));
            return;
        }

        // Открытие категории
        final String categoryName = String.join(" ", Arrays.copyOfRange(args, 1, categoryEndIndex));
        HeadsGUI categoryGui = plugin.getMenuManager().get(categoryName.replace(" ", "_").replace("&", "_"));
        if (categoryGui == null) {
            plugin.getLocalization().sendMessage(sender, "command.open.invalidCategory", 
                msg -> msg.replaceText(builder -> builder.matchLiteral("{category}").replacement(categoryName)));
            if (sender instanceof Player) {
                Compatibility.playSound(sender, plugin.getSoundConfig().get("failed"));
            }
            return;
        }

        int pageIndex = 0;
        if (plugin.getCfg().isTrackPage()) {
            pageIndex = categoryGui.getGuiRegistry().getCurrentPage(finalTarget.getUniqueId(), categoryGui.getKey()).orElse(0);
        }
        categoryGui.open(finalTarget, pageIndex);
        
        if (finalTarget.equals(sender)) {
            plugin.getLocalization().sendMessage(sender, "command.open.opening");
        } else {
            plugin.getLocalization().sendMessage(sender, "command.open.openingFor", 
                msg -> msg.replaceText(builder -> builder.matchLiteral("{menu}").replacement(categoryName))
                          .replaceText(builder -> builder.matchLiteral("{target}").replacement(finalTarget.getName())));
        }
        Compatibility.playSound(finalTarget, plugin.getSoundConfig().get("menu.open"));
    }

    @Override
    public @Nullable List<String> handleCompletions(CommandSender sender, String[] args) {
        if (completions == null) {
            completions = plugin.getHeadApi().findKnownCategories();
        }
        
        // Если это последний аргумент и у отправителя есть права, предлагаем имена игроков
        if (args.length >= 2 && sender.hasPermission("headdb.command.open.others")) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .toList();
        }
        
        return completions;
    }

}
