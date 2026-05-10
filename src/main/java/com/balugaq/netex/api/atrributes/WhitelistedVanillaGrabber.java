package com.balugaq.netex.api.atrributes;

import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.network.NetworkRoot;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface WhitelistedVanillaGrabber extends WhitelistedGrabber {
    default void grabInventory(@NotNull BlockMenu blockMenu, @NotNull BlockState blockState, @NotNull Inventory inventory, @NotNull NetworkRoot root, @NotNull List<ItemStack> templates) {
        final Location sourceLocation = blockState.getLocation();
        if (inventory instanceof FurnaceInventory furnaceInventory) {
            final ItemStack furnaceInventoryResult = furnaceInventory.getResult();
            final ItemStack furnaceInventoryFuel = furnaceInventory.getFuel();
            if (furnaceInventoryResult != null && furnaceInventoryResult.getType() != Material.AIR && inTemplates(templates, furnaceInventoryResult)) {
                grabItem(root, blockMenu, sourceLocation, furnaceInventoryResult);
            } else if (inTemplates(templates, furnaceInventoryFuel)) {
                grabItem(root, blockMenu, sourceLocation, furnaceInventoryFuel);
            }
        } else if (inventory instanceof BrewerInventory brewerInventory) {
            if (!(blockState instanceof BrewingStand brewingStand)) return;
            if (brewingStand.getBrewingTime() > 0) return;

            if (inTemplates(templates, brewerInventory.getFuel())) {
                grabItem(root, blockMenu, sourceLocation, brewerInventory.getFuel());
                return;
            }

            for (int i = 0; i < 3; i++) {
                final ItemStack stack = brewerInventory.getContents()[i];
                if (inTemplates(templates, stack)) {
                    grabItem(root, blockMenu, sourceLocation, stack);
                    break;
                }
            }
        } else {
            for (ItemStack stack : inventory.getContents()) {
                if (inTemplates(templates, stack) && grabItem(root, blockMenu, sourceLocation, stack)) {
                    break;
                }
            }
        }
    }

    default boolean grabItem(
        @NotNull NetworkRoot root,
        @NotNull BlockMenu blockMenu,
        @NotNull Location sourceLocation,
        @Nullable ItemStack stack) {
        if (stack != null && stack.getType() != Material.AIR) {
            final ItemStack transfer = stack.clone();
            final int originalAmount = transfer.getAmount();
            root.addItemStack0Async(blockMenu.getLocation(), transfer).whenComplete((ignored, throwable) ->
                FoliaSupport.runRegion(sourceLocation, () -> {
                    final int moved = Math.max(0, originalAmount - transfer.getAmount());
                    if (moved > 0 && stack.getType() != Material.AIR) {
                        stack.setAmount(Math.max(0, stack.getAmount() - moved));
                    }
                }));
            return true;
        } else {
            return false;
        }
    }
}
