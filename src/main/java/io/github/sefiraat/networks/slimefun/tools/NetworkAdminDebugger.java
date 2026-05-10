package io.github.sefiraat.networks.slimefun.tools;

import com.balugaq.netex.utils.Lang;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.SpecialSlimefunItem;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.slimefun.network.AdminDebuggable;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
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

public class NetworkAdminDebugger extends SpecialSlimefunItem {
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

    public NetworkAdminDebugger(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        addItemHandler((ItemUseHandler) this::onUse);
    }

    protected void onUse(@NotNull PlayerRightClickEvent e) {
        final Optional<Block> optional = e.getClickedBlock();
        if (optional.isPresent()) {
            final Block block = optional.get();
            final Player player = e.getPlayer();
            if (!player.isOp()) {
                player.sendMessage(Lang.getString("messages.unsupported-operation.debugger.player_is_not_op"));
                return;
            }
            e.cancel();
            runAtTargetRegion(block, () -> {
                final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(block.getLocation());
                if (slimefunItem instanceof AdminDebuggable debuggable) {
                    debuggable.toggleDebugMode(block.getLocation(), player);
                } else {
                    sendPlayerMessage(player, "§c目标方块不可调试");
                }
            });
        }
    }
}
