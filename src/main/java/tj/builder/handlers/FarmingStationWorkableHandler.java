package tj.builder.handlers;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GTLog;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import tj.capability.IItemFluidHandlerInfo;
import tj.capability.TJCapabilities;
import tj.capability.impl.AbstractWorkableHandler;
import tj.util.ItemStackHelper;

import java.util.ArrayList;
import java.util.List;


public class FarmingStationWorkableHandler extends AbstractWorkableHandler<IItemHandlerModifiable, FluidTank> implements IItemFluidHandlerInfo {

    private final List<ItemStack> itemInputs = new ArrayList<>();
    private final List<ItemStack> itemOutputs = new ArrayList<>();
    private final Object2ObjectMap<Block, ItemStack> blockType = new Object2ObjectOpenHashMap<>();
    private BlockPos.MutableBlockPos posHarvester;
    private BlockPos pos;
    private int outputIndex;
    private int range;
    private int radius;
    private long euBonus;

    public FarmingStationWorkableHandler(MetaTileEntity metaTileEntity) {
        super(metaTileEntity);
    }

    @Override
    public void initialize(int range) {
        super.initialize(range);
        this.range = range;
        this.radius = (int) Math.sqrt(range);
        this.euBonus = this.radius / 10;
    }

    @Override
    protected boolean startRecipe() {
        this.maxProgress = this.calculateOverclock(30 * this.euBonus, this.range, 2.8F);
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
            this.posHarvester.setPos(this.pos.getX() + (progress % this.radius), this.pos.getY(), this.pos.getZ() + (progress / this.radius));
            World world = this.metaTileEntity.getWorld();
            IBlockState state = world.getBlockState(this.posHarvester);
            Block block = state.getBlock();
            if (block instanceof BlockLog) {
                ItemStack stack = this.blockType.get(block);
                if (stack != null)
                    stack.grow(1);
                else {
                    ItemStack itemStack = new ItemStack(block, 1, state.getBlock().getMetaFromState(state));
                    this.blockType.put(block, itemStack);
                    this.itemOutputs.add(itemStack);
                }
                this.metaTileEntity.getWorld().destroyBlock(this.posHarvester, false);
            }
        }
    }



    @Override
    protected boolean completeRecipe() {
        for (int i = this.outputIndex; i < this.itemOutputs.size(); i++) {
            if (ItemStackHelper.insertIntoItemHandler(this.exportItems.get(), this.itemOutputs.get(i), true).isEmpty()) {
                ItemStackHelper.insertIntoItemHandler(this.exportItems.get(), this.itemOutputs.get(i), false);
                this.outputIndex++;
            } else return false;
        }
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
