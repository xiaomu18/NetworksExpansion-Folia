package com.ytdd9527.networksexpansion.implementation.tools;

import com.balugaq.netex.utils.Lang;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import io.github.sefiraat.networks.commands.NetworksMain;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Deprecated
public class NetworksExpansionWorldEditAxe extends SpecialSlimefunItem {

    public NetworksExpansionWorldEditAxe(@NotNull ItemGroup itemGroup, @NotNull SlimefunItemStack item) {
        super(itemGroup, item, RecipeType.NULL, new ItemStack[]{});
        addItemHandler(
            (ItemUseHandler) e -> {
                final Player player = e.getPlayer();
                if (!player.isOp()) {
                    player.sendMessage(
                        Lang.getString("messages.unsupported-operation.comprehensive.no_permission"));
                    return;
                }
                NetworksMain.sendWorldEditDisabledMessage(player);
                e.cancel();
            },
            (ToolUseHandler) (e, t, f, d) -> {
                final Player player = e.getPlayer();
                if (!player.isOp()) {
                    player.sendMessage(Lang.getString("messages.unsupported-operation.no_permission"));
                    return;
                }
                NetworksMain.sendWorldEditDisabledMessage(player);
                e.setCancelled(true);
            });
    }
}
