package io.github.sefiraat.networks.network;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.ytdd9527.networksexpansion.utils.FoliaSupport;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import io.github.sefiraat.networks.slimefun.network.NetworkPowerNode;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkNode {

    public static final Set<BlockFace> VALID_FACES =
        EnumSet.of(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    @Getter
    protected final Set<NetworkNode> childrenNodes = ConcurrentHashMap.newKeySet();

    protected final Location nodePosition;
    protected final NodeType nodeType;

    @Getter
    protected final long power;

    @Getter
    protected @Nullable NetworkNode parent = null;

    @Getter
    protected NetworkRoot root = null;

    private record NodeScan(@NotNull NodeDefinition definition, @NotNull NodeType type, long power, boolean menuValidated) {
    }

    public NetworkNode(Location location, NodeType type) {
        this(location, type, -1);
    }

    protected NetworkNode(Location location, NodeType type, long power) {
        this.nodePosition = location;
        this.nodeType = type;
        this.power = power >= 0 ? power : retrieveBlockCharge();
    }

    public void addChild(@NotNull NetworkNode child) {
        child.setParent(this);
        child.setRoot(this.getRoot());
        if (this.root != null) {
            this.root.addRootPower(child.getPower());
            this.root.registerNode(child.nodePosition, child.nodeType);
        }
        this.childrenNodes.add(child);
    }

    @NotNull
    public Location getNodePosition() {
        return nodePosition;
    }

    @NotNull
    public NodeType getNodeType() {
        return nodeType;
    }

    public boolean networkContains(@NotNull NetworkNode networkNode) {
        return networkContains(networkNode.nodePosition);
    }

    public boolean networkContains(@NotNull Location location) {
        if (this.root == null) {
            return false;
        }

        return this.root.getNodeLocations().contains(location);
    }

    private void setRoot(@Nullable NetworkRoot root) {
        this.root = root;
    }

    private void setParent(@Nullable NetworkNode parent) {
        this.parent = parent;
    }

    public void addAllChildren() {
        addAllChildrenAsync();
    }

    public @NotNull CompletableFuture<Void> addAllChildrenAsync() {
        if (this.root == null) {
            return CompletableFuture.completedFuture(null);
        }

        Deque<NetworkNode> nodeStack = new ArrayDeque<>(200);
        nodeStack.push(this);
        return addAllChildrenAsync(nodeStack);
    }

    private @NotNull CompletableFuture<Void> addAllChildrenAsync(@NotNull Deque<NetworkNode> nodeStack) {
        while (!nodeStack.isEmpty()) {
            NetworkNode currentNode = nodeStack.pop();
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

            for (BlockFace face : VALID_FACES) {
                final Location testLocation = currentNode.nodePosition.clone().add(face.getDirection());

                chain = chain.thenCompose(ignored -> scanNode(testLocation).thenCompose(scan -> {
                    if (scan == null || this.root == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    final Location ownerLocation = this.root.nodePosition;
                    return FoliaSupport.supplyRegion(ownerLocation, () -> {
                        if (this.root.isOverburdened()) {
                            return null;
                        }

                        final NodeType testType = scan.type();
                        if (testType == NodeType.CONTROLLER && !testLocation.equals(this.root.nodePosition)) {
                            killAdditionalController(testLocation);
                            return null;
                        }

                        if (testType == NodeType.CONTROLLER || currentNode.networkContains(testLocation)) {
                            return null;
                        }

                        if (this.root.getNodeCount() >= this.root.getMaxNodes()) {
                            this.root.setOverburdened(true);
                            nodeStack.clear();
                            return null;
                        }

                        final NetworkNode networkNode = new NetworkNode(testLocation, testType, scan.power());
                        currentNode.addChild(networkNode, scan.menuValidated());

                        nodeStack.push(networkNode);
                        scan.definition().setNode(networkNode);
                        NetworkStorage.registerNode(testLocation, scan.definition());
                        return null;
                    });
                }));
            }

            return chain.thenCompose(ignored -> addAllChildrenAsync(nodeStack));
        }

        return CompletableFuture.completedFuture(null);
    }

    private @NotNull CompletableFuture<NodeScan> scanNode(@NotNull Location location) {
        if (location.getWorld() == null
            || !location.isWorldLoaded()
            || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return CompletableFuture.completedFuture(null);
        }

        return FoliaSupport.supplyRegion(location, () -> {
            final NodeDefinition definition = NetworkStorage.getNode(location);
            if (definition == null) {
                return null;
            }

            final NodeType type = definition.getType();
            long power = 0;
            boolean menuValidated = false;
            if (type == NodeType.POWER_NODE) {
                final SlimefunItem item = StorageCacheUtils.getSfItem(location);
                if (item instanceof NetworkPowerNode powerNode) {
                    power = powerNode.getCharge(location);
                }
            } else if (type == NodeType.CELL) {
                BlockMenu menu = StorageCacheUtils.getMenu(location);
                menuValidated = menu != null && Arrays.equals(
                    menu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW),
                    NetworkRoot.CELL_AVAILABLE_SLOTS);
            } else if (type == NodeType.GREEDY_BLOCK) {
                BlockMenu menu = StorageCacheUtils.getMenu(location);
                menuValidated = menu != null && Arrays.equals(
                    menu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW),
                    NetworkRoot.GREEDY_BLOCK_AVAILABLE_SLOTS);
            } else if (type == NodeType.ADVANCED_GREEDY_BLOCK) {
                BlockMenu menu = StorageCacheUtils.getMenu(location);
                menuValidated = menu != null && Arrays.equals(
                    menu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW),
                    NetworkRoot.ADVANCED_GREEDY_BLOCK_AVAILABLE_SLOTS);
            }
            return new NodeScan(definition, type, power, menuValidated);
        });
    }

    private void addChild(@NotNull NetworkNode child, boolean menuValidated) {
        child.setParent(this);
        child.setRoot(this.getRoot());
        if (this.root != null) {
            this.root.addRootPower(child.getPower());
            this.root.registerNode(child.nodePosition, child.nodeType, menuValidated);
        }
        this.childrenNodes.add(child);
    }

    private void killAdditionalController(@NotNull Location location) {
        FoliaSupport.runRegion(location, () -> {
            SlimefunItem sfItem = StorageCacheUtils.getSfItem(location);
            if (sfItem == null || location.getWorld() == null) {
                return;
            }
            Slimefun.getDatabaseManager().getBlockDataController().removeBlock(location);
            NetworkController.wipeNetwork(location);
            location.getWorld().dropItemNaturally(location, sfItem.getItem());
            location.getBlock().setType(Material.AIR);
            NetworkController.wipeNetwork(location);
        });
    }

    protected long retrieveBlockCharge() {
        if (this.nodeType == NodeType.POWER_NODE) {
            int blockCharge = 0;
            final SlimefunItem item = StorageCacheUtils.getSfItem(this.nodePosition);
            if (item instanceof NetworkPowerNode powerNode) {
                blockCharge = powerNode.getCharge(this.nodePosition);
            }
            return blockCharge;
        }
        return 0;
    }
}
