package buildcraft.core.lib.utils;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

import buildcraft.core.BuildCraftCore;
import buildcraft.core.proxy.CoreProxy;

public class BlockMiner {
    protected final World world;
    protected final TileEntity owner;
    protected final BlockPos pos;

    private boolean hasMined, hasFailed;
    private double powerRequired, powerAccepted;

    public BlockMiner(World world, TileEntity owner, BlockPos pos) {
        this.world = world;
        this.owner = owner;
        this.pos = pos;
    }

    public boolean hasMined() {
        return hasMined;
    }

    public boolean hasFailed() {
        return hasFailed;
    }

    public void mineStack(ItemStack stack) {
        // First, try to add to a nearby chest
        stack.stackSize -= Utils.addToRandomInventoryAround(owner.getWorld(), owner.getPos(), stack);

        // Second, try to add to adjacent pipes
        if (stack.stackSize > 0) {
            stack.stackSize -= Utils.addToRandomInjectableAround(owner.getWorld(), owner.getPos(), null, stack);
        }

        // Lastly, throw the object away
        if (stack.stackSize > 0) {
            float f = world.rand.nextFloat() * 0.8F + 0.1F;
            float f1 = world.rand.nextFloat() * 0.8F + 0.1F;
            float f2 = world.rand.nextFloat() * 0.8F + 0.1F;

            EntityItem entityitem = new EntityItem(owner.getWorld(), owner.getPos().getX() + f, owner.getPos().getY() + f1 + 0.5F, owner.getPos()
                    .getZ() + f2, stack);

            entityitem.lifespan = BuildCraftCore.itemLifespan * 20;
            entityitem.setDefaultPickupDelay();

            float f3 = 0.05F;
            entityitem.motionX = (float) world.rand.nextGaussian() * f3;
            entityitem.motionY = (float) world.rand.nextGaussian() * f3 + 1.0F;
            entityitem.motionZ = (float) world.rand.nextGaussian() * f3;
            owner.getWorld().spawnEntityInWorld(entityitem);
        }
    }

    public void invalidate() {
        world.sendBlockBreakProgress(pos.hashCode(), pos, -1);
    }

    public double acceptPower(double offeredPower) {
        powerRequired = BlockUtils.computeBlockBreakEnergy(world, pos);

        double usedAmount = MathUtils.clamp(offeredPower, 0, Math.max(0, powerRequired - powerAccepted));
        powerAccepted += usedAmount;

        if (powerAccepted >= powerRequired) {
            world.sendBlockBreakProgress(pos.hashCode(), pos, -1);

            hasMined = true;

            IBlockState state = world.getBlockState(pos);

            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(world, pos, state, CoreProxy.proxy.getBuildCraftPlayer((WorldServer) world)
                    .get());
            MinecraftForge.EVENT_BUS.post(breakEvent);

            if (!breakEvent.isCanceled()) {
                List<ItemStack> stacks = BlockUtils.getItemStackFromBlock((WorldServer) world, pos);

                if (stacks != null) {
                    for (ItemStack s : stacks) {
                        if (s != null) {
                            mineStack(s);
                        }
                    }
                }

                world.playAuxSFXAtEntity(null, 2001, pos, Block.getStateId(state));

                Utils.preDestroyBlock(world, pos);
                world.setBlockToAir(pos);
            } else {
                hasFailed = true;
            }
        } else {
            world.sendBlockBreakProgress(pos.hashCode(), pos, (int) MathUtils.clamp(Math.floor(powerAccepted * 10 / powerRequired), 0, 9));
        }
        return usedAmount;
    }
}
