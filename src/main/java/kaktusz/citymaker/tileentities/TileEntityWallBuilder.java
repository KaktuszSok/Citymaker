package kaktusz.citymaker.tileentities;

import com.google.common.collect.ImmutableSet;
import kaktusz.citymaker.blocks.BlockDirectional;
import kaktusz.citymaker.capabilities.MachineEnergy;
import kaktusz.citymaker.capabilities.WallBuilderItemHandler;
import kaktusz.citymaker.containers.WallBuilderContainer;
import kaktusz.citymaker.containers.WallBuilderContainerGUI;
import kaktusz.citymaker.init.ModConfig;
import kaktusz.citymaker.util.IHasGUI;
import kaktusz.citymaker.util.RotationUtils;
import kaktusz.citymaker.util.VFXUtils;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class TileEntityWallBuilder extends TileEntity implements ITickable, IHasGUI {

	private static class ColumnPreset {
		public final IBlockState bottomBlock;
		public final List<IBlockState> columnMain = new ArrayList<>(); //blocks right above the bottom block
		public final List<IBlockState> columnDecor = new ArrayList<>(); //blocks sticking out in front of the main column

		public ColumnPreset(World world, BlockPos bottomBlockPos, EnumFacing decorDirection) {
			bottomBlock = world.getBlockState(bottomBlockPos);
			BlockPos scanPos = bottomBlockPos.up();

			while(true) {
				IBlockState scannedStateMain = world.getBlockState(scanPos);
				if(!isValidBlock(scannedStateMain))
					break;

				columnMain.add(scannedStateMain); //add column block
				columnDecor.add(world.getBlockState(scanPos.add(decorDirection.getDirectionVec()))); //add decor in front of the column, even if its empty
				scanPos = scanPos.up();
			}
		}

		public int getHeight() {
			return columnMain.size();
		}
	}

	public static final int INV_SIZE = 9*3;
	private static final Map<Capability<?>, Function<TileEntityWallBuilder, Object>> CAPABILITY_PROVIDERS = new HashMap<>();
	static {
		CAPABILITY_PROVIDERS.put(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, te -> te.inventory);
		CAPABILITY_PROVIDERS.put(CapabilityEnergy.ENERGY, te -> te.energy);
	}
	private static final int MAX_RANGE = 48;

	public final WallBuilderItemHandler inventory = new WallBuilderItemHandler(INV_SIZE);
	public final MachineEnergy energy = new MachineEnergy(ModConfig.wallBuilderFEPerBlock*10);

	private FakePlayer fakePlayerCache;
	private EnumFacing facing;
	private int currStage = 0;
	private final Map<IBlockState, ColumnPreset> scannedColumnPresets = new HashMap<>();
	private BlockPos buildingPos;
	private EnumFacing nextPosDir;
	private final Set<BlockPos> builtOrInvalidColumnPositions = new HashSet<>();

	@Override
	public void update() {
		if(world.isRemote || world.isBlockPowered(pos) || consumesSupplies() && !inventory.canBuild()) //redstone = stop, no supplies = stop
			return;

		switch (currStage) {
			case 0:
				if(!tryUseEnergy())
					break;

				facing = world.getBlockState(pos).getValue(BlockDirectional.FACING);
				scanForColumnPresets();
				if(scannedColumnPresets.isEmpty())
					break;

				currStage = 1;
				buildingPos = pos.subtract(facing.getDirectionVec()).up();
				nextPosDir = facing.rotateY();
				builtOrInvalidColumnPositions.clear();
				break;
			case 1:
				if(!tryUseEnergy())
					break;
				if(!tryPlaceNextBlock())
					currStage = 2;
			default:
				break;
		}
	}

	protected boolean needsEnergy() {
		return true;
	}

	protected boolean consumesSupplies() {
		return true;
	}

	private void scanForColumnPresets() {
		EnumFacing right = facing.rotateY();
		EnumFacing left = right.getOpposite();
		scannedColumnPresets.clear();
		Queue<BlockPos> scanQueue = new LinkedList<>();
		scanQueue.add(pos.subtract(facing.getDirectionVec())); //start at block behind us
		Set<BlockPos> visited = new HashSet<>();
		Set<String> ignoredBlocks = ImmutableSet.copyOf(ModConfig.wallBuilderIgnoredBlocks);

		while (!scanQueue.isEmpty()) {
			BlockPos scanPos = scanQueue.poll();

			if(Math.abs(scanPos.getX() - pos.getX()) >= MAX_RANGE || Math.abs(scanPos.getZ() - pos.getZ()) >= MAX_RANGE) //too far
				continue;

			if(!visited.add(scanPos)) //already visited
				continue;

			IBlockState scannedState = world.getBlockState(scanPos);
			ResourceLocation blockRegName = scannedState.getBlock().getRegistryName();
			if(!isValidBlock(scannedState) || blockRegName != null && ignoredBlocks.contains(blockRegName.toString())) //bad block
				continue;

			ColumnPreset scannedColumn = new ColumnPreset(world, scanPos, facing);
			if(!scannedColumnPresets.containsKey(scannedState)) { //possibly a new column preset
				if (scannedColumn.getHeight() == 0) //not a column (or column "seed" block)
					continue;

				scannedColumnPresets.put(scannedState, scannedColumn);
			}

			scanQueue.add(scanPos.add(right.getDirectionVec()));
			scanQueue.add(scanPos.add(left.getDirectionVec()));
		}
	}

	/**
	 * Tries to place the next block that needs to be built.
	 * @return True if there is more work to be done.
	 */
	private boolean tryPlaceNextBlock() {
		BlockPos groundPos = new BlockPos(buildingPos.getX(), pos.getY(), buildingPos.getZ());
		ColumnPreset currentPreset = scannedColumnPresets.get(world.getBlockState(groundPos));
		if(currentPreset == null) { //something went wrong if this is null - skip this column
			return tryMoveToNextColumn();
		}

		int currY = buildingPos.getY() - groundPos.getY(); //starts at 1, as we begin to build from the block above the ground. Will not exceed preset height.
		if(currY > currentPreset.getHeight()) {
			return tryMoveToNextColumn();
		}

		IBlockState desiredState = currentPreset.columnMain.get(currY-1);
		int rotationsRequired = RotationUtils.getAmountOfYRotationsDelta(facing.rotateY(), nextPosDir);
		desiredState = RotationUtils.rotateAroundY(desiredState, rotationsRequired);
		boolean stuck = !tryPlaceBlock(buildingPos, desiredState);
		if(stuck)
			return true;

		IBlockState desiredDecorState = currentPreset.columnDecor.get(currY-1);
		desiredDecorState = RotationUtils.rotateAroundY(desiredDecorState, rotationsRequired);
		if(isValidBlock(desiredDecorState)) {
			stuck = !placeDecor(desiredDecorState);
			if (stuck)
				return true;
		}

		//alles gut! move to next block up in column (or next column, if we exceed the current one's height)
		buildingPos = buildingPos.up();
		return true;
	}

	/**
	 * Tries to find the next column to move to in the order forward, right, left.
	 */
	private boolean tryMoveToNextColumn() {
		BlockPos groundPos = new BlockPos(buildingPos.getX(), pos.getY(), buildingPos.getZ());
		for (int i = 0; i < 4; i++, nextPosDir = nextPosDir.rotateY()) {
			if(i == 2) //don't attempt to backtrack
				continue;

			BlockPos nextPos = groundPos.add(nextPosDir.getDirectionVec());
			if(Math.abs(nextPos.getX() - pos.getX()) >= MAX_RANGE || Math.abs(nextPos.getZ() - pos.getZ()) >= MAX_RANGE) { //too far
				continue;
			}
			if(!builtOrInvalidColumnPositions.add(nextPos)) { //we were here already
				continue;
			}
			IBlockState nextState = world.getBlockState(nextPos);
			ColumnPreset preset = scannedColumnPresets.get(nextState);
			if(preset == null) { //ground block is not a column seed
				continue;
			}

			//ground block is a column seed!
			buildingPos = nextPos.up(); //start building at the block above ground
			return true;
		}

		return false;
	}

	private boolean isPosOnColumn(BlockPos where) {
		BlockPos groundPos = new BlockPos(where.getX(), pos.getY(), where.getZ());
		IBlockState groundState = world.getBlockState(groundPos);
		return scannedColumnPresets.get(groundState) != null;
	}

	/**
	 * Tries to place a certain block at a certain position, taking inventory resources into account.
	 * @return True if the block was placed or skipped, false if the builder should get stuck at the current position (e.g. out of materials).
	 */
	private boolean tryPlaceBlock(BlockPos where, IBlockState desiredState) {
		IBlockState currState = world.getBlockState(where);
		if(!currState.getMaterial().isReplaceable()) { //there is already a block here - skip it.
			spawnParticlesAroundBlock(where, EnumParticleTypes.CRIT);
			return true;
		}

		if(currState == desiredState) { //block is already correct - skip it.
			spawnParticlesAroundBlock(where, EnumParticleTypes.CRIT);
			return true;
		}

		ItemStack requiredItem = desiredState.getBlock().getPickBlock(desiredState,
				new RayTraceResult(new Vec3d(0d, -1.0d, 0d), EnumFacing.UP, where),
				world, where, getFakePlayer());

		if(consumesSupplies() && !inventory.tryConsumeItemLike(requiredItem)) {
			spawnParticlesAroundBlock(where, EnumParticleTypes.REDSTONE);
			return false; //out of items - get stuck at this block.
		}

		//try place the block
		BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(
				new BlockSnapshot(world, where, currState),
				world.getBlockState(where.down()),
				getFakePlayer());
		MinecraftForge.EVENT_BUS.post(placeEvent);

		if(placeEvent.isCanceled()) { //can't place - skip this block
			return true;
		}

		SoundType soundType = desiredState.getBlock().getSoundType(desiredState, world, where, null);
		world.setBlockState(where, desiredState);
		world.playSound(null, where, soundType.getPlaceSound(), SoundCategory.BLOCKS, 1.0f, soundType.getPitch());
		spawnParticlesAroundBlock(where, EnumParticleTypes.CRIT_MAGIC);
		return true;
	}

	/**
	 * Attempts to place decor on the "left" of the current path, and, if successful, attempts the same in front.
	 * Decor is not placed if the position belongs to a column.
	 * @return True if the decor was placed successfully or skipped, false if the builder should get stuck (e.g. out of materials)
	 */
	private boolean placeDecor(IBlockState desiredDecorState) {
		//try place decor on left
		BlockPos leftPos = buildingPos.add(nextPosDir.rotateYCCW().getDirectionVec());
		if(isPosOnColumn(leftPos)) {
			return true; //column - skip
		}
		if(!tryUseEnergy() || !tryPlaceBlock(leftPos, desiredDecorState))
			return false; //no energy or materials - stuck

		//if successful, try the same on the front
		BlockPos frontPos = buildingPos.add(nextPosDir.getDirectionVec());
		if(isPosOnColumn(frontPos)) {
			return true; //column - skip
		}
		if(!tryUseEnergy() || !tryPlaceBlock(frontPos, RotationUtils.rotateAroundY(desiredDecorState, 1)))
			return false; //no energy or materials - stuck

		return true; //success!
	}

	public static boolean isValidBlock(IBlockState state) {
		return state.getMaterial().blocksMovement();
	}

	private boolean tryUseEnergy() {
		return !needsEnergy() || energy.tryConsumeEnergy(ModConfig.wallBuilderFEPerBlock);
	}

	private void spawnParticlesAroundBlock(BlockPos where, EnumParticleTypes particleType) {
		VFXUtils.serverSpawnParticle(world, where.getX()+0.5D, where.getY()+0.5D, where.getZ(), particleType, true);
		VFXUtils.serverSpawnParticle(world, where.getX()-0.5D, where.getY()+0.5D, where.getZ(), particleType, true);
		VFXUtils.serverSpawnParticle(world, where.getX(), where.getY()+0.5D, where.getZ()+0.5D, particleType, true);
		VFXUtils.serverSpawnParticle(world, where.getX(), where.getY()+0.5D, where.getZ()-0.5D, particleType, true);
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
		return new TextComponentTranslation("container.wall_builder");
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
