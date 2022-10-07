package kaktusz.citymaker.items;

import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.init.ModItems;
import kaktusz.citymaker.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.*;

public class ItemRoadStaff extends Item implements IHasModel {
	/**
	 * Maximum distance between the centre of the road and the edge of it, including the centre block itself.
	 */
	private static final int MAX_ROAD_HALFWIDTH = 12;
	private static final int MAX_ROAD_LENGTH = 512;

	private static class PlannedRoadBlock {
		public IBlockState state;
		public int yPos;
		public double distanceSqrFromMidline = Float.MAX_VALUE;
	}

	public ItemRoadStaff(String name, CreativeTabs tab) {
		setUnlocalizedName(name);
		setRegistryName(name);
		setCreativeTab(tab);

		ModItems.ITEMS.add(this);
	}

	@Override
	public void registerModels() {
		Citymaker.PROXY.registerItemRenderer(this, 0, "inventory");
	}

	//TODO shift+leftclick = clear
	//TODO auto-inherit tangent from previous curve
	//TODO visualise current state
	//TODO preview road and deleted blocks
	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(player.isSneaking()) {
			return shiftRightClick(worldIn, player, hand, pos);
		}

		return rightClick(worldIn, player, hand, pos);
	}

	private EnumActionResult rightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn, BlockPos posIn) {
		ItemStack stack = playerIn.getHeldItem(handIn);
		NBTTagCompound nbt = getOrCreateNBT(stack);

		if(!nbt.hasKey("palette")) {
			if(worldIn.isRemote)
				MessageUtils.sendErrorMessage(playerIn, "road_staff_no_palette");
			return EnumActionResult.FAIL;
		}

		if(!nbt.hasKey("tangentIn")) {
			nbt.setTag("tangentIn", NBTUtil.createPosTag(posIn));
			return EnumActionResult.SUCCESS;
		}
		if(!nbt.hasKey("startPos")) {
			nbt.setTag("startPos", NBTUtil.createPosTag(posIn));
			return EnumActionResult.SUCCESS;
		}
		BlockPos tangentIn = NBTUtil.getPosFromTag(nbt.getCompoundTag("tangentIn"));
		BlockPos startPos = NBTUtil.getPosFromTag(nbt.getCompoundTag("startPos"));
		BlockPos endPos = posIn;

		boolean straight = tangentIn.getX() == startPos.getX() && tangentIn.getZ() == startPos.getZ();
		if(!straight) {
			Vec2f tangent = new Vec2f(startPos.getX() - tangentIn.getX(), startPos.getZ() - tangentIn.getZ());
			Vec2f endpointsDir = new Vec2f(endPos.getX() - startPos.getX(), endPos.getZ() - startPos.getZ());
			float deltaAngle = GeometryUtils.signedAngle(tangent, endpointsDir);
			straight = deltaAngle == 0f || Float.isNaN(deltaAngle);
		}
		List<BlockPos> points = straight ? //TODO use vec3d instead? to fix accuracy, idk if it'll help tho
				getPointsStraight(worldIn, playerIn, startPos, endPos)
				: getPointsCurved(worldIn, playerIn, tangentIn, startPos, endPos);
		if(points == null) {
			return EnumActionResult.FAIL;
		}
		nbt.removeTag("tangentIn");
		nbt.removeTag("startPos");

		List<IBlockState> palette = getStatesFromNBT(nbt.getTagList("palette", Constants.NBT.TAG_STRING));
		if(palette.isEmpty())
			return EnumActionResult.FAIL;
		Map<Vec2i, PlannedRoadBlock> plannedBlocks = new HashMap<>();
		int roadStretch = palette.size()-1; //how far the road will stretch in each direction at each point (its half-width not including centreline)
		for (int i = 0; i < points.size(); i++) {
			for (int xOffset = -roadStretch; xOffset <= roadStretch; xOffset++) {
				for (int zOffset = -roadStretch; zOffset <= roadStretch; zOffset++) {
					double distSqr = xOffset*xOffset + zOffset*zOffset;
					if(distSqr >= palette.size()*palette.size()) {
						continue;
					}
					BlockPos point = points.get(i);
					Vec2i worldPosXZ = new Vec2i(point.getX() + xOffset, point.getZ() + zOffset);
					PlannedRoadBlock plan = plannedBlocks.get(worldPosXZ);
					if(plan == null) {
						plan = new PlannedRoadBlock();
						plannedBlocks.put(worldPosXZ, plan);
						plan.distanceSqrFromMidline = Float.POSITIVE_INFINITY;
					}
					if(plan.distanceSqrFromMidline <= distSqr) {
						continue;
					}
					plan.distanceSqrFromMidline = distSqr;
					plan.yPos = (int)Math.round(MathsUtils.lerp(startPos.getY(), endPos.getY(), (double)i/points.size()));
					int dist = Math.min((int)Math.sqrt(distSqr), palette.size()-1);
					plan.state = palette.get(dist);
				}
			}
		}
		Map<IBlockState, Integer> priorityMap = new HashMap<>();
		for (int i = 0; i < palette.size(); i++) {
			priorityMap.putIfAbsent(palette.get(i), i);
		}
		plannedBlocks.forEach((xz, pb) -> {
			for (int h = 5; h >= 0; h--) {
				BlockPos placePos = new BlockPos(xz.x, pb.yPos + h, xz.y);
				IBlockState curr = worldIn.getBlockState(placePos);
				int currPriority = priorityMap.computeIfAbsent(curr, b ->
						b.getBlockHardness(worldIn, placePos) < 0f && !b.getMaterial().isReplaceable() ?
								-1 : Integer.MAX_VALUE); //max priority if indestructible, otherwise min priority
				int newPriority = h == 0 ? priorityMap.get(pb.state) : -1; //clearing space above the road has maximum priority
				if(newPriority >= currPriority) {
					continue; //skip if we are trying to replace a higher priority (smaller priority number) block
				}
				worldIn.setBlockState(placePos, h == 0 ? pb.state : Blocks.AIR.getDefaultState(), 2);
			}
		});

		stack.setTagCompound(nbt);
		return EnumActionResult.SUCCESS;
	}

	private List<BlockPos> getPointsStraight(World worldIn, EntityPlayer playerIn, BlockPos startPos, BlockPos endPos) {
		List<BlockPos> points = new ArrayList<>();
		double dx = endPos.getX() - startPos.getX();
		double dy = endPos.getY() - startPos.getY();
		double dz = endPos.getZ() - startPos.getZ();
		double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
		if(dist > MAX_ROAD_LENGTH) {
			if(worldIn.isRemote)
				MessageUtils.sendErrorMessage(playerIn, "road_staff_too_long", MAX_ROAD_LENGTH, dist);
			return null;
		}
		//calculate normalised distances
		double nx = dx / dist;
		double ny = dy / dist;
		double nz = dz / dist;
		//initialise offsets
		double ox = 0, oy = 0, oz = 0;
		for (double travelled = 0d; travelled <= dist; travelled += 1d) {
			BlockPos point = new BlockPos(startPos.getX() + ox, startPos.getY() + oy, startPos.getZ() + oz);
			points.add(point);
			ox += nx;
			oy += ny;
			oz += nz;
		}
		return points;
	}

	private List<BlockPos> getPointsCurved(World worldIn, EntityPlayer playerIn, BlockPos tangentIn, BlockPos startPos, BlockPos endPos) {
		Vec2f tangent = new Vec2f(startPos.getX() - tangentIn.getX(), startPos.getZ() - tangentIn.getZ());
		@SuppressWarnings("SuspiciousNameCombination")
		Vec2f tangentPerp = new Vec2f(tangent.y, -tangent.x);
		Vec2f endpointsDir = new Vec2f(endPos.getX() - startPos.getX(), endPos.getZ() - startPos.getZ());
		Vec2f endpointsMid = new Vec2f(
				(startPos.getX() + endPos.getX())/2f,
				(startPos.getZ() + endPos.getZ())/2f
		);
		@SuppressWarnings("SuspiciousNameCombination")
		Vec2f endpointsPerp = new Vec2f(endpointsDir.y, -endpointsDir.x);
		Vec2f centrePos = GeometryUtils.findLinesIntersect(
				new Vec2f(startPos.getX(), startPos.getZ()),
				tangentPerp, endpointsMid, endpointsPerp);
		float dx = startPos.getX() - centrePos.x;
		float dz = startPos.getZ() - centrePos.y;
		float radius = (float)Math.sqrt(dx*dx + dz*dz);
		Vec2f centreToStart = new Vec2f(dx, dz);
		Vec2f centreToEnd = new Vec2f(endPos.getX() - centrePos.x, endPos.getZ() - centrePos.y);
		float deltaAngle = GeometryUtils.deltaAngle360(centreToStart, centreToEnd);
		boolean invertAngle = GeometryUtils.signedAngle(tangent, endpointsDir) <= 0;
		double adjustedDeltaAngle = invertAngle ? 360f - deltaAngle : deltaAngle;
		float startAngle = GeometryUtils.deltaAngle360(Vec2f.UNIT_X, centreToStart);
		double circumference = 2*Math.PI*radius;
		double anglePerMetre = 360f/circumference;
		if(adjustedDeltaAngle/anglePerMetre > MAX_ROAD_LENGTH) {
			if(worldIn.isRemote)
				MessageUtils.sendErrorMessage(playerIn, "road_staff_too_long", MAX_ROAD_LENGTH, adjustedDeltaAngle/anglePerMetre);
			return null;
		}

		List<BlockPos> points = new ArrayList<>();
		MutableAABB circleBB = new MutableAABB(startPos.getX(), startPos.getY(), startPos.getZ());
		circleBB.addPoint(endPos.getX(), endPos.getY(), endPos.getZ());
		for (double a = 0d; a < adjustedDeltaAngle; a += anglePerMetre) {
			double trueAngle = invertAngle ? startAngle - a : startAngle + a;
			trueAngle = Math.toRadians(trueAngle);
			BlockPos point = new BlockPos(
					centrePos.x + Math.cos(trueAngle)*radius,
					startPos.getY(),
					centrePos.y + Math.sin(trueAngle)*radius
			);
			if(a % 90d < anglePerMetre) {
				circleBB.addPoint(point.getX(), point.getY(), point.getZ());
			}
			points.add(point);
		}
		return points;
	}

	private List<IBlockState> getStatesFromNBT(NBTTagList paletteNBT) {
		List<IBlockState> result = new ArrayList<>();
		for (NBTBase nbtBase : paletteNBT) {
			String string = ((NBTTagString)nbtBase).getString();
			try {
				String[] split = string.split("#");
				Block block = Block.getBlockFromName(split[0]);
				Objects.requireNonNull(block);
				IBlockState state = block.getStateFromMeta(Integer.parseInt(split[1]));
				result.add(state);
			}
			catch (IndexOutOfBoundsException | NullPointerException | NumberFormatException e) { //something wrong with the nbt
				result.clear();
				return result;
			}
		}

		return result;
	}

	private EnumActionResult shiftRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn, BlockPos posIn) {
		ItemStack stack = playerIn.getHeldItem(handIn);
		NBTTagCompound nbt = getOrCreateNBT(stack);

		if(!nbt.hasKey("paletteStart")) { //start palette choice if we aren't choosing already
			nbt.removeTag("palette");
			nbt.removeTag("tangentIn");
			nbt.removeTag("startPos");
			nbt.setTag("paletteStart", NBTUtil.createPosTag(posIn));
			stack.setTagCompound(nbt);
			return EnumActionResult.SUCCESS;
		}
		//we are already choosing - finish palette choice with this click
		BlockPos startPos = NBTUtil.getPosFromTag(nbt.getCompoundTag("paletteStart"));
		nbt.removeTag("paletteStart");
		stack.setTagCompound(nbt);
		int dx = posIn.getX() - startPos.getX();
		int dz = posIn.getZ() - startPos.getZ();
		if(startPos.getY() != posIn.getY() || (dx != 0 && dz != 0)) {
			if(worldIn.isRemote)
				MessageUtils.sendErrorMessage(playerIn, "palette_not_in_line");
			return EnumActionResult.FAIL; //start and end must be in line
		}
		int width = Math.abs(dx) + Math.abs(dz);
		width += 1;
		if(width > MAX_ROAD_HALFWIDTH) {
			if(worldIn.isRemote)
				MessageUtils.sendErrorMessage(playerIn, "palette_too_wide", MAX_ROAD_HALFWIDTH);
			return EnumActionResult.FAIL; //width must be odd
		}

		//scan blocks selected and save them to nbt list
		//at least one of x- or z-factor is 0, and the other is 1 or -1 (or 0).
		int xFactor = Integer.signum(dx);
		int zFactor = Integer.signum(dz);
		NBTTagList paletteTag = new NBTTagList();
		List<ITextComponent> blockNames = new ArrayList<>(width);
		for (int offset = 0; offset < width; offset++) {
			int currX = startPos.getX() + offset*xFactor;
			int currZ = startPos.getZ() + offset*zFactor;
			BlockPos currPos = new BlockPos(currX, startPos.getY(), currZ);
			IBlockState blockState = worldIn.getBlockState(currPos);
			ITextComponent blockName = blockState.getBlock().getPickBlock(
					blockState,
					new RayTraceResult(new Vec3d(0d, -1.0d, 0d), EnumFacing.UP),
					worldIn, currPos, playerIn)
					.getTextComponent();
			if(!isBlockStateValidForPalette(blockState)) {
				if(worldIn.isRemote)
					MessageUtils.sendErrorMessage(playerIn, "palette_invalid_blocks", blockName);
				return EnumActionResult.FAIL; //width must be odd
			}
			ResourceLocation id = Objects.requireNonNull(blockState.getBlock().getRegistryName());
			int meta = blockState.getBlock().getMetaFromState(blockState);
			paletteTag.appendTag(new NBTTagString(id.toString() + "#" + meta));
			blockNames.add(blockName);
		}
		nbt.setTag("palette", paletteTag);
		stack.setTagCompound(nbt);
		ITextComponent blockNamesComponent = new TextComponentString("");
		boolean first = true;
		for (ITextComponent blockName : blockNames) {
			if(first) {
				first = false;
			} else {
				blockNamesComponent.appendText(", ");
			}

			blockNamesComponent.appendSibling(blockName);
		}
		ITextComponent feedbackMessage = new TextComponentTranslation("citymaker.info.palette_chosen", blockNamesComponent);
		if(worldIn.isRemote)
			MessageUtils.sendInfoMessage(playerIn, feedbackMessage);
		return EnumActionResult.SUCCESS;
	}

	private boolean isBlockStateValidForPalette(IBlockState blockState) {
		return !blockState.getBlock().hasTileEntity(blockState)
				&& blockState.getMaterial().blocksMovement();
	}

	private NBTTagCompound getOrCreateNBT(ItemStack stack) {
		NBTTagCompound nbt;
		if(!stack.hasTagCompound()) {
			nbt = new NBTTagCompound();
		}
		else {
			nbt = stack.getTagCompound();
		}
		return nbt;
	}
}
