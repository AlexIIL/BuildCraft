/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fluids.FluidStack;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.api.schematics.SchematicBlockFactory;
import buildcraft.api.schematics.SchematicBlockFactoryRegistry;

public class SchematicBlockManager {
    public static ISchematicBlock<?> getSchematicBlock(SchematicBlockContext context) {
        for (SchematicBlockFactory<?> schematicBlockFactory : Lists.reverse(SchematicBlockFactoryRegistry.getFactories())) {
            if (schematicBlockFactory.predicate.test(context)) {
                ISchematicBlock<?> schematicBlock = schematicBlockFactory.supplier.get();
                schematicBlock.init(context);
                return schematicBlock;
            }
        }
        throw new UnsupportedOperationException();
    }

    public static ISchematicBlock<?> getSchematicBlock(IBlockAccess world,
                                                       BlockPos basePos,
                                                       BlockPos pos) {
        SchematicBlockContext context = new SchematicBlockContext(
            world,
            basePos,
            pos
        );
        return getSchematicBlock(context);
    }

    public static Pair<List<ItemStack>[][][], List<FluidStack>[][][]> computeRequired(Blueprint blueprint) {
        @SuppressWarnings("unchecked") List<ItemStack>[][][] requiredItems = (List<ItemStack>[][][]) new List
            [blueprint.size.getX()]
            [blueprint.size.getY()]
            [blueprint.size.getZ()];
        @SuppressWarnings("unchecked") List<FluidStack>[][][] requiredFluids = (List<FluidStack>[][][]) new List
            [blueprint.size.getX()]
            [blueprint.size.getY()]
            [blueprint.size.getZ()];
        BptBlockAccess access = new BptBlockAccess(blueprint);
        for (int z = 0; z < blueprint.size.getZ(); z++) {
            for (int y = 0; y < blueprint.size.getY(); y++) {
                for (int x = 0; x < blueprint.size.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    int paletteIndex = blueprint.data[pos.getX()][pos.getY()][pos.getZ()];
                    ISchematicBlock<?> schematicBlock = blueprint.palette.get(paletteIndex);
                    SchematicBlockContext schematicBlockContext = new SchematicBlockContext(
                        access,
                        BlockPos.ORIGIN,
                        pos
                    );
                    requiredItems[x][y][z] =
                        schematicBlock.computeRequiredItems(schematicBlockContext);
                    requiredFluids[x][y][z] =
                        schematicBlock.computeRequiredFluids(schematicBlockContext);
                }
            }
        }
        return Pair.of(requiredItems, requiredFluids);
    }

    @Nonnull
    public static NBTTagCompound writeToNBT(ISchematicBlock<?> schematicBlock) {
        NBTTagCompound schematicBlockTag = new NBTTagCompound();
        schematicBlockTag.setString(
            "name",
            SchematicBlockFactoryRegistry
                .getFactoryByInstance(schematicBlock)
                .name
                .toString()
        );
        schematicBlockTag.setTag("data", schematicBlock.serializeNBT());
        return schematicBlockTag;
    }

    @Nonnull
    public static ISchematicBlock<?> readFromNBT(NBTTagCompound schematicBlockTag) throws InvalidInputDataException {
        ResourceLocation name = new ResourceLocation(schematicBlockTag.getString("name"));
        SchematicBlockFactory<?> factory = SchematicBlockFactoryRegistry.getFactoryByName(name);
        if (factory == null) {
            throw new InvalidInputDataException("Unknown schematic type " + name);
        }
        ISchematicBlock<?> schematicBlock = factory.supplier.get();
        NBTTagCompound data = schematicBlockTag.getCompoundTag("data");
        try {
            schematicBlock.deserializeNBT(data);
            return schematicBlock;
        } catch (InvalidInputDataException e) {
            throw new InvalidInputDataException("Failed to load the schematic from " + data, e);
        }
    }
}
