package tj.builder.handlers;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.common.blocks.wood.BlockGregLeaves;
import gregtech.common.blocks.wood.BlockGregLog;
import gregtech.common.blocks.wood.BlockGregSapling;
import gregtech.common.items.MetaItems;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.capability.IItemFluidHandlerInfo;
import tj.capability.TJCapabilities;
import tj.capability.impl.AbstractWorkableHandler;
import tj.util.ItemStackHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


public class FarmingStationWorkableHandler extends AbstractWorkableHandler<FarmingStationWorkableHandler> implements IItemFluidHandlerInfo {

    private static ItemStack RUBBER_REFERENCE;
    private final List<ItemStack> itemInputs = new ArrayList<>();
    private final List<ItemStack> itemOutputs = new ArrayList<>();
    private final Object2ObjectMap<String, ItemStack> itemType = new Object2ObjectOpenHashMap<>();
    private Supplier<IItemHandlerModifiable> toolInventory;
    private Supplier<IItemHandlerModifiable> fertilizerInventory;
    private BlockPos.MutableBlockPos posHarvester;
    private BlockPos pos;
    private int fertilizerChance;
    private int outputIndex;
    private int range;
    private int radius;

    public FarmingStationWorkableHandler(MetaTileEntity metaTileEntity) {
        super(metaTileEntity);
    }

    public FarmingStationWorkableHandler setToolInventory(Supplier<IItemHandlerModifiable> toolInventory) {
        this.toolInventory = toolInventory;
        return this;
    }

    public FarmingStationWorkableHandler setFertilizerInventory(Supplier<IItemHandlerModifiable> fertilizerInventory) {
        this.fertilizerInventory = fertilizerInventory;
        return this;
    }

    @Override
    public FarmingStationWorkableHandler initialize(int range) {
        super.initialize(range);
        this.range = range;
        this.radius = (int) Math.sqrt(range);
        this.fertilizerChance = this.tier.getAsInt() * 10;
        return this;
    }

    @Override
    protected boolean startRecipe() {
        this.maxProgress = this.range;
        this.energyPerTick = this.range / 4;
        return true;
    }

    @Override
    protected void progressRecipe(int progress) {
        super.progressRecipe(progress);
        if (this.progress > progress) {
            if (this.pos == null) {
                BlockPos pos = this.metaTileEntity.getPos();
                this.pos = new BlockPos(pos.getX() - (this.radius / 2), pos.getY(), pos.getZ() - (this.radius / 2));
            }
            if (this.posHarvester == null)
                this.posHarvester = new BlockPos.MutableBlockPos(this.pos);
            if (RUBBER_REFERENCE == null)
                RUBBER_REFERENCE = new ItemStack(Item.getByNameOrId("gregtech:meta_item_i"), 1, 32627);
            this.posHarvester.setPos(this.pos.getX() + (progress % this.radius), this.pos.getY(), this.pos.getZ() + (progress / this.radius));
            World world = this.metaTileEntity.getWorld();
            IBlockState state = world.getBlockState(this.posHarvester);
            Block block = state.getBlock();
            if (block instanceof IGrowable && this.fertilizerChance >= Math.random() * 100) {
                ItemStack stack = this.fertilizerInventory.get().getStackInSlot(0);
                if (!stack.isEmpty()) {
                    ((IGrowable) block).grow(world, world.rand, this.posHarvester, state);
                    stack.shrink(1);
                }
            }
            if (block == Blocks.AIR) {
                IBlockState itemState = null;
                ItemStack stack = ItemStack.EMPTY;
                IItemHandlerModifiable importItems = this.importItems.get();
                for (int i = 0; i < importItems.getSlots(); i++) {
                    ItemStack stack1 = importItems.getStackInSlot(i);
                    Block itemBlock = Block.getBlockFromItem(stack1.getItem());
                    if (itemBlock instanceof BlockSapling || itemBlock instanceof BlockGregSapling) {
                        itemState = itemBlock.getStateFromMeta(stack1.getMetadata());
                        stack = stack1;
                    }
                }
                if (itemState != null) {
                    ItemStack hoeStack = this.toolInventory.get().getStackInSlot(0);
                    if (!hoeStack.isEmpty()) {
                        hoeStack.damageItem(1, FakePlayerFactory.getMinecraft((WorldServer) world));
                        world.setBlockState(this.posHarvester, itemState);
                        stack.shrink(1);
                    }
                }
            } else this.harvestBlock(this.posHarvester);
        }
    }

    /**
     * Try to harvest a block. if successful, try to harvest the same kind of blocks in a 3x3 area. do the same again but above the block.
     * @param pos BlockPos
     */
    private void harvestBlock(BlockPos.MutableBlockPos pos) {
        ItemStack toolStack;
        double chance = 100;
        boolean harvestable = false;
        World world = this.metaTileEntity.getWorld();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BlockLog && !(toolStack = this.toolInventory.get().getStackInSlot(1)).isEmpty()) {
            toolStack.damageItem(1, FakePlayerFactory.getMinecraft((WorldServer) world));
            harvestable = true;
        } else if (block instanceof BlockGregLog && !(toolStack = this.toolInventory.get().getStackInSlot(1)).isEmpty()) {
            if (state.getValue(BlockGregLog.NATURAL))
                this.addItemDrop(RUBBER_REFERENCE.getItem(), 1 + world.rand.nextInt(2) , RUBBER_REFERENCE.getMetadata());
            toolStack.damageItem(1, FakePlayerFactory.getMinecraft((WorldServer) world));
            harvestable = true;
        } else if ((block instanceof BlockLeaves || block instanceof BlockGregLeaves) && !(toolStack = this.toolInventory.get().getStackInSlot(1)).isEmpty()) {
            toolStack.damageItem(1, FakePlayerFactory.getMinecraft((WorldServer) world));
            harvestable = true;
            chance = 5;
        }
        if (harvestable) {
            Item item = block.getItemDropped(state, world.rand, 0);
            if (item != Items.AIR && chance >= Math.random() * 100) {
                this.addItemDrop(item, 1, block.damageDropped(state));
            }
            world.destroyBlock(pos, false);
            BlockPos.MutableBlockPos harvester = new BlockPos.MutableBlockPos(pos);
            for (int x = -1; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    for (int z = -1; z < 2; z++) {
                        harvester.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                        this.harvestBlock(harvester);
                    }
                }
            }
        }
    }

    private void addItemDrop(Item item, int count, int meta) {
        String key = item.getRegistryName().toString() + ":" + meta;
        ItemStack stack = this.itemType.get(key);
        if (stack != null)
            stack.grow(count);
        else {
            ItemStack itemStack = new ItemStack(item, count, meta);
            this.itemType.put(key, itemStack);
            this.itemOutputs.add(itemStack);
        }
    }

    @Override
    protected boolean completeRecipe() {
        IItemHandlerModifiable importItems = this.importItems.get();
        IItemHandlerModifiable exportItems = this.exportItems.get();
        for (int i = 0; i < this.itemOutputs.size(); i++) {
            ItemStack stack = this.itemOutputs.get(i);
            Block block = Block.getBlockFromItem(stack.getItem());
            if (block instanceof BlockSapling || block instanceof BlockGregSapling) {
                this.itemOutputs.set(i, ItemStackHelper.insertIntoItemHandler(importItems, stack, false));
            }
        }
        for (int i = this.outputIndex; i < this.itemOutputs.size(); i++) {
            if (ItemStackHelper.insertIntoItemHandler(exportItems, this.itemOutputs.get(i), true).isEmpty()) {
                ItemStackHelper.insertIntoItemHandler(exportItems, this.itemOutputs.get(i), false);
                this.outputIndex++;
            } else return false;
        }
        this.outputIndex = 0;
        this.itemType.clear();
        this.itemOutputs.clear();
        return true;
    }

    @Override
    public List<ItemStack> getItemInputs() {
        return this.itemInputs;
    }

    @Override
    public List<ItemStack> getItemOutputs() {
        return this.itemOutputs;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound data = super.serializeNBT();
        NBTTagList inputList = new NBTTagList(), outputList = new NBTTagList();
        for (ItemStack stack : this.itemInputs)
            inputList.appendTag(stack.serializeNBT());
        for (ItemStack stack : this.itemOutputs)
            outputList.appendTag(stack.serializeNBT());
        data.setTag("inputList", inputList);
        data.setTag("outputList", outputList);
        data.setInteger("outputIndex", this.outputIndex);
        return data;
    }

    @Override
    public void deserializeNBT(NBTTagCompound data) {
        super.deserializeNBT(data);
        NBTTagList inputList = data.getTagList("inputList", 10), outputList = data.getTagList("outputList", 10);
        for (int i = 0; i < inputList.tagCount(); i++)
            this.itemInputs.add(new ItemStack(inputList.getCompoundTagAt(i)));
        for (int i = 0; i < outputList.tagCount(); i++)
            this.itemOutputs.add(new ItemStack(outputList.getCompoundTagAt(i)));
        this.outputIndex = data.getInteger("outputIndex");
    }

    @Override
    public <T> T getCapability(Capability<T> capability) {
        if (capability == TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING)
            return TJCapabilities.CAPABILITY_ITEM_FLUID_HANDLING.cast(this);
        return super.getCapability(capability);
    }
}
