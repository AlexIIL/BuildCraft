/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt */
package buildcraft.factory.tile;

import java.lang.ref.WeakReference;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.WorldServer;

import buildcraft.api.core.IInvSlot;
import buildcraft.api.mj.EnumMjDeviceType;
import buildcraft.api.mj.EnumMjPowerType;
import buildcraft.api.mj.IMjExternalStorage;
import buildcraft.api.mj.IMjHandler;
import buildcraft.api.mj.reference.DefaultMjExternalStorage;
import buildcraft.api.mj.reference.DefaultMjInternalStorage;
import buildcraft.api.tiles.IHasWork;
import buildcraft.core.lib.block.TileBuildCraft;
import buildcraft.core.lib.gui.ContainerDummy;
import buildcraft.core.lib.inventory.InvUtils;
import buildcraft.core.lib.inventory.InventoryConcatenator;
import buildcraft.core.lib.inventory.InventoryIterator;
import buildcraft.core.lib.inventory.SimpleInventory;
import buildcraft.core.lib.inventory.StackHelper;
import buildcraft.core.lib.utils.CraftingUtils;
import buildcraft.core.lib.utils.Utils;
import buildcraft.core.proxy.CoreProxy;

public class TileAutoWorkbench extends TileBuildCraft implements ISidedInventory, IHasWork, IMjHandler {

    public static final int SLOT_RESULT = 9;
    public static final int CRAFT_TIME = 256;
    public static final int UPDATE_TIME = 16;
    private static final int[] SLOTS = Utils.createSlotArray(0, 10);

    public int progress = 0;
    public LocalInventoryCrafting craftMatrix = new LocalInventoryCrafting();

    private SimpleInventory resultInv = new SimpleInventory(1, "Auto Workbench", 64);
    private SimpleInventory inputInv = new SimpleInventory(9, "Auto Workbench", 64);

    private IInventory inv = InventoryConcatenator.make().add(inputInv).add(resultInv).add(craftMatrix);

    private SlotCrafting craftSlot;
    private InventoryCraftResult craftResult = new InventoryCraftResult();

    private int[] bindings = new int[9];
    private int[] bindingCounts = new int[9];

    private int update = Utils.RANDOM.nextInt();

    private boolean hasWork = false;
    private boolean scheduledCacheRebuild = false;

    private final DefaultMjExternalStorage externalStorage;
    private final DefaultMjInternalStorage internalStorage;

    public TileAutoWorkbench() {
        externalStorage = new DefaultMjExternalStorage(EnumMjDeviceType.MACHINE, EnumMjPowerType.REDSTONE, 1.6);
        internalStorage = new DefaultMjInternalStorage(3.2, 1.6, 400, 0.16);
        externalStorage.setInternalStorage(internalStorage);
    }

    @Override
    public boolean hasWork() {
        return hasWork;
    }

    public class LocalInventoryCrafting extends InventoryCrafting {
        public IRecipe currentRecipe;
        public boolean useBindings, isJammed;

        public LocalInventoryCrafting() {
            super(new ContainerDummy(), 3, 3);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (useBindings) {
                if (slot >= 0 && slot < 9 && bindings[slot] >= 0) {
                    return inputInv.getStackInSlot(bindings[slot]);
                } else {
                    return null;
                }
            }
            return super.getStackInSlot(slot);
        }

        public ItemStack getRecipeOutput() {
            if (currentRecipe == null) {
                return null;
            }
            ItemStack result = currentRecipe.getCraftingResult(craftMatrix);
            if (result != null) {
                result = result.copy();
            }
            return result;
        }

        private IRecipe findRecipe() {
            for (IInvSlot slot : InventoryIterator.getIterable(this, EnumFacing.UP)) {
                ItemStack stack = slot.getStackInSlot();
                if (stack == null) {
                    continue;
                }
                if (stack.getItem().hasContainerItem(stack)) {
                    return null;
                }
            }

            return CraftingUtils.findMatchingRecipe(craftMatrix, worldObj);
        }

        public void rebuildCache() {
            currentRecipe = findRecipe();
            hasWork = currentRecipe != null && currentRecipe.getRecipeOutput() != null;

            ItemStack result = getRecipeOutput();
            ItemStack resultInto = resultInv.getStackInSlot(0);

            if (resultInto != null && (!StackHelper.canStacksMerge(resultInto, result) || resultInto.stackSize + result.stackSize > resultInto
                    .getMaxStackSize())) {
                isJammed = true;
            } else {
                isJammed = false;
            }
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            if (useBindings) {
                if (slot >= 0 && slot < 9 && bindings[slot] >= 0) {
                    inputInv.setInventorySlotContents(bindings[slot], stack);
                }
                return;
            }
            super.setInventorySlotContents(slot, stack);
            scheduledCacheRebuild = true;
        }

        @Override
        public void markDirty() {
            super.markDirty();
            scheduledCacheRebuild = true;
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            if (useBindings) {
                if (slot >= 0 && slot < 9 && bindings[slot] >= 0) {
                    return inputInv.decrStackSize(bindings[slot], amount);
                } else {
                    return null;
                }
            }
            scheduledCacheRebuild = true;
            return decrStackSize(slot, amount);
        }

        public void setUseBindings(boolean use) {
            useBindings = use;
        }
    }

    public WeakReference<EntityPlayer> getInternalPlayer() {
        return CoreProxy.proxy.getBuildCraftPlayer((WorldServer) worldObj, pos.up());
    }

    @Override
    public void markDirty() {
        super.markDirty();
        inv.markDirty();
    }

    @Override
    public int getSizeInventory() {
        return 10;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv.getStackInSlot(slot);
    }

    @Override
    public ItemStack decrStackSize(int slot, int count) {
        return inv.decrStackSize(slot, count);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inv.setInventorySlotContents(slot, stack);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        return inv.getStackInSlotOnClosing(slot);
    }

    @Override
    public String getInventoryName() {
        return "";
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return worldObj.getTileEntity(pos) == this && player.getDistanceSq(pos) <= 64.0D;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        resultInv.readFromNBT(data);
        if (data.hasKey("input")) {
            InvUtils.readInvFromNBT(inputInv, "input", data);
            InvUtils.readInvFromNBT(craftMatrix, "matrix", data);
        } else {
            InvUtils.readInvFromNBT(inputInv, "matrix", data);
            for (int i = 0; i < 9; i++) {
                ItemStack inputStack = inputInv.getStackInSlot(i);
                if (inputStack != null) {
                    ItemStack matrixStack = inputStack.copy();
                    matrixStack.stackSize = 1;
                    craftMatrix.setInventorySlotContents(i, matrixStack);
                }
            }
        }

        craftMatrix.rebuildCache();

        // Legacy Code
        if (data.hasKey("stackList")) {
            ItemStack[] stacks = new ItemStack[9];
            InvUtils.readStacksFromNBT(data, "stackList", stacks);
            for (int i = 0; i < 9; i++) {
                craftMatrix.setInventorySlotContents(i, stacks[i]);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        resultInv.writeToNBT(data);
        InvUtils.writeInvToNBT(inputInv, "input", data);
        InvUtils.writeInvToNBT(craftMatrix, "matrix", data);
    }

    @Override
    public void update() {
        super.update();

        if (worldObj.isRemote) {
            return;
        }

        internalStorage.tick(getWorld());

        if (scheduledCacheRebuild) {
            craftMatrix.rebuildCache();
            scheduledCacheRebuild = false;
        }

        if (craftMatrix.isJammed || craftMatrix.currentRecipe == null) {
            progress = 0;
            return;
        }

        if (craftSlot == null) {
            craftSlot = new SlotCrafting(getInternalPlayer().get(), craftMatrix, craftResult, 0, 0, 0);
        }

        if (!hasWork) {
            return;
        }

        int updateNext = update + (int) (internalStorage.currentPower() * 10) + 1;
        int updateThreshold = (update & ~15) + 16;
        update = Math.min(updateThreshold, updateNext);
        if ((update % UPDATE_TIME) == 0) {
            updateCrafting();
        }
        internalStorage.extractPower(getWorld(), 1.6, 1.6, false);
    }

    public int getProgressScaled(int i) {
        return (progress * i) / CRAFT_TIME;
    }

    /** Increment craft job, find recipes, produce output */
    private void updateCrafting() {
        progress += UPDATE_TIME;

        for (int i = 0; i < 9; i++) {
            bindingCounts[i] = 0;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack comparedStack = craftMatrix.getStackInSlot(i);
            if (comparedStack == null || comparedStack.getItem() == null) {
                bindings[i] = -1;
                continue;
            }

            if (bindings[i] == -1 || !StackHelper.isMatchingItem(inputInv.getStackInSlot(bindings[i]), comparedStack, true, true)) {
                boolean found = false;
                for (int j = 0; j < 9; j++) {
                    if (j == bindings[i]) {
                        continue;
                    }

                    ItemStack inputInvStack = inputInv.getStackInSlot(j);

                    if (StackHelper.isMatchingItem(inputInvStack, comparedStack, true, false) && inputInvStack.stackSize > bindingCounts[j]) {
                        found = true;
                        bindings[i] = j;
                        bindingCounts[j]++;
                        break;
                    }
                }
                if (!found) {
                    craftMatrix.isJammed = true;
                    progress = 0;
                    return;
                }
            } else {
                bindingCounts[bindings[i]]++;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (bindingCounts[i] > 0) {
                ItemStack stack = inputInv.getStackInSlot(i);
                if (stack != null && stack.stackSize < bindingCounts[i]) {
                    // Do not break progress yet, instead give it a chance to rebuild
                    // It will quit when trying to find a valid binding to "fit in"
                    for (int j = 0; j < 9; j++) {
                        if (bindings[j] == i) {
                            bindings[j] = -1;
                        }
                    }
                    return;
                }
            }
        }

        if (progress < CRAFT_TIME) {
            return;
        }

        progress = 0;

        craftMatrix.setUseBindings(true);
        ItemStack result = craftMatrix.getRecipeOutput();

        if (result != null && result.stackSize > 0) {
            ItemStack resultInto = resultInv.getStackInSlot(0);

            craftSlot.onPickupFromSlot(getInternalPlayer().get(), result);

            if (resultInto == null) {
                resultInv.setInventorySlotContents(0, result);
            } else {
                resultInto.stackSize += result.stackSize;
            }
        }

        craftMatrix.setUseBindings(false);
        craftMatrix.rebuildCache();
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_RESULT) {
            return false;
        }
        if (stack.getItem().hasContainerItem(stack)) {
            return false;
        }
        return true;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing face) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {
        return slot < 9;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, EnumFacing side) {
        return slot == SLOT_RESULT;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public IMjExternalStorage getMjStorage() {
        return externalStorage;
    }
}
