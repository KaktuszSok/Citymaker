package kaktusz.citymaker.tileentities;

import kaktusz.citymaker.capabilities.MachineEnergy;
import kaktusz.citymaker.capabilities.RoadBuilderItemHandler;
import kaktusz.citymaker.containers.RoadBuilderContainer;
import kaktusz.citymaker.containers.RoadBuilderContainerGUI;
import kaktusz.citymaker.init.ModConfig;
import kaktusz.citymaker.rendering.MapTexture;
import kaktusz.citymaker.util.IHasGUI;
import kaktusz.citymaker.util.Roadmap;
import kaktusz.citymaker.util.VFXUtils;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TileEntityRoadBuilder extends TileEntity implements ITickable, IHasGUI {

	public static final int INV_SIZE = 3;
	private static final Map<Capability<?>, Function<TileEntityRoadBuilder, Object>> CAPABILITY_PROVIDERS = new HashMap<>();
	static {
		CAPABILITY_PROVIDERS.put(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, te -> te.inventory);
		CAPABILITY_PROVIDERS.put(CapabilityEnergy.ENERGY, te -> te.energy);
	}
	private static final int MAX_BLOCKS_PER_TICK = 16;

	public final RoadBuilderItemHandler inventory = new RoadBuilderItemHandler(INV_SIZE);
	public final MachineEnergy energy = new MachineEnergy(ModConfig.roadBuilderFEPerBlock*MAX_BLOCKS_PER_TICK);
	private int gridIdx = 0;
	private final int[] xzArray = new int[2]; //re-usable array for storing x and z tuple

	private String roadmapStr = null;
	private Roadmap roadmapCache;
	private FakePlayer fakePlayerCache;
	private int scanCooldown = 0;
	private MapTexture mapTexture;
	private boolean isMapTextureFresh;

	@Override
	public void update() {
		if(world.isRemote)
			return;

		if(scanCooldown > 0) {
			scanCooldown--;
			return;
		}

		if(world.isBlockPowered(pos)) //redstone = stop
			return;

		Roadmap roadmap = null;
		int statusParticleTypeThisTick = 0; //0 = none, 1 = processed block but didn't need to place, 2 = placed, 3 = out of materials
		for (
			int iterationsThisTick = 0;
					(!needsEnergy() || energy.canConsumeEnergy(ModConfig.roadBuilderFEPerBlock))
					&& iterationsThisTick < MAX_BLOCKS_PER_TICK
					&& inventory.canBuild();
			iterationsThisTick++, gridIdx++
		) {
			if(iterationsThisTick == 0)
				roadmap = getRoadmap();

			if(roadmap == null) //string was null or invalid
				return;

			energy.tryConsumeEnergy(ModConfig.roadBuilderFEPerBlock);
			markDirty();

			//scan chunk by chunk
			gridIdx = gridIdx % roadmap.getArea();
			coordinatesFromGridIndex(gridIdx, roadmap, xzArray);
			int x = xzArray[0];
			int z = xzArray[1];

			ItemStack desiredItem = getSelectedItemStack(roadmap.statemap[x][z]);
			if(desiredItem == null) {
				if(statusParticleTypeThisTick < 1)
					statusParticleTypeThisTick = 1;
				continue; //doesn't matter what is placed at this position
			}

			int chunksWidth = roadmap.statemap.length/16;
			int xChunkOffset = chunksWidth/2;
			int startX = ((pos.getX() >> 4) - xChunkOffset) << 4;
			int startZ = ((pos.getZ() >> 4) << 4) - 1;
			BlockPos worldPos = getTopBlock(startX+x, startZ-z);
			if(worldPos.getY() < 0) {
				if(statusParticleTypeThisTick < 1)
					statusParticleTypeThisTick = 1;
				continue; //no terrain to replace
			}

			if(desiredItem.isEmpty() || desiredItem.getCount() <= 1) {
				if(statusParticleTypeThisTick != 2) //don't show out of supply particles if we've successfully placed anything this tick
					statusParticleTypeThisTick = 3;
				VFXUtils.serverSpawnParticle(world, worldPos, EnumParticleTypes.REDSTONE, true);
				break; //don't have materials to build this cell type - terminate the tick and stay on this block
			}

			Block desiredBlock = itemStackToBlock(desiredItem);
			if(desiredBlock == null) {
				if(statusParticleTypeThisTick != 2)
					statusParticleTypeThisTick = 3;
				VFXUtils.serverSpawnParticle(world, worldPos, EnumParticleTypes.REDSTONE, true);
				break; //this item is not a valid block
			}

			FakePlayer fakePlayer = getFakePlayer();
			IBlockState desiredState = desiredBlock.getStateForPlacement(
					world,
					worldPos,
					EnumFacing.UP,
					0f,
					0.5f,
					0f,
					desiredItem.getMetadata(),
					fakePlayer,
					EnumHand.MAIN_HAND);
			if(!desiredState.getMaterial().isSolid() || !isValidTerrain(desiredState, worldPos)) {
				if(statusParticleTypeThisTick != 2)
					statusParticleTypeThisTick = 3;
				VFXUtils.serverSpawnParticle(world, worldPos, EnumParticleTypes.REDSTONE, true);
				break; //block is not valid road block
			}

			IBlockState currState = world.getBlockState(worldPos);
			if(currState == desiredState) { //block already correct
				VFXUtils.serverSpawnParticle(world, worldPos, EnumParticleTypes.CRIT, true);
				continue;
			}
			if(currState.getBlockHardness(world, worldPos) < 0f && !currState.getMaterial().isReplaceable()) { //block is indestructible
				VFXUtils.serverSpawnParticle(world, worldPos, EnumParticleTypes.CRIT, true);
				continue;
			}

			BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(world, worldPos, currState, fakePlayer);
			MinecraftForge.EVENT_BUS.post(breakEvent);
			if(breakEvent.isCanceled())
				continue; //can't break

			BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(
					new BlockSnapshot(world, worldPos, currState),
					world.getBlockState(worldPos.down()),
					fakePlayer);
			MinecraftForge.EVENT_BUS.post(placeEvent);
			if(placeEvent.isCanceled())
				continue; //can't place

			SoundType soundType = desiredBlock.getSoundType(desiredState, world, worldPos, null);
			world.setBlockState(worldPos, desiredState);
			world.playSound(null, worldPos, soundType.getPlaceSound(), SoundCategory.BLOCKS, 1.0f, soundType.getPitch());
			if(statusParticleTypeThisTick < 2)
				statusParticleTypeThisTick = 2;
			VFXUtils.serverSpawnParticle(world, worldPos, EnumParticleTypes.CRIT_MAGIC, true);

			if(consumesSupplies())
				desiredItem.shrink(1);
		}

		EnumParticleTypes statusParticles;
		switch (statusParticleTypeThisTick) {
			case 1:
				statusParticles = EnumParticleTypes.CRIT;
				break;
			case 2:
				statusParticles = EnumParticleTypes.CRIT_MAGIC;
				break;
			case 3:
				statusParticles = EnumParticleTypes.REDSTONE;
				break;
			default:
				return;
		}
		coordinatesFromGridIndex(gridIdx % roadmap.getArea(), roadmap, xzArray);
		double xFraction = (double)xzArray[0] / roadmap.statemap.length;
		double zFraction = (double)xzArray[1] / roadmap.statemap[0].length;
		VFXUtils.serverSpawnParticle(world, pos.getX() + xFraction, pos.getY() + 1.25D, pos.getZ() + 1 - zFraction, statusParticles, false);
	}

	protected boolean needsEnergy() {
		return true;
	}

	protected boolean consumesSupplies() {
		return true;
	}

	private void coordinatesFromGridIndex(int gridIdx, Roadmap roadmap, int[] output) {
		int chunkIdx = gridIdx / (16*16);
		int blockIdx = gridIdx % (16*16);
		int cx = chunkIdx % (roadmap.statemap.length/16);
		int cz = chunkIdx / (roadmap.statemap.length/16);
		int bx = blockIdx % 16;
		int bz = blockIdx / 16;
		int x = cx*16 + bx;
		int z = cz*16 + bz;
		output[0] = x;
		output[1] = z;
	}

	private BlockPos getTopBlock(int x, int z) {
		BlockPos blockpos = new BlockPos(x, 255, z);
		Chunk chunk = world.getChunkFromBlockCoords(blockpos);
		BlockPos blockpos1;

		for (blockpos = new BlockPos(x, chunk.getTopFilledSegment() + 16, z); blockpos.getY() >= 0; blockpos = blockpos1)
		{
			blockpos1 = blockpos.down();
			IBlockState state = chunk.getBlockState(blockpos1);

			if (isValidTerrain(state, blockpos1))
			{
				break;
			}
		}

		return blockpos.down();
	}

	private boolean isValidTerrain(IBlockState state, BlockPos blockpos) {
		return (state.getMaterial().blocksMovement() || state.getMaterial().isLiquid())
				&& !state.getBlock().isLeaves(state, world, blockpos)
				&& !state.getBlock().isFoliage(world, blockpos)
				&& !state.getBlock().isWood(world, blockpos);
	}

	@Nullable
	private ItemStack getSelectedItemStack(byte roadmapByte) {
		switch (Roadmap.CellState.fromByte(roadmapByte)) {
			case FOOTPATH:
				return inventory.getStackInSlot(2);
			case ROAD:
				return inventory.getStackInSlot(1);
			case CENTRELINE:
				return inventory.getStackInSlot(0);
		}
		return null;
	}

	@Nullable
	public Block itemStackToBlock(@Nonnull ItemStack stack) {
		if(stack.isEmpty()) return null;
		Block result = Block.getBlockFromItem(stack.getItem());
		if(result == Blocks.AIR)
			return null;
		else return result;
	}

	public Roadmap scanArea(int chunksX, int chunksZ) {
		return scanArea(chunksX, chunksZ, false);
	}
	public Roadmap scanArea(int chunksX, int chunksZ, boolean bypassCooldown) {
		if(!bypassCooldown && scanCooldown > 0)
			return null;

		scanCooldown = chunksX*chunksZ/8; //cooldown 1 tick per 8 chunks scanned. TODO: config?

		Roadmap roadmap = new Roadmap();
		roadmap.initialise(chunksX*16, chunksZ*16);

		int xChunkOffset = chunksX/2;
		int startX = ((pos.getX() >> 4) - xChunkOffset) << 4;
		int startZ = ((pos.getZ() >> 4) << 4) - 1;
		for (int x = 0; x < chunksX*16; x++) {
			for (int z = 0; z < chunksZ*16; z++) {
				BlockPos worldPos = getTopBlock(startX+x, startZ-z);
				if(worldPos.getY() < 0) {
					roadmap.statemap[x][z] = Roadmap.CellState.EMPTY.toByte();
					roadmap.heightmap[x][z] = -1024;
					continue;
				}

				roadmap.statemap[x][z] = getMatchingState(worldPos).toByte();
				roadmap.heightmap[x][z] = (short)worldPos.getY();
				roadmap.terrainmap[x][z] = world.getBlockState(worldPos).getMaterial().isSolid();
			}
		}
		roadmap.hasTerrainData = true;

		return roadmap;
	}

	private Roadmap.CellState getMatchingState(BlockPos worldPos) {
		if (checkIfBlockMatchesCell(Roadmap.CellState.FOOTPATH, worldPos))
			return Roadmap.CellState.FOOTPATH;
		if (checkIfBlockMatchesCell(Roadmap.CellState.ROAD, worldPos))
			return Roadmap.CellState.ROAD;
		if (checkIfBlockMatchesCell(Roadmap.CellState.CENTRELINE, worldPos))
			return Roadmap.CellState.CENTRELINE;

		return Roadmap.CellState.EMPTY;
	}

	private boolean checkIfBlockMatchesCell(Roadmap.CellState cell, BlockPos worldPos) {
		ItemStack cellItem = getSelectedItemStack(cell.toByte());
		if(cellItem == null || cellItem.isEmpty())
			return false;
		Block cellBlock = itemStackToBlock(cellItem);
		if(cellBlock == null)
			return false;
		IBlockState cellState = cellBlock.getStateForPlacement(
				world,
				worldPos,
				EnumFacing.UP,
				0f,
				0.5f,
				0f,
				cellItem.getMetadata(),
				getFakePlayer(),
				EnumHand.MAIN_HAND);
		if(!cellState.getMaterial().isSolid() || !isValidTerrain(cellState, worldPos))
			return false;

		return cellState == world.getBlockState(worldPos);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("inventory", inventory.serializeNBT());

		//noinspection ConstantConditions
		compound.setTag("energy", CapabilityEnergy.ENERGY.getStorage().writeNBT(
				CapabilityEnergy.ENERGY,
				energy,
				null));

		compound.setTag("roadmap", new NBTTagString(getSavedRoadmapString(false)));

		compound.setTag("gridIdx", new NBTTagInt(gridIdx));

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

		setRoadmap(compound.getString("roadmap"), false);

		gridIdx = compound.getInteger("gridIdx");

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

		tag.setTag("roadmap", new NBTTagString(getSavedRoadmapString(true)));
		return tag;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag) {
		super.handleUpdateTag(tag);
		setRoadmap(tag.getString("roadmap"), false);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		super.onDataPacket(net, pkt);
		handleUpdateTag(pkt.getNbtCompound());

	}

	@Nonnull
	private String getSavedRoadmapString(boolean includeTerrainData) {
		String roadmapStrSaved;
		Roadmap roadmap = getRoadmap();
		if(roadmap == null) {
			roadmapStrSaved = "";
		} else {
			roadmapStrSaved = Roadmap.toBase64(roadmap, includeTerrainData);
		}
		if(roadmapStrSaved == null)
			roadmapStrSaved = "";
		return roadmapStrSaved;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return CAPABILITY_PROVIDERS.containsKey(capability) || super.hasCapability(capability, facing);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		Function<TileEntityRoadBuilder, Object> cap = CAPABILITY_PROVIDERS.get(capability);
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
	public RoadBuilderContainer getServerGuiContainer(EntityPlayer player) {
		return new RoadBuilderContainer(player.inventory, this);
	}

	@Override
	public RoadBuilderContainerGUI getClientGuiContainer(EntityPlayer player) {
		return new RoadBuilderContainerGUI(this, getServerGuiContainer(player));
	}

	public String getRoadmapStr() {
		return roadmapStr;
	}

	@Nullable
	public Roadmap getRoadmap() {
		if(StringUtils.isNullOrEmpty(roadmapStr))
			return null;

		if(roadmapCache == null) {
			roadmapCache = Roadmap.fromBase64(roadmapStr);
			if(roadmapCache == null) {
				roadmapStr = null; //clear string if it can't be made into a valid roadmap
			}
			else if(!roadmapCache.hasTerrainData) { //add terrain data if not present in string
				Roadmap scannedMap = scanArea(roadmapCache.heightmap.length/16, roadmapCache.heightmap[0].length/16);
				if(scannedMap != null) {
					Roadmap.mergeGridData(roadmapCache, scannedMap);
					roadmapCache = scannedMap;
				}
			}
		}

		return roadmapCache;
	}

	public void setRoadmap(@Nullable String roadmapStr, boolean updateClients) {
		this.roadmapStr = roadmapStr;
		roadmapCache = null;
		gridIdx = 0;
		isMapTextureFresh = false;
		markDirty();

		if(updateClients) {
			updateRoadmapForClients();
		}
	}

	public void setRoadmap(@Nonnull Roadmap roadmap, boolean updateClients) {
		this.roadmapStr = Roadmap.toBase64(roadmap);
		roadmapCache = roadmap;
		gridIdx = 0;
		isMapTextureFresh = false;
		markDirty();

		if(updateClients) {
			updateRoadmapForClients();
		}
	}

	private void updateRoadmapForClients() {
		//world.markBlockRangeForRenderUpdate(pos, pos);
		IBlockState state = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, state, state, 2);
		world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
		markDirty();
	}

	private FakePlayer getFakePlayer() {
		if(fakePlayerCache == null && !world.isRemote) {
			fakePlayerCache = FakePlayerFactory.getMinecraft((WorldServer)world);
		}

		return fakePlayerCache;
	}

	public MapTexture getGuiMapTexture() {
		if (!isMapTextureFresh) {
			if(mapTexture != null)
				mapTexture.deleteGlTexture();
			mapTexture = new MapTexture(getRoadmap());
			isMapTextureFresh = true;
		}
		return mapTexture;
	}
}
