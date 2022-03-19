package kaktusz.citymaker.blocks;

import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.init.ModBlocks;
import kaktusz.citymaker.tileentities.TileEntityRoadBuilderCreative;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;

public class BlockRoadBuilderCreative extends BlockRoadBuilder {

	public BlockRoadBuilderCreative(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
		ModBlocks.BLOCK_REGISTER_CALLBACKS.add(() -> GameRegistry.registerTileEntity(
				TileEntityRoadBuilderCreative.class,
				new ResourceLocation(Citymaker.MODID, "road_builder_creative")
		));
	}

	@Nullable
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityRoadBuilderCreative();
	}
}
