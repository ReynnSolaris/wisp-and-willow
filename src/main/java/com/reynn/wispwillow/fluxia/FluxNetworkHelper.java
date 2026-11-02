package com.reynn.wispwillow.fluxia;

import com.reynn.wispwillow.fluxia.net.FluxArcS2C;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class FluxNetworkHelper {

    /**
     * @param cameFrom for every node the walk reached, the node it was reached <em>from</em>.
     *                 The BFS parent chain, and the only record of how the graph is actually
     *                 wired once the walk has flattened it into a list.
     */
    public record FluxNetworkSnapshot(
            List<IFluxiaNode> nodes,
            List<IFluxiaNode> pools,
            List<IFluxiaNode> batteries,
            List<IFluxiaNode> machines,
            Map<BlockPos, BlockPos> cameFrom
    ) {
        /**
         * The chain of positions from the walk's origin to {@code target}, origin first.
         *
         * <p>Distribution is global — a pool pushes straight into every node it can reach,
         * however many links away. Drawing that transfer as one long beam claimed a direct
         * connection that does not exist, so the beam is drawn hop by hop along this path
         * instead: the power still moves in one step, but what the player sees is the wiring
         * they actually laid.</p>
         */
        public List<BlockPos> pathTo(BlockPos origin, BlockPos target) {
            List<BlockPos> reversed = new ArrayList<>();
            BlockPos at = target;
            // Bounded by the node ceiling: a corrupt parent map must not spin forever.
            for (int guard = 0; guard <= MAX_NODES; guard++) {
                reversed.add(at);
                if (at.equals(origin)) {
                    Collections.reverse(reversed);
                    return reversed;
                }
                BlockPos parent = cameFrom.get(at);
                if (parent == null) break;
                at = parent;
            }
            return List.of(origin, target);   // no path recorded; fall back to a direct beam
        }
    }

    /** Ceiling on how many nodes a single Fluxia graph walk will visit. */
    private static final int MAX_NODES = 512;

    public static FluxNetworkSnapshot gatherNetwork(ServerLevel level, BlockPos startPos) {
        Map<BlockPos, IFluxiaNode> visited = new HashMap<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();

        var startBe = level.getBlockEntity(startPos);
        if (!(startBe instanceof IFluxiaNode startNode))
            return new FluxNetworkSnapshot(List.of(), List.of(), List.of(), List.of(), Map.of());

        visited.put(startPos, startNode);
        queue.add(startPos);

        while (!queue.isEmpty() && visited.size() < MAX_NODES) {
            BlockPos pos = queue.remove();
            IFluxiaNode node = visited.get(pos);

            // 1) Scan 6-directional neighbors
            for (Direction dir : Direction.values()) {
                if (!node.canConnectFlux(dir)) continue;

                BlockPos neighborPos = pos.relative(dir);
                if (visited.containsKey(neighborPos)) continue;

                var be = level.getBlockEntity(neighborPos);
                if (be instanceof IFluxiaNode neighbor && neighbor.canConnectFlux(dir.getOpposite())) {
                    visited.put(neighborPos, neighbor);
                    cameFrom.put(neighborPos, pos);
                    queue.add(neighborPos);
                }
            }

            // 2) Scan explicit wand links. Prune first: FluxLinks.onNodeRemoved handles the
            // common case at break time, but this catches whatever slipped past it — a
            // target removed while this end was unloaded, or by something that writes to
            // the world without going through Block#onRemove.
            FluxLinks.pruneStale(level, node);

            for (BlockPos linkedPos : node.getLinkedNodes()) {
                if (visited.containsKey(linkedPos)) continue;
                var be = level.getBlockEntity(linkedPos);
                if (be instanceof IFluxiaNode linkedNode) {
                    visited.put(linkedPos, linkedNode);
                    cameFrom.put(linkedPos, pos);
                    queue.add(linkedPos);
                }
            }
        }

        // Split by role
        List<IFluxiaNode> all       = new ArrayList<>(visited.values());
        List<IFluxiaNode> pools     = new ArrayList<>();
        List<IFluxiaNode> batteries = new ArrayList<>();
        List<IFluxiaNode> machines  = new ArrayList<>();

        for (var node : all) {
            switch (node.getFluxRole()) {
                case POOL -> pools.add(node);
                case BATTERY -> batteries.add(node);
                case MACHINE -> machines.add(node);
                default -> {} // RELAY / others stay only in 'all'
            }
        }

        return new FluxNetworkSnapshot(all, pools, batteries, machines, cameFrom);
    }

    /**
     * Global network-wide distribution FROM the node at sourcePos.
     *
     * - Uses BFS graph from sourcePos (adjacent + wand links)
     * - Prioritizes: MACHINES -> BATTERIES -> other POOLS -> RELAY/others
     * - Sends up to perTargetLimit flux to each target per tick
     */
    public static void distributeFromSource(ServerLevel level, BlockPos sourcePos, int perTargetLimit) {
        var be = level.getBlockEntity(sourcePos);
        if (!(be instanceof IFluxiaNode sourceNode)) return;

        IFluxiaStorage srcStorage = sourceNode.getFluxStorage();
        if (srcStorage == null) return;

        int available = srcStorage.amount();
        if (available <= 0) return;

        FluxNetworkSnapshot snapshot = gatherNetwork(level, sourcePos);
        if (snapshot.nodes().isEmpty()) return;

        // Build prioritized target list. A LinkedHashSet keeps the machines -> batteries ->
        // pools -> everything else ordering while deduplicating in O(1); the previous
        // List.contains() check made this quadratic in the size of the network.
        Set<IFluxiaNode> targets = new LinkedHashSet<>();

        // 1) Machines first
        for (IFluxiaNode n : snapshot.machines()) if (n != sourceNode) targets.add(n);
        // 2) Batteries next
        for (IFluxiaNode n : snapshot.batteries()) if (n != sourceNode) targets.add(n);
        // 3) Other pools (besides source)
        for (IFluxiaNode n : snapshot.pools()) if (n != sourceNode) targets.add(n);
        // 4) Anything else (relays, etc.) not already included
        for (IFluxiaNode n : snapshot.nodes()) if (n != sourceNode) targets.add(n);

        // Now push flux to targets in that order
        boolean movedAny = false;
        for (IFluxiaNode targetNode : targets) {
            if (available <= 0) break;

            IFluxiaStorage targetStorage = targetNode.getFluxStorage();
            if (targetStorage == null) continue;

            // Skip if full
            if (targetStorage.amount() >= targetStorage.capacity()) continue;

            int toSend = Math.min(perTargetLimit, available);
            if (toSend <= 0) break;

            int pulled = srcStorage.extract(toSend, false);
            if (pulled <= 0) break;
            int accepted = targetStorage.insert(pulled, false);
            if (accepted < pulled) srcStorage.insert(pulled - accepted, false);
            if (accepted <= 0) continue;

            available -= accepted;
            movedAny = true;

            // mark the receiving end dirty
            if (targetNode instanceof BlockEntity tBe) {
                tBe.setChanged();
                level.sendBlockUpdated(tBe.getBlockPos(), tBe.getBlockState(), tBe.getBlockState(), 3);
                drawTransfer(level, snapshot, sourcePos, tBe.getBlockPos(), accepted);
            }

            if (available <= 0) break;
        }

        // The source only needs one update packet for the whole distribution pass,
        // not one per target it fed.
        if (movedAny) {
            be.setChanged();
            level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    /**
     * Draws one transfer as a chain of beams along the links it conceptually travelled.
     *
     * <p>The power itself moves in a single step, straight from the pool into the target —
     * that is what {@code distributeFromSource} does and it is not changing. What changes is
     * the picture: a pool two links away from a machine used to fire one beam across the gap,
     * which read as a connection the player had never made. Walking the BFS parent chain and
     * sending one arc per hop makes the light follow the wand links instead.</p>
     */
    private static void drawTransfer(ServerLevel level, FluxNetworkSnapshot snapshot,
                                     BlockPos sourcePos, BlockPos targetPos, int amount) {
        List<BlockPos> path = snapshot.pathTo(sourcePos, targetPos);
        if (path.size() < 2) return;
        for (int i = 0; i + 1 < path.size(); i++) {
            FluxArcS2C.send(level, path.get(i), path.get(i + 1), amount);
        }
    }
}
