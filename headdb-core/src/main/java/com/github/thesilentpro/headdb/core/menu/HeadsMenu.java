package com.github.thesilentpro.headdb.core.menu;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import com.github.thesilentpro.grim.button.SimpleButton;
import com.github.thesilentpro.grim.gui.GUI;
import com.github.thesilentpro.grim.page.PaginatedSimplePage;
import com.github.thesilentpro.headdb.api.model.Head;
import com.github.thesilentpro.headdb.core.HeadDB;
import com.github.thesilentpro.headdb.core.factory.ItemFactoryRegistry;
import com.github.thesilentpro.headdb.core.storage.PlayerData;
import com.github.thesilentpro.headdb.core.util.Compatibility;

import net.kyori.adventure.text.Component;

public class HeadsMenu extends PaginatedSimplePage {

    public HeadsMenu(HeadDB plugin, GUI<Integer> gui, Component title, List<Head> heads) {
        super(gui, title, 
            plugin.getMenuConfig().getSize("category", 6),
            plugin.getMenuConfig().getButtonSlot("category", "controls.back", 48),
            plugin.getMenuConfig().getButtonSlot("category", "controls.info", 49),
            plugin.getMenuConfig().getButtonSlot("category", "controls.next", 50)
        );
        preventInteraction();
        for (Head head : heads) {
            addButton(new SimpleButton(head.getItem(), ctx -> {
                Player player = (Player) ctx.event().getWhoClicked();
                if (ctx.event().getClick() == ClickType.DROP) {
                    // TODO: Manage head
                    return;
                }
                if (ctx.event().getClick() == ClickType.RIGHT) {
                    PlayerData playerData = plugin.getPlayerStorage().getPlayer(ctx.event().getWhoClicked().getUniqueId());
                    if (playerData.getFavorites().contains(head.getId())) {
                        playerData.removeFavorite(head.getId());
                        plugin.getLocalization().sendMessage(ctx.event().getWhoClicked(), "menu.favorites.remove", msg -> msg.replaceText(builder -> builder.matchLiteral("{name}").replacement(head.getName())));
                        Compatibility.playSound((Player) ctx.event().getWhoClicked(), plugin.getSoundConfig().get("favorite.remove"));
                    } else {
                        playerData.addFavorite(head.getId());
                        plugin.getLocalization().sendMessage(ctx.event().getWhoClicked(), "menu.favorites.add", msg -> msg.replaceText(builder -> builder.matchLiteral("{name}").replacement(head.getName())));
                        Compatibility.playSound((Player) ctx.event().getWhoClicked(), plugin.getSoundConfig().get("favorite.add"));
                    }
                    return;
                }

                if (plugin.getEconomyProvider() != null) {
                    new PurchaseHeadMenu(plugin, player, head, this).open(player);
                    Compatibility.playSound((Player) ctx.event().getWhoClicked(), plugin.getSoundConfig().get("menu.open"));
                } else {
                    ItemStack item = head.getItem();
                    ItemFactoryRegistry.get().giveItem((Player) ctx.event().getWhoClicked(), plugin.getCfg().getOmit(), plugin.getCfg().isDropOnFullInventory(), item);
                    plugin.getLocalization().sendMessage(ctx.event().getWhoClicked(), "purchase.noEconomy", msg -> msg.replaceText(builder -> builder.matchLiteral("{amount}").replacement(String.valueOf(item.getAmount()))).replaceText(builder -> builder.matchLiteral("{name}").replacement(head.getName())));
                    Compatibility.playSound((Player) ctx.event().getWhoClicked(), plugin.getSoundConfig().get("head.take"));
                }
            }));
        }
        
        // Apply border if enabled in config
        if (plugin.getMenuConfig().isBorderEnabled("category")) {
            Material borderMaterial = plugin.getMenuConfig().getBorderMaterial("category", Material.GRAY_STAINED_GLASS_PANE);
            String borderName = plugin.getMenuConfig().getBorderName("category");
            Component name = borderName.isEmpty() ? Component.text("") : plugin.getMenuConfig().parseComponent(borderName);
            
            ItemStack borderItem = Compatibility.newItem(borderMaterial, name);
            SimpleButton borderButton = new SimpleButton(borderItem);
            
            // Fill empty slots with border
            for (int i = 0; i < getSize(); i++) {
                if (getButton(i).isEmpty()) {
                    setButton(i, borderButton);
                }
            }
        }
        
        reRender();
    }

}