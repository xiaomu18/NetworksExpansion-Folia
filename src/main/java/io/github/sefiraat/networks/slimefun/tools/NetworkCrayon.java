package io.github.sefiraat.networks.slimefun.tools;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NetworkCrayon extends SpecialSlimefunItem {
    private static boolean canDirectlyAccess(@NotNull Block block) {
        return block.getWorld() == null || FoliaSupport.isOwnedByCurrentRegion(block.getLocation());
    }

    private static void runAtTargetRegion(@NotNull Block block, @NotNull Runnable runnable) {
        if (canDirectlyAccess(block)) {
            runnable.run();
        } else {
            FoliaSupport.runRegion(block.getLocation(), runnable);
        }
    }

    private static void sendPlayerMessage(@NotNull Player player, @NotNull String message) {
        FoliaSupport.runPlayer(player, () -> player.sendMessage(message));
    }

    public NetworkCrayon(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
        addItemHandler((ItemUseHandler) e -> {
            final Optional<Block> optional = e.getClickedBlock();
            if (optional.isPresent()) {
                final Block block = optional.get();
                final Player player = e.getPlayer();
                e.cancel();
                runAtTargetRegion(block, () -> {
                    final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(block.getLocation());
                    if (slimefunItem instanceof NetworkController) {
                        toggleCrayon(block, player);
                    } else {
                        sendPlayerMessage(player, "§c目标方块不是网络控制器");
                    }
                });
            }
        });
    }

    public void toggleCrayon(@NotNull Block block, @NotNull Player player) {
        if (NetworkController.hasCrayon(block.getLocation())) {
            NetworkController.removeCrayon(block.getLocation());
            player.sendMessage(Lang.getString("messages.completed-operation.crayon.disabled"));
        } else {
            NetworkController.addCrayon(block.getLocation());
            player.sendMessage(Lang.getString("messages.completed-operation.crayon.enabled"));
        }
    }
}
