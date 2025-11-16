package com.github.thesilentpro.headdb.core.menu;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import com.github.thesilentpro.grim.button.SimpleButton;
import com.github.thesilentpro.grim.gui.GUI;
import com.github.thesilentpro.grim.page.PaginatedSimplePage;
import com.github.thesilentpro.headdb.core.HeadDB;
import com.github.thesilentpro.headdb.core.config.CustomCategory;
import com.github.thesilentpro.headdb.core.util.Compatibility;

import net.kyori.adventure.text.Component;

public class CustomCategoriesMenu extends PaginatedSimplePage {

    public CustomCategoriesMenu(HeadDB plugin, GUI<Integer> gui, Component title, List<CustomCategory> categories) {
        super(gui, title,
            plugin.getMenuConfig().getSize("custom_categories", 6),
            plugin.getMenuConfig().getButtonSlot("custom_categories", "controls.back", 48),
            plugin.getMenuConfig().getButtonSlot("custom_categories", "controls.info", 49),
            plugin.getMenuConfig().getButtonSlot("custom_categories", "controls.next", 50)
        );
        preventInteraction();
        for (CustomCategory category : categories) {
            addButton(new SimpleButton(category.getIcon(), ctx -> {
                if (ctx.event().getClick() == ClickType.DROP) {
                    // todo: manage head
                    return;
                }

                if (!category.isEnabled()) {
                    Compatibility.playSound((Player) ctx.event().getWhoClicked(), plugin.getSoundConfig().get("menu.disabled"));
                    return;
                }

                if (!ctx.event().getWhoClicked().hasPermission("headdb.category." + category)) {
                    plugin.getLocalization().sendMessage(ctx.event().getWhoClicked(), "noPermission");
                    Compatibility.playSound((Player) ctx.event().getWhoClicked(), plugin.getSoundConfig().get("noPermission"));
                    return;
                }

                plugin.getMenuManager().get(category.getIdentifier()).open((Player) ctx.event().getWhoClicked());
                Compatibility.playSound((Player) ctx.event().getWhoClicked(), plugin.getSoundConfig().get("menu.open"));
            }));
        }
        reRender();
    }

}