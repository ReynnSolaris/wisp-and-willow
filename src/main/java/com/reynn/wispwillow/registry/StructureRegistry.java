package com.reynn.wispwillow.registry;

import com.mojang.serialization.MapCodec;
import com.reynn.wispwillow.WispWillow;
import com.reynn.wispwillow.worldgen.structures.FluxiaOasisPiece;
import com.reynn.wispwillow.worldgen.structures.FluxiaOasisStructure;
import com.reynn.wispwillow.worldgen.structures.MainLandStructures;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class StructureRegistry {
    public static final DeferredRegister<StructureType<?>> DEFERRED_REGISTRY_STRUCTURE = DeferredRegister.create(Registries.STRUCTURE_TYPE, WispWillow.MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<MainLandStructures>> SKY_STRUCTURES = DEFERRED_REGISTRY_STRUCTURE.register("willow_structures", () -> explicitStructureTypeTyping(MainLandStructures.CODEC));

    public static final DeferredHolder<StructureType<?>, StructureType<FluxiaOasisStructure>> FLUXIA_OASIS =
            DEFERRED_REGISTRY_STRUCTURE.register("fluxia_oasis", () -> explicitStructureTypeTyping(FluxiaOasisStructure.CODEC));

    /**
     * Piece types, which is how a half-generated structure finds its way back out of the
     * region file: the id saved beside each piece is looked up here to rebuild it.
     */
    public static final DeferredRegister<StructurePieceType> DEFERRED_REGISTRY_STRUCTURE_PIECE =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, WispWillow.MODID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> FLUXIA_OASIS_PIECE =
            DEFERRED_REGISTRY_STRUCTURE_PIECE.register("fluxia_oasis",
                    () -> (StructurePieceType.ContextlessType) FluxiaOasisPiece::new);

    private static <T extends Structure> StructureType<T> explicitStructureTypeTyping(MapCodec<T> structureCodec) {
        return () -> structureCodec;
    }
}
