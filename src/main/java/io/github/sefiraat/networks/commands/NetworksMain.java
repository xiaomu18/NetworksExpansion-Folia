package io.github.sefiraat.networks.commands;

import com.balugaq.netex.api.algorithm.Calculator;
import com.balugaq.netex.api.data.ItemContainer;
import com.balugaq.netex.api.data.StorageUnitData;
import com.balugaq.netex.api.enums.ErrorType;
import com.balugaq.netex.utils.InventoryUtil;
import com.balugaq.netex.utils.Lang;
import com.balugaq.netex.utils.MapUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.core.items.unusable.AbstractBlueprint;
import com.ytdd9527.networksexpansion.implementation.machines.unit.NetworksDrawer;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import com.ytdd9527.networksexpansion.utils.ParticleUtil;
import com.ytdd9527.networksexpansion.utils.WorldUtils;
import io.github.bakedlibs.dough.collections.Pair;
import io.github.bakedlibs.dough.skins.PlayerHead;
import io.github.bakedlibs.dough.skins.PlayerSkin;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.stackcaches.BlueprintInstance;
import io.github.sefiraat.networks.network.stackcaches.ItemRequest;
import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.slimefun.NetworksSlimefunItemStacks;
import io.github.sefiraat.networks.slimefun.network.AdminDebuggable;
import io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.StackUtils;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentCraftingBlueprintType;
import io.github.sefiraat.networks.utils.datatypes.PersistentQuantumStorageType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.ChunkPosition;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapView;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation", "DuplicatedCode"})
public class NetworksMain implements TabExecutor {
    private static final String FOLIA_WORLDEDIT_DISABLED_MESSAGE =
        ChatColor.RED + "Folia builds currently disable /networks worldedit* features.";
    private static final String FOLIA_CROSS_REGION_BLOCK_MESSAGE =
        ChatColor.RED + "Folia builds cannot directly inspect or modify a target block in another region.";

    @Deprecated
    private static final Set<UUID> requesters = new ConcurrentSkipListSet<>();

    @Deprecated
    private static final Networks javaPlugin = Networks.getInstance();

    @Deprecated
    private static final Map<UUID, Pair<Location, Location>> SELECTED_POS = new ConcurrentHashMap<>();

    public NetworksMain() {
    }

    public static void sendWorldEditDisabledMessage(@NotNull Player player) {
        player.sendMessage(FOLIA_WORLDEDIT_DISABLED_MESSAGE);
    }

    private static boolean canDirectlyAccess(@NotNull Location location) {
        return location.getWorld() == null || FoliaSupport.isOwnedByCurrentRegion(location);
    }

    private static boolean ensureDirectBlockAccess(@NotNull Player player, @NotNull Location location) {
        if (canDirectlyAccess(location)) {
            return true;
        }

        player.sendMessage(FOLIA_CROSS_REGION_BLOCK_MESSAGE);
        return false;
    }

    @Deprecated
    public static @Nullable Location getPos1(@NotNull Player p) {
        if (SELECTED_POS.get(p.getUniqueId()) == null) {
            return null;
        }

        return SELECTED_POS.get(p.getUniqueId()).getFirstValue();
    }

    @Deprecated
    public static @Nullable Location getPos2(@NotNull Player p) {
        if (SELECTED_POS.get(p.getUniqueId()) == null) {
            return null;
        }
        return SELECTED_POS.get(p.getUniqueId()).getSecondValue();
    }

    @Deprecated
    public static void setPos1(@NotNull Player p, Location pos) {
        SELECTED_POS.put(p.getUniqueId(), new Pair<>(pos, getPos2(p)));
    }

    @Deprecated
    public static void setPos2(@NotNull Player p, Location pos) {
        SELECTED_POS.put(p.getUniqueId(), new Pair<>(getPos1(p), pos));
    }

    @Deprecated
    public static void clearPos(@NotNull Player p) {
        SELECTED_POS.remove(p.getUniqueId());
        p.sendMessage(Lang.getString("messages.commands.clear-selected-pos"));
    }

    @Deprecated
    public static void toggleShowSelectedAreaOutline(@NotNull Player p) {
        sendWorldEditDisabledMessage(p);
    }

    @Deprecated
    private static void handleSelectedAreaOutlineShowRequest(@NotNull Player p) {
    }

    @Deprecated
    public static @NotNull String locationToString(@Nullable Location l) {
        if (l == null) {
            return Lang.getString("icons.drawer.location_error.unknown");
        }
        if (l.getWorld() == null) {
            return Lang.getString("icons.drawer.location_error.unknown");
        }
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    @Deprecated
    public static long locationRange(@Nullable Location pos1, @Nullable Location pos2) {
        if (pos1 == null || pos2 == null) {
            return 0;
        }

        final int downX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        final int upX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        final int downY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        final int upY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        final int downZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        final int upZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        return (long) (Math.abs(upX - downX) + 1) * (Math.abs(upY - downY) + 1) * (Math.abs(upZ - downZ) + 1);
    }

    @Deprecated
    private static void doWorldEdit(
        @Nullable Location pos1, @Nullable Location pos2, @NotNull Consumer<Location> consumer) {
        if (pos1 == null || pos2 == null) {
            return;
        }
        final int downX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        final int upX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        final int downY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        final int upY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        final int downZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        final int upZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        for (int x = downX; x <= upX; x++) {
            for (int y = downY; y <= upY; y++) {
                for (int z = downZ; z <= upZ; z++) {
                    consumer.accept(new Location(pos1.getWorld(), x, y, z));
                }
            }
        }
    }

    public static void viewLog(@NotNull Player player) {
        final Block targetBlock = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-admin-debuggable"));
            return;
        }
        if (!ensureDirectBlockAccess(player, targetBlock.getLocation())) {
            return;
        }

        final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(targetBlock.getLocation());
        if (slimefunItem == null) {
            player.sendMessage(Lang.getString("messages.commands.must-admin-debuggable"));
            return;
        }

        if (!(slimefunItem instanceof AdminDebuggable debuggable)) {
            player.sendMessage(Lang.getString("messages.commands.must-admin-debuggable"));
            return;
        }

        if (debuggable.hasViewer(player)) {
            debuggable.removeViewer(player);
            player.sendMessage(Lang.getString("messages.commands.viewer-removed"));
        } else {
            debuggable.addViewer(player);
            player.sendMessage(Lang.getString("messages.commands.viewer-added"));
        }
    }

    public static void setQuantum(@NotNull Player player, long amount) {
        final Block targetBlock = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-quantum-storage"));
            return;
        }
        if (!ensureDirectBlockAccess(player, targetBlock.getLocation())) {
            return;
        }

        final SlimefunBlockData blockData = StorageCacheUtils.getBlock(targetBlock.getLocation());
        if (blockData == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-quantum-storage"));
            return;
        }

        final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(targetBlock.getLocation());
        if (slimefunItem == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-quantum-storage"));
            return;
        }

        final Location targetLocation = targetBlock.getLocation();
        if (!(slimefunItem instanceof NetworkQuantumStorage)) {
            player.sendMessage(Lang.getString("messages.commands.invalid-quantum-storage"));
            return;
        }

        final BlockMenu blockMenu = StorageCacheUtils.getMenu(targetLocation);
        if (blockMenu == null) {
            player.sendMessage(Lang.getString("messages.commands.invalid-quantum-storage"));
            return;
        }

        final QuantumCache cache = NetworkQuantumStorage.getCaches().get(blockMenu.getLocation());
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if ((cache.getItemStack() == null || cache.getItemStack().getType() == Material.AIR)
            && itemInHand.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-hand-item"));
            return;
        }
        final ItemStack clone = (itemInHand.getType() == Material.AIR ? cache.getItemStack() : itemInHand).clone();
        NetworkQuantumStorage.setItem(blockMenu, clone, amount);

        clone.setAmount(1);
        cache.setItemStack(clone);
        cache.setAmount(Math.min(amount, cache.getLimitLong()));
        NetworkQuantumStorage.updateDisplayItem(blockMenu, cache);
        NetworkQuantumStorage.syncBlock(blockMenu.getLocation(), cache);
        NetworkQuantumStorage.getCaches().put(blockMenu.getLocation(), cache);
    }

    private static void addStorageItem(@NotNull Player player, int amount) {
        final Block targetBlock = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }
        if (!ensureDirectBlockAccess(player, targetBlock.getLocation())) {
            return;
        }

        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-hand-item"));
            return;
        }

        final SlimefunBlockData blockData = StorageCacheUtils.getBlock(targetBlock.getLocation());
        if (blockData == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }

        final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(targetBlock.getLocation());
        if (slimefunItem == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }

        if (!(slimefunItem instanceof NetworksDrawer)) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
        }

        final Location targetLocation = targetBlock.getLocation();
        final ItemStack clone = itemInHand.clone();
        final StorageUnitData data = NetworksDrawer.getStorageData(targetLocation);

        if (data == null) {
            player.sendMessage(Lang.getString("messages.commands.invalid-drawer"));
            return;
        }

        clone.setAmount(amount);
        data.depositItemStack(clone, false);
        NetworksDrawer.setStorageData(targetLocation, data);
        player.sendMessage(Lang.getString("messages.commands.updated-drawer"));
    }

    private static void reduceStorageItem(@NotNull Player player, int amount) {
        final Block targetBlock = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }
        if (!ensureDirectBlockAccess(player, targetBlock.getLocation())) {
            return;
        }

        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-hand-item"));
            return;
        }

        final SlimefunBlockData blockData = StorageCacheUtils.getBlock(targetBlock.getLocation());
        if (blockData == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }

        final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(targetBlock.getLocation());
        if (slimefunItem == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }

        if (!(slimefunItem instanceof NetworksDrawer)) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
        }

        final Location targetLocation = targetBlock.getLocation();
        final ItemStack clone = itemInHand.clone();
        final StorageUnitData data = NetworksDrawer.getStorageData(targetLocation);

        if (data == null) {
            player.sendMessage(Lang.getString("messages.commands.invalid-drawer"));
            return;
        }

        clone.setAmount(1);
        data.requestItem(new ItemRequest(clone, amount));
        NetworksDrawer.setStorageData(targetLocation, data);
        player.sendMessage(Lang.getString("messages.commands.updated-drawer"));
    }

    public static void setContainerId(@NotNull Player player, int containerId) {
        final Block targetBlock = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }
        if (!ensureDirectBlockAccess(player, targetBlock.getLocation())) {
            return;
        }

        final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(targetBlock.getLocation());
        if (slimefunItem == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }

        if (!(slimefunItem instanceof NetworksDrawer)) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }

        final Location location = targetBlock.getLocation();

        player.sendMessage(Lang.getString("messages.commands.wait-for-data"));
        NetworksDrawer.requestData(location, containerId);
        player.sendMessage(String.format(
            Lang.getString("messages.commands.set-container-id"), locationToString(location), containerId));
    }

    @Deprecated
    public static void worldeditPos1(@NotNull Player player) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditPos1(@NotNull Player player, Location location) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditPos2(@NotNull Player player) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditPos2(@NotNull Player player, Location location) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditClone(@NotNull Player player) {
        worldeditClone(player, false);
    }

    @Deprecated
    @SuppressWarnings("UnstableApiUsage")
    public static void worldeditClone(@NotNull Player player, boolean overrideData) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditPaste(@NotNull Player player, @NotNull String sfid) {
        worldeditPaste(player, sfid, false, false);
    }

    @Deprecated
    public static void worldeditPaste(@NotNull Player player, @NotNull String sfid, boolean overrideData) {
        worldeditPaste(player, sfid, overrideData, false);
    }

    @Deprecated
    @SuppressWarnings("UnstableApiUsage")
    public static void worldeditPaste(
        @NotNull Player player, @NotNull String sfid, boolean overrideData, boolean force) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditClear(@NotNull Player player, boolean callHandler, boolean skipVanilla) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditBlockMenuSetSlot(@NotNull Player player, int slot) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditBlockInfoAdd(@NotNull Player player, @NotNull String key, @NotNull String value) {
        sendWorldEditDisabledMessage(player);
    }

    @Deprecated
    public static void worldeditBlockInfoRemove(@NotNull Player player, @NotNull String key) {
        sendWorldEditDisabledMessage(player);
    }

    private static void updateItem(@NotNull Player player) {
        final ItemStack itemInHand = player.getInventory().getItemInMainHand();
        final SlimefunItem slimefunItem = SlimefunItem.getByItem(itemInHand);
        if (slimefunItem == null) {
            player.sendMessage(Lang.getString("messages.commands.not-a-slimefun-item"));
            return;
        }

        final String currentId = slimefunItem.getId();
        if (slimefunItem instanceof NetworksDrawer) {
            player.sendMessage(Lang.getString("messages.commands.cannot-update-cargo-storage-unit"));
        } else if (slimefunItem instanceof NetworkQuantumStorage) {
            final ItemMeta meta = itemInHand.getItemMeta();
            QuantumCache quantumCache =
                DataTypeMethods.getCustom(meta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);

            if (quantumCache == null) {
                quantumCache = DataTypeMethods.getCustom(
                    meta, Keys.QUANTUM_STORAGE_INSTANCE2, PersistentQuantumStorageType.TYPE);
            }

            if (quantumCache == null) {
                quantumCache = DataTypeMethods.getCustom(
                    meta, Keys.QUANTUM_STORAGE_INSTANCE3, PersistentQuantumStorageType.TYPE);
            }

            if (quantumCache == null || quantumCache.getItemStack() == null) {
                SlimefunItem sf = SlimefunItem.getById(currentId);
                if (sf != null) {
                    itemInHand.setItemMeta(sf.getItem().getItemMeta());
                }
                player.sendMessage(Lang.getString("messages.commands.updated-item"));
                return;
            }

            final ItemStack stored = quantumCache.getItemStack();
            final SlimefunItem sfi = SlimefunItem.getByItem(stored);
            if (sfi != null) {
                final String quantumStoredId = sfi.getId();
                SlimefunItem sf = SlimefunItem.getById(quantumStoredId);
                if (sf != null) {
                    stored.setItemMeta(sf.getItem().getItemMeta());
                }
                player.sendMessage(Lang.getString("messages.commands.updated-item-in-quantum-storage"));
            }
            DataTypeMethods.setCustom(
                meta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE, quantumCache);
            quantumCache.updateMetaLore(meta);
            itemInHand.setItemMeta(meta);
            player.sendMessage(Lang.getString("messages.commands.updated-item"));
        } else {
            SlimefunItem sf = SlimefunItem.getById(currentId);
            if (sf != null) {
                itemInHand.setItemMeta(sf.getItem().getItemMeta());
            }
            player.sendMessage(Lang.getString("messages.commands.updated-item"));
        }
    }

    public static void getStorageItem(@NotNull Player player, int slot) {
        final Block targetBlock = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }
        if (!ensureDirectBlockAccess(player, targetBlock.getLocation())) {
            return;
        }

        final SlimefunBlockData blockData = StorageCacheUtils.getBlock(targetBlock.getLocation());
        if (blockData == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }

        final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(targetBlock.getLocation());
        if (slimefunItem == null) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
            return;
        }

        if (!(slimefunItem instanceof NetworksDrawer)) {
            player.sendMessage(Lang.getString("messages.commands.must-look-at-drawer"));
        }

        final Location targetLocation = targetBlock.getLocation();
        final StorageUnitData data = NetworksDrawer.getStorageData(targetLocation);

        if (data == null) {
            player.sendMessage(Lang.getString("messages.commands.invalid-drawer"));
            return;
        }

        final List<ItemContainer> stored = data.getStoredItems();
        if (slot >= stored.size()) {
            player.sendMessage(String.format(Lang.getString("messages.commands.invalid-slot"), stored.size() - 1));
        } else {
            final ItemStack stack = stored.get(slot).getSampleDirectly();
            if (stack == null || stack.getType() == Material.AIR) {
                player.sendMessage(Lang.getString("messages.commands.empty-slot"));
                return;
            }

            InventoryUtil.addItem(player, StackUtils.getAsQuantity(stack, 1));
        }
    }

    public static void help(@NotNull CommandSender sender, @Nullable String mainCommand) {
        if (mainCommand == null) {
            for (String message : Lang.getStringList("messages.commands.help")) {
                sender.sendMessage(message);
            }
            return;
        }
        switch (mainCommand.toLowerCase(Locale.ROOT)) {
            case "help" -> {
                for (String message : Lang.getStringList("messages.commands.example.help")) {
                    sender.sendMessage(message);
                }
            }
            case "fillquantum" -> {
                for (String message : Lang.getStringList("messages.commands.example.fillquantum")) {
                    sender.sendMessage(message);
                }
            }
            case "fixblueprint" -> {
                for (String message : Lang.getStringList("messages.commands.example.fixblueprint")) {
                    sender.sendMessage(message);
                }
            }
            case "addstorageitem" -> {
                for (String message : Lang.getStringList("messages.commands.example.addstorageitem")) {
                    sender.sendMessage(message);
                }
            }
            case "reducestorageitem" -> {
                for (String message : Lang.getStringList("messages.commands.example.reducestorageitem")) {
                    sender.sendMessage(message);
                }
            }
            case "setquantum" -> {
                for (String message : Lang.getStringList("messages.commands.example.setquantum")) {
                    sender.sendMessage(message);
                }
            }
            case "setcontainerid" -> {
                for (String message : Lang.getStringList("messages.commands.example.setcontainerid")) {
                    sender.sendMessage(message);
                }
            }
            case "getstorageitem" -> {
                for (String message : Lang.getStringList("messages.commands.example.getstorageitem")) {
                    sender.sendMessage(message);
                }
            }
            case "worldedit" -> {
                for (String message : Lang.getStringList("messages.commands.example.worldedit")) {
                    sender.sendMessage(message);
                }
            }
            case "updateitem" -> {
                for (String message : Lang.getStringList("messages.commands.example.updateitem")) {
                    sender.sendMessage(message);
                }
            }
            case "viewlog" -> {
                for (String message : Lang.getStringList("messages.commands.example.viewlog")) {
                    sender.sendMessage(message);
                }
            }
            default -> {
                for (String message : Lang.getStringList("messages.commands.example.unknown-command")) {
                    sender.sendMessage(message);
                }
            }
        }
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            help(sender, null);
            return true;
        }
        switch (args[0]) {
            case "fillquantum",
                 "fixblueprint",
                 "addstorageitem",
                 "reducestorageitem",
                 "setquantum",
                 "setcontainerid" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(getErrorMessage(ErrorType.MUST_BE_PLAYER));
                    return false;
                }
            }
            case "help" -> {
            }
        }

        // Player or console
        if (args[0].toLowerCase(Locale.ROOT).equals("help")) {
            if (sender.isOp()) {
                if (args.length >= 2) {
                    help(sender, args[1]);
                } else {
                    help(sender, null);
                }
            } else {
                sender.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
            }
            return true;
        }

        // Player only
        if (sender instanceof Player player) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "fillquantum" -> {
                    if (!player.hasPermission("networks.admin")
                        && !player.hasPermission("networks.commands.fillquantum")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length == 1) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "amount"));
                        return true;
                    }

                    try {
                        long amount = Calculator.calculate(args[1]).longValue();
                        if (amount < 0 || amount > NetworkQuantumStorage.MAX_AMOUNT) {
                            player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "amount"));
                            return true;
                        }
                        fillQuantum(player, amount);
                    } catch (NumberFormatException e) {
                        if ("full".equals(args[1])){
                            fillQuantum(player, NetworkQuantumStorage.MAX_AMOUNT);
                            return true;
                        }
                        player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "amount"));
                        player.sendMessage(e.getMessage());
                    }

                    return true;
                }

                case "fixblueprint" -> {
                    if (!player.hasPermission("networks.admin")
                        && !player.hasPermission("networks.commands.fixblueprint")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length == 1) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "keyInMeta"));
                        return true;
                    }

                    String before = args[1];
                    fixBlueprint(player, before);
                    return true;
                }

                case "setquantum" -> {
                    if (!player.hasPermission("networks.admin")
                        && !player.hasPermission("networks.commands.setquantum")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length == 1) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "amount"));
                        return true;
                    }

                    try {
                        long amount = Calculator.calculate(args[1]).longValue();
                        if (amount < 0 || amount > NetworkQuantumStorage.MAX_AMOUNT) {
                            throw new NumberFormatException("");
                        }
                        setQuantum(player, amount);
                    } catch (NumberFormatException e) {
                        if ("full".equals(args[1])) {
                            setQuantum(player, NetworkQuantumStorage.MAX_AMOUNT);
                            return true;
                        }
                        player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "amount"));
                        player.sendMessage(e.getMessage());
                    }

                    return true;
                }
                case "addstorageitem" -> {
                    if (!player.hasPermission("networks.admin")
                        && !player.hasPermission("networks.commands.addstorageitem")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length == 1) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "amount"));
                        return true;
                    }

                    try {
                        int amount = Calculator.calculate(args[1]).intValue();
                        addStorageItem(player, amount);
                    } catch (NumberFormatException e) {
                        player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "amount"));
                        player.sendMessage(e.getMessage());
                    }

                    return true;
                }

                case "reducestorageitem" -> {
                    if (!player.hasPermission("networks.admin")
                        && !player.hasPermission("networks.commands.reducestorageitem")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length == 1) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "amount"));
                        return true;
                    }

                    try {
                        int amount = Calculator.calculate(args[1]).intValue();
                        reduceStorageItem(player, amount);
                    } catch (NumberFormatException e) {
                        player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "amount"));
                        player.sendMessage(e.getMessage());
                    }

                    return true;
                }

                case "setcontainerid" -> {
                    if (!player.hasPermission("networks.admin")
                        && !player.hasPermission("networks.commands.setcontainerid")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length == 1) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "containerId"));
                        return true;
                    }

                    try {
                        int containerId = Calculator.calculate(args[1]).intValue();
                        setContainerId(player, containerId);
                    } catch (NumberFormatException e) {
                        player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "containerId"));
                        player.sendMessage(e.getMessage());
                    }

                    return true;
                }

                case "worldedit" -> {
                    sendWorldEditDisabledMessage(player);
                    return true;
                }

                case "updateitem" -> {
                    if (!player.hasPermission("networks.admin")
                        && !player.hasPermission("networks.commands.updateitem")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    updateItem(player);
                    return true;
                }

                case "getstorageitem" -> {
                    if (!player.hasPermission("networks.admin")
                        && !player.hasPermission("networks.commands.getstorageitem")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length <= 2) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "slot"));
                        return true;
                    }

                    try {
                        int slot = Calculator.calculate(args[1]).intValue();
                        getStorageItem(player, slot);
                    } catch (NumberFormatException e) {
                        player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "slot"));
                        player.sendMessage(e.getMessage());
                    }

                    return true;
                }

                case "viewlog" -> {
                    if (!player.hasPermission("networks.admin") && !player.hasPermission("networks.commands.viewlog")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    viewLog(player);
                    return true;
                }

                case "map" -> {
                    if (!player.hasPermission("networks.admin") && !player.hasPermission("networks.commands.map")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length == 1) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "filePath"));
                        return true;
                    }

                    String filePath = args[1];
                    Pair<ItemStack, MapView> pair = MapUtil.getImageItem(filePath);
                    if (pair != null) {
                        ItemStack first = pair.getFirstValue();
                        MapView second = pair.getSecondValue();
                        if (second == null) {
                            player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "filePath"));
                            return true;
                        }

                        if (first != null) {
                            InventoryUtil.addItem(player, first);
                            player.sendMap(second);
                        }
                    }
                }

                case "cch" -> {
                    if (!player.hasPermission("networks.admin") && !player.hasPermission("networks.commands.cch")) {
                        player.sendMessage(getErrorMessage(ErrorType.NO_PERMISSION));
                        return true;
                    }

                    if (args.length == 1) {
                        player.sendMessage(getErrorMessage(ErrorType.MISSING_REQUIRED_ARGUMENT, "cchName"));
                        return true;
                    }

                    final Block targetBlock = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
                    if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                        player.sendMessage(Lang.getString("messages.commands.must-admin-debuggable"));
                        return true;
                    }
                    if (!ensureDirectBlockAccess(player, targetBlock.getLocation())) {
                        return true;
                    }

                    final SlimefunItem slimefunItem = StorageCacheUtils.getSfItem(targetBlock.getLocation());
                    if (slimefunItem == null) {
                        player.sendMessage(Lang.getString("messages.commands.must-admin-debuggable"));
                        return true;
                    }

                    if (!(slimefunItem instanceof AdminDebuggable debuggable)) {
                        player.sendMessage(Lang.getString("messages.commands.must-admin-debuggable"));
                        return true;
                    }

                    Location lookingAt = targetBlock.getLocation();
                    String cchName = args[1];
                    Map<Location, ?> map;
                    switch (cchName) {
                        case "l" -> {
                            // list all
                            player.sendMessage("l", "oah", "pah", "tmih", "tmoh", "caih", "caoh");
                            return true;
                        }
                        case "oah" -> map = NetworkRoot.observingAccessHistory;
                        case "pah" -> map = NetworkRoot.persistentAccessHistory;
                        case "tmih" -> map = NetworkRoot.transportMissInputHistory;
                        case "tmoh" -> map = NetworkRoot.transportMissOutputHistory;
                        case "caih" -> map = NetworkRoot.controlledAccessInputHistory;
                        case "caoh" -> map = NetworkRoot.controlledAccessOutputHistory;
                        default -> {
                            player.sendMessage(getErrorMessage(ErrorType.INVALID_REQUIRED_ARGUMENT, "cchName"));
                            return true;
                        }
                    }

                    Object value = map.get(lookingAt);
                    if (value == null) {
                        player.sendMessage("no cache yet.");
                    }

                    if (value instanceof Map<?, ?>) {
                        player.sendMessage("缓存: " + cchName);
                        @SuppressWarnings("unchecked") Map<Location, Integer> locations = (Map<Location, Integer>) value;
                        Map<String, Integer> formatted = locations.entrySet().stream().map(e -> {
                            SlimefunItem sf = canDirectlyAccess(e.getKey()) ? StorageCacheUtils.getSfItem(e.getKey()) : null;
                            if (sf == null) {
                                return Map.entry(e.getKey().toString(), e.getValue());
                            }
                            return Map.entry(ChatColor.translateAlternateColorCodes('&', sf.getItemName()), e.getValue());
                        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        for (Map.Entry<String, Integer> entry : formatted.entrySet()) {
                            player.sendMessage(entry.getKey() + ": " + entry.getValue());
                        }
                    } else if (value instanceof Number n) {
                        player.sendMessage("缓存: " + cchName);
                        player.sendMessage("值: " + n.intValue());
                    }
                }

                default -> help(player, null);
            }
        }
        // We always return true, even if the command was not executed, so that the help message is not shown.
        return true;
    }

    public void fillQuantum(@NotNull Player player, long amount) {
        final ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.no-item-in-hand"));
            return;
        }

        SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);

        if (!(slimefunItem instanceof NetworkQuantumStorage)) {
            player.sendMessage(Lang.getString("messages.commands.must-hand-quantum-storage"));
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        final QuantumCache quantumCache =
            DataTypeMethods.getCustom(meta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);

        if (quantumCache == null || quantumCache.getItemStack() == null) {
            player.sendMessage(Lang.getString("messages.commands.no-set-item"));
            return;
        }

        quantumCache.setAmount(Math.min(amount, NetworkQuantumStorage.MAX_AMOUNT));
        DataTypeMethods.setCustom(meta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE, quantumCache);
        quantumCache.updateMetaLore(meta);
        itemStack.setItemMeta(meta);
        player.sendMessage(Lang.getString("messages.commands.updated-quantum-storage"));
    }

    // change "networks-changed:recipe" -> "networks:recipe"
    public void fixBlueprint(@NotNull Player player, @NotNull String before) {
        ItemStack blueprint = player.getInventory().getItemInMainHand();
        if (blueprint.getType() == Material.AIR) {
            player.sendMessage(Lang.getString("messages.commands.must-hand-blueprint"));
            return;
        }

        final SlimefunItem item = SlimefunItem.getByItem(blueprint);

        if (!(item instanceof AbstractBlueprint)) {
            player.sendMessage(Lang.getString("messages.commands.must-hand-blueprint"));
            return;
        }

        ItemMeta blueprintMeta = blueprint.getItemMeta();

        final Optional<BlueprintInstance> optional = DataTypeMethods.getOptionalCustom(
            blueprintMeta,
            new NamespacedKey(before, Keys.BLUEPRINT_INSTANCE.getKey()),
            PersistentCraftingBlueprintType.TYPE);

        if (optional.isEmpty()) {
            player.sendMessage(Lang.getString("messages.commands.invalid-blueprint"));
            return;
        }

        BlueprintInstance instance = optional.get();

        ItemStack fix = NetworksSlimefunItemStacks.CRAFTING_BLUEPRINT.clone();
        ItemStack item2 = instance.getItemStack();
        if (item2 != null) {
            AbstractBlueprint.setBlueprint(fix, instance.getRecipeItems(), item2);
        }

        blueprint.setItemMeta(fix.getItemMeta());

        player.sendMessage(Lang.getString("messages.commands.fixed-blueprint"));
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String @NotNull [] args) {
        List<String> raw = onTabCompleteRaw(sender, args);
        return StringUtil.copyPartialMatches(args[args.length - 1], raw, new ArrayList<>());
    }

    public @NotNull List<String> onTabCompleteRaw(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("worldedit")) {
            return List.of();
        }

        switch (args.length) {
            case 1 -> {
                return List.of(
                    "addStorageItem",
                    "fillQuantum",
                    "fixBlueprint",
                    "getStorageItem",
                    "help",
                    "reduceStorageItem",
                    "setContainerId",
                    "setQuantum",
                    "updateItem",
                    "viewLog");
            }
            case 2 -> {
                return switch (args[0].toLowerCase(Locale.ROOT)) {
                    // case "help", "updateitem" -> List.of();
                    case "getstorageitem" -> List.of("<slot>");
                    case "fillquantum", "addstorageitem", "reducestorageitem", "setquantum" -> List.of("<amount>");
                    case "fixblueprint" -> List.of("<keyInMeta>");
                    case "setcontainerid" -> List.of("<containerId>");
                    case "worldedit" -> List.of(
                        "pos1",
                        "pos2",
                        "paste",
                        "clear",
                        "clone",
                        "blockmenu",
                        "blockinfo",
                        "clearpos",
                        "showareaoutline");
                    default -> List.of();
                };
            }
            case 3 -> {
                if (args[0].equalsIgnoreCase("worldedit")) {
                    return switch (args[1]) {
                        // case "pos1", "pos2" -> List.of();
                        case "paste" -> Slimefun.getRegistry().getAllSlimefunItems().stream()
                            .filter(sfItem -> sfItem.getItem().getType().isBlock())
                            .map(SlimefunItem::getId)
                            .toList();
                        case "blockinfo" -> List.of("add", "remove", "set");
                        case "blockmenu" -> List.of("setSlot");
                        case "clear" -> List.of("true", "false");
                        default -> List.of();
                    };
                }
            }
            case 4 -> {
                if (args[0].equalsIgnoreCase("worldedit")) {
                    return switch (args[1].toLowerCase(Locale.ROOT)) {
                        case "paste" -> List.of("override", "keep");
                        case "blockmenu" -> "setslot".equals(args[2].toLowerCase(Locale.ROOT))
                            ? List.of(
                            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
                            "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27",
                            "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
                            "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53")
                            : List.of();
                        case "clear" -> List.of("true", "false");
                        default -> List.of();
                    };
                }
            }
            case 5 -> {
                if (args[0].equalsIgnoreCase("worldedit")) {
                    if (args[1].equalsIgnoreCase("paste")) {
                        return List.of("true", "false");
                    }
                }
            }
        }

        return new ArrayList<>();
    }

    public @NotNull String getErrorMessage(@NotNull ErrorType errorType) {
        return getErrorMessage(errorType, null);
    }

    public @NotNull String getErrorMessage(@NotNull ErrorType errorType, String argument) {
        return switch (errorType) {
            case NO_PERMISSION -> Lang.getString("messages.commands.no-permission");
            case NO_ITEM_IN_HAND -> Lang.getString("messages.commands.no-item-in-hand");
            case MISSING_REQUIRED_ARGUMENT -> String.format(
                Lang.getString("messages.commands.missing-required-argument"), argument);
            case INVALID_REQUIRED_ARGUMENT -> String.format(
                Lang.getString("messages.commands.invalid-required-argument"), argument);
            case MUST_BE_PLAYER -> Lang.getString("messages.commands.must-be-player");
            default -> Lang.getString("messages.commands.unknown-error");
        };
    }
}
