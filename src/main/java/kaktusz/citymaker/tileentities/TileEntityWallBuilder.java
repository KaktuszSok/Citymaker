package kaktusz.citymaker.tileentities;

import kaktusz.citymaker.capabilities.MachineEnergy;
import kaktusz.citymaker.capabilities.WallBuilderItemHandler;
import kaktusz.citymaker.containers.WallBuilderContainer;
import kaktusz.citymaker.containers.WallBuilderContainerGUI;
import kaktusz.citymaker.init.ModConfig;
import kaktusz.citymaker.util.IHasGUI;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TileEntityWallBuilder extends TileEntity implements ITickable, IHasGUI {
	public static final int INV_SIZE = 9*3;
	private static final Map<Capability<?>, Function<TileEntityWallBuilder, Object>> CAPABILITY_PROVIDERS = new HashMap<>();
	static {
		CAPABILITY_PROVIDERS.put(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, te -> te.inventory);
		CAPABILITY_PROVIDERS.put(CapabilityEnergy.ENERGY, te -> te.energy);
	}

	public final WallBuilderItemHandler inventory = new WallBuilderItemHandler(INV_SIZE);
	public final MachineEnergy energy = new MachineEnergy(ModConfig.wallBuilderFEPerBlock);

	private FakePlayer fakePlayerCache;

	@Override
	public void update() {

	}

	protected boolean needsEnergy() {
		return true;
	}

	protected boolean consumesSupplies() {
		return true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("inventory", inventory.serializeNBT());

		//noinspection ConstantConditions
		compound.setTag("energy", CapabilityEnergy.ENERGY.getStorage().writeNBT(
				CapabilityEnergy.ENERGY,
				energy,
				null));

		return super.writeToNBT(compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		inventory.deserializeNBT(compound.getCompoundTag("inventory"));

		NBTBase energyNBT = compound.getTag("energy");
		//noinspection ConstantConditions //allegedly never null, but on client-side is always null
		if(energyNBT != null) {
			CapabilityEnergy.ENERGY.getStorage().readNBT(CapabilityEnergy.ENERGY, energy, EnumFacing.NORTH, energyNBT);
		}

		super.readFromNBT(compound);
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, -1, this.getUpdateTag());
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound tag = super.getUpdateTag();
		return tag;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		super.onDataPacket(net, pkt);
		handleUpdateTag(pkt.getNbtCompound());
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return CAPABILITY_PROVIDERS.containsKey(capability) || super.hasCapability(capability, facing);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		Function<TileEntityWallBuilder, Object> cap = CAPABILITY_PROVIDERS.get(capability);
		return cap != null ? (T)cap.apply(this) : super.getCapability(capability, facing);
	}

	public boolean canInteractWith(EntityPlayer playerIn) {
		// If we are too far away from this tile entity you cannot use it
		return !isInvalid() && playerIn.getDistanceSq(pos.add(0.5D, 0.5D, 0.5D)) <= 8D*8D;
	}

	@Nonnull
	@Override
	public ITextComponent getDisplayName() {
		return new TextComponentTranslation("container.road_builder");
	}

	@Override
	public WallBuilderContainer getServerGuiContainer(EntityPlayer player) {
		return new WallBuilderContainer(player.inventory, this);
	}

	@Override
	public WallBuilderContainerGUI getClientGuiContainer(EntityPlayer player) {
		return new WallBuilderContainerGUI(this, getServerGuiContainer(player));
	}

	private FakePlayer getFakePlayer() {
		if(fakePlayerCache == null && !world.isRemote) {
			fakePlayerCache = FakePlayerFactory.getMinecraft((WorldServer)world);
		}

		return fakePlayerCache;
	}
}
