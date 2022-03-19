package kaktusz.citymaker.blocks;

import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.init.ModBlocks;
import kaktusz.citymaker.tileentities.TileEntityRoadBuilder;
import kaktusz.citymaker.util.InventoryUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlockRoadBuilder extends BlockBase implements ITileEntityProvider {

	public static final int GUI_ID = 1;

	public BlockRoadBuilder(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
		ModBlocks.BLOCK_REGISTER_CALLBACKS.add(() -> GameRegistry.registerTileEntity(
				TileEntityRoadBuilder.class,
				new ResourceLocation(Citymaker.MODID, "road_builder")
		));
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityRoadBuilder();
	}

	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return Item.getItemFromBlock(this);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if(worldIn.isRemote)
			return true;

		TileEntity te = worldIn.getTileEntity(pos);
		if(!(te instanceof TileEntityRoadBuilder))
			return false;

		playerIn.openGui(Citymaker.INSTANCE, GUI_ID, worldIn, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
	{
		TileEntity tileentity = worldIn.getTileEntity(pos);

		if (tileentity instanceof TileEntityRoadBuilder)
		{
			InventoryUtils.dropInventoryItems(worldIn, pos, ((TileEntityRoadBuilder)tileentity).inventory);
			worldIn.updateComparatorOutputLevel(pos, this);
		}

		super.breakBlock(worldIn, pos, state);
	}
}
