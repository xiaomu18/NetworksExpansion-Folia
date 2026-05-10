package io.github.sefiraat.networks;

import io.github.bakedlibs.dough.blocks.ChunkPosition;
import io.github.sefiraat.networks.network.NetworkNode;
import io.github.sefiraat.networks.network.NodeDefinition;
import lombok.experimental.UtilityClass;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class NetworkStorage {
    private static final Map<ChunkPosition, Set<Location>> ALL_NETWORK_OBJECTS_BY_CHUNK = new ConcurrentHashMap<>();
    private static final Map<Location, NodeDefinition> ALL_NETWORK_OBJECTS = new ConcurrentHashMap<>();

    public static void removeNode(Location location) {
        final NodeDefinition nodeDefinition = ALL_NETWORK_OBJECTS.remove(location);
        removeChunkIndex(location);

        if (nodeDefinition == null) {
            return;
        }

        final NetworkNode node = nodeDefinition.getNode();

        if (node == null) {
            return;
        }

        for (NetworkNode childNode : new HashSet<>(node.getChildrenNodes())) {
            removeNode(childNode.getNodePosition());
        }
    }

    public static boolean containsKey(Location location) {
        return ALL_NETWORK_OBJECTS.containsKey(location);
    }

    public static NodeDefinition getNode(Location location) {
        return ALL_NETWORK_OBJECTS.get(location);
    }

    public static void registerNode(@NotNull Location location, NodeDefinition nodeDefinition) {
        ALL_NETWORK_OBJECTS.put(location, nodeDefinition);
        ChunkPosition unionKey = new ChunkPosition(location);
        ALL_NETWORK_OBJECTS_BY_CHUNK.computeIfAbsent(unionKey, ignored -> ConcurrentHashMap.newKeySet()).add(location);
    }

    public static void unregisterChunk(@NotNull Chunk chunk) {
        ChunkPosition chunkPosition = new ChunkPosition(chunk);
        Set<Location> locations = ALL_NETWORK_OBJECTS_BY_CHUNK.remove(chunkPosition);
        if (locations == null) {
            return;
        }
        for (Location location : new HashSet<>(locations)) {
            removeNode(location);
        }
    }

    public static @NotNull Map<Location, NodeDefinition> getAllNetworkObjects() {
        return new HashMap<>(ALL_NETWORK_OBJECTS);
    }

    private static void removeChunkIndex(@NotNull Location location) {
        ChunkPosition unionKey = new ChunkPosition(location);
        Set<Location> locations = ALL_NETWORK_OBJECTS_BY_CHUNK.get(unionKey);
        if (locations == null) {
            return;
        }
        locations.remove(location);
        if (locations.isEmpty()) {
            ALL_NETWORK_OBJECTS_BY_CHUNK.remove(unionKey, locations);
        }
    }
}
