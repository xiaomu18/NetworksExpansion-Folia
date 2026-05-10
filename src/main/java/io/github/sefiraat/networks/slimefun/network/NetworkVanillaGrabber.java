package io.github.sefiraat.networks.slimefun.network;

import com.balugaq.netex.api.enums.FeedbackType;
import com.balugaq.netex.api.enums.MinecraftVersion;
import com.balugaq.netex.api.interfaces.SoftCellBannable;
import com.balugaq.netex.utils.InventoryUtil;
import com.balugaq.netex.utils.Lang;
import com.bgsoftware.wildchests.api.WildChestsAPI;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"DuplicatedCode", "GrazieInspection"})
public class NetworkVanillaGrabber extends NetworkDirectional implements SoftCellBannable {

    private static final int[] BACKGROUND_SLOTS = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 15, 16, 17, 18, 20, 22, 23, 24, 26, 27, 28, 30, 31, 33, 34, 35, 36,
        37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int OUTPUT_SLOT = 25;
    private static final int NORTH_SLOT = 11;
    private static final int SOUTH_SLOT = 29;
    private static final int EAST_SLOT = 21;
    private static final int WEST_SLOT = 19;
    private static final int UP_SLOT = 14;
    private static final int DOWN_SLOT = 32;
    private static final Set<org.bukkit.Location> PENDING_GRABS = ConcurrentHashMap.newKeySet();

    public NetworkVanillaGrabber(
        @NotNull ItemGroup itemGroup,
        @NotNull SlimefunItemStack item,
        @NotNull RecipeType recipeType,
        ItemStack @NotNull [] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.GRABBER);
        this.getSlotsToDrop().add(OUTPUT_SLOT);
    }

    @Override
    protected void onTick(@Nullable BlockMenu blockMenu, @NotNull Block block) {
        super.onTick(blockMenu, block);
        if (blockMenu != null) {
            tryGrabItem(blockMenu);
        }
    }

    @SuppressWarnings("removal")
    private void tryGrabItem(@NotNull BlockMenu blockMenu) {

        final ItemStack itemInSlot = blockMenu.getItemInSlot(OUTPUT_SLOT);

        if (itemInSlot != null && itemInSlot.getType() != Material.AIR) {
            sendFeedback(blockMenu.getLocation(), FeedbackType.ALREADY_HAS_ITEM);
            return;
        }

        final NodeDefinition definition = NetworkStorage.getNode(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            sendFeedback(blockMenu.getLocation(), FeedbackType.NO_NETWORK_FOUND);
            return;
        }

        if (checkSoftCellBan(blockMenu.getLocation(), definition.getNode().getRoot())) {
            return;
        }

        final BlockFace direction = getCurrentDirection(blockMenu);
        final Block block = blockMenu.getBlock();
        final Block targetBlock = block.getRelative(direction);
        final org.bukkit.Location menuLocation = blockMenu.getLocation();
        final org.bukkit.Location targetLocation = targetBlock.getLocation();

        /* Netex - #293
        // No longer check permission
        // Fix for early vanilla pusher release
        final String ownerUUID = StorageCacheUtils.getData(block.getLocation(), OWNER_KEY);
        if (ownerUUID == null) {
            sendFeedback(block.getLocation(), FeedbackType.NO_OWNER_FOUND);
            return;
        }
        final UUID uuid = UUID.fromString(ownerUUID);
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        // dirty fix
        try {
            if (!Slimefun.getProtectionManager()
                .hasPermission(offlinePlayer, targetBlock, Interaction.INTERACT_BLOCK)) {
                sendFeedback(block.getLocation(), FeedbackType.NO_PERMISSION);
                return;
            }
        } catch (NullPointerException ex) {
            sendFeedback(block.getLocation(), FeedbackType.ERROR_OCCURRED);
            return;
        }
        */
        if (!PENDING_GRABS.add(menuLocation)) {
            return;
        }

        FoliaSupport.runRegion(targetLocation, () -> {
            // Netex start - #287
            if (StorageCacheUtils.getMenu(targetLocation) != null) {
                FoliaSupport.runRegion(menuLocation, () -> PENDING_GRABS.remove(menuLocation));
                return;
            }
            // Netex end - #287

            final BlockState blockState = PaperLib.getBlockState(targetLocation.getBlock(), false).getState();

            if (!(blockState instanceof InventoryHolder holder)) {
                FoliaSupport.runRegion(menuLocation, () -> {
                    PENDING_GRABS.remove(menuLocation);
                    sendFeedback(menuLocation, FeedbackType.NO_INVENTORY_FOUND);
                });
                return;
            }

            boolean wildChests = Networks.getSupportedPluginManager().isWildChests();
            boolean isChest = wildChests && WildChestsAPI.getChest(targetLocation) != null;

            sendDebugMessage(menuLocation, String.format(Lang.getString("messages.debug.wildchests"), wildChests));
            sendDebugMessage(menuLocation, String.format(Lang.getString("messages.debug.ischest"), isChest));

            if (wildChests && isChest) {
                sendDebugMessage(menuLocation, Lang.getString("messages.debug.wildchests-trigger-failed"));
                FoliaSupport.runRegion(menuLocation, () -> {
                    PENDING_GRABS.remove(menuLocation);
                    sendFeedback(menuLocation, FeedbackType.PROTECTED_BLOCK);
                });
                return;
            }

            sendDebugMessage(menuLocation, Lang.getString("messages.debug.wildchests-trigger-success"));
            final Inventory inventory = holder.getInventory();
            final GrabResult result;

            if (inventory instanceof FurnaceInventory furnaceInventory) {
                result = tryTakeFromFurnace(furnaceInventory);
            } else if (inventory instanceof BrewerInventory brewerInventory) {
                if (!(blockState instanceof BrewingStand brewingStand) || brewingStand.getBrewingTime() > 0) {
                    FoliaSupport.runRegion(menuLocation, () -> PENDING_GRABS.remove(menuLocation));
                    return;
                }
                result = tryTakeFromBrewer(brewerInventory);
            } else {
                result = tryTakeFromInventory(inventory);
            }

            if (result == null) {
                FoliaSupport.runRegion(menuLocation, () -> PENDING_GRABS.remove(menuLocation));
                return;
            }

            FoliaSupport.runRegion(menuLocation, () -> {
                final ItemStack liveOutput = blockMenu.getItemInSlot(OUTPUT_SLOT);
                if (liveOutput == null || liveOutput.getType() == Material.AIR) {
                    blockMenu.replaceExistingItem(OUTPUT_SLOT, result.itemStack());
                    PENDING_GRABS.remove(menuLocation);
                    sendFeedback(menuLocation, FeedbackType.WORKING);
                } else {
                    FoliaSupport.runRegion(targetLocation, result.restore());
                    PENDING_GRABS.remove(menuLocation);
                }
            });
        });
    }

    private @Nullable GrabResult tryTakeFromFurnace(@NotNull FurnaceInventory furnaceInventory) {
        final ItemStack result = furnaceInventory.getResult();
        if (result != null && result.getType() != Material.AIR) {
            final ItemStack taken = result.clone();
            furnaceInventory.setResult(null);
            return new GrabResult(taken, () -> {
                if (furnaceInventory.getResult() == null || furnaceInventory.getResult().getType() == Material.AIR) {
                    furnaceInventory.setResult(taken.clone());
                } else {
                    InventoryUtil.addItem(furnaceInventory, taken.clone());
                }
            });
        }

        final ItemStack fuel = furnaceInventory.getFuel();
        if (fuel != null && fuel.getType() == Material.BUCKET) {
            final ItemStack taken = fuel.clone();
            furnaceInventory.setFuel(null);
            return new GrabResult(taken, () -> {
                if (furnaceInventory.getFuel() == null || furnaceInventory.getFuel().getType() == Material.AIR) {
                    furnaceInventory.setFuel(taken.clone());
                } else {
                    InventoryUtil.addItem(furnaceInventory, taken.clone());
                }
            });
        }

        return null;
    }

    private @Nullable GrabResult tryTakeFromBrewer(@NotNull BrewerInventory brewerInventory) {
        for (int i = 0; i < 3; i++) {
            final ItemStack stack = brewerInventory.getContents()[i];
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            if (stack.getItemMeta() instanceof PotionMeta potionMeta) {
                if (Networks.getInstance().getMCVersion().isAtLeast(MinecraftVersion.V1_20_5)) {
                    if (potionMeta.getBasePotionType() == PotionType.WATER) {
                        continue;
                    }
                } else {
                    PotionData bpd = potionMeta.getBasePotionData();
                    if (bpd == null || bpd.getType() == PotionType.WATER) {
                        continue;
                    }
                }
            }

            final int slot = i;
            final ItemStack taken = stack.clone();
            brewerInventory.setItem(slot, null);
            return new GrabResult(taken, () -> {
                final ItemStack live = brewerInventory.getItem(slot);
                if (live == null || live.getType() == Material.AIR) {
                    brewerInventory.setItem(slot, taken.clone());
                } else {
                    InventoryUtil.addItem(brewerInventory, taken.clone());
                }
            });
        }

        return null;
    }

    private @Nullable GrabResult tryTakeFromInventory(@NotNull Inventory inventory) {
        final ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            final ItemStack stack = contents[i];
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            final int slot = i;
            final ItemStack taken = stack.clone();
            inventory.setItem(slot, null);
            return new GrabResult(taken, () -> {
                final ItemStack live = inventory.getItem(slot);
                if (live == null || live.getType() == Material.AIR) {
                    inventory.setItem(slot, taken.clone());
                } else {
                    InventoryUtil.addItem(inventory, taken.clone());
                }
            });
        }

        return null;
    }

    private record GrabResult(@NotNull ItemStack itemStack, @NotNull Runnable restore) {
    }

    @Override
    protected int @NotNull [] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }

    @Override
    public int getNorthSlot() {
        return NORTH_SLOT;
    }

    @Override
    public int getSouthSlot() {
        return SOUTH_SLOT;
    }

    @Override
    public int getEastSlot() {
        return EAST_SLOT;
    }

    @Override
    public int getWestSlot() {
        return WEST_SLOT;
    }

    @Override
    public int getUpSlot() {
        return UP_SLOT;
    }

    @Override
    public int getDownSlot() {
        return DOWN_SLOT;
    }

    @Override
    public boolean runSync() {
        return true;
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    protected Particle.@NotNull DustOptions getDustOptions() {
        return new Particle.DustOptions(Color.MAROON, 1);
    }
}
