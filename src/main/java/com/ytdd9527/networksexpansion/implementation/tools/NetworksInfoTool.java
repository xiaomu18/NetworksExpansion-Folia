package com.ytdd9527.networksexpansion.implementation.tools;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import com.ytdd9527.networksexpansion.utils.TextUtil;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.slimefun.network.NetworkObject;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NetworksInfoTool extends SpecialSlimefunItem {
    private static boolean canDirectlyAccess(@NotNull Location location) {
        return location.getWorld() == null || FoliaSupport.isOwnedByCurrentRegion(location);
    }

    private static void runAtTargetRegion(@NotNull Location location, @NotNull Runnable runnable) {
        if (canDirectlyAccess(location)) {
            runnable.run();
        } else {
            FoliaSupport.runRegion(location, runnable);
        }
    }

    private static void sendPlayerMessage(@NotNull Player player, @NotNull String message) {
        FoliaSupport.runPlayer(player, () -> player.sendMessage(message));
    }

    public NetworksInfoTool(@NotNull ItemGroup itemGroup, @NotNull SlimefunItemStack item) {
        super(itemGroup, item, RecipeType.NULL, new ItemStack[]{});
        addItemHandler((ItemUseHandler) e -> {
            e.cancel();
            final Player player = e.getPlayer();
            if (!player.isOp()) {
                player.sendMessage(Lang.getString("messages.unsupported-operation.comprehensive.no_permission"));
                return;
            }
            final Optional<Block> optional = e.getClickedBlock();
            if (optional.isPresent()) {
                final Location location = optional.get().getLocation();
                if (player.isSneaking()) {
                    location.add(0, 1, 0);
                }
                runAtTargetRegion(location, () -> {
                    final SlimefunItem sfi = StorageCacheUtils.getSfItem(location);
                    if (sfi instanceof NetworkObject) {
                        final NodeDefinition nodeDefinition = NetworkStorage.getNode(location);
                        if (nodeDefinition == null) {
                            sendPlayerMessage(player, TextUtil.GREEN + "nodeDefinition = null");
                            return;
                        }

                        sendPlayerMessage(player, TextUtil.GREEN + "nodeDefinition.Charge = " + nodeDefinition.getCharge());
                        sendPlayerMessage(player, TextUtil.GREEN + "nodeDefinition.Type = " + nodeDefinition.getType());

                        if (nodeDefinition.getNode() == null) {
                            sendPlayerMessage(player, TextUtil.GREEN + "nodeDefinition.Node = null");
                            return;
                        }

                        sendPlayerMessage(player, TextUtil.GREEN + "nodeDefinition.Node.NodePosition = "
                            + nodeDefinition.getNode().getNodePosition());

                        if (nodeDefinition.getNode().getRoot() == null) {
                            sendPlayerMessage(player, TextUtil.GREEN + "nodeDefinition.Node.Root = null");
                            return;
                        }

                        sendPlayerMessage(player, TextUtil.GREEN + "nodeDefinition.Node.Root.NodePosition = "
                            + nodeDefinition.getNode().getRoot().getNodePosition());
                    } else {
                        sendPlayerMessage(player, TextUtil.RED + "Not a NetworkObject");
                    }
                });
            }
        });
    }
}
