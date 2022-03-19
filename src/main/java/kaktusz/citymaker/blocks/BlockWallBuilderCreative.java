package kaktusz.citymaker.blocks;

import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.init.ModBlocks;
import kaktusz.citymaker.tileentities.TileEntityWallBuilderCreative;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;

public class BlockWallBuilderCreative extends BlockWallBuilder {
	public BlockWallBuilderCreative(String name, Material material, CreativeTabs tab) {
		super(name, material, tab);
		ModBlocks.BLOCK_REGISTER_CALLBACKS.add(() -> GameRegistry.registerTileEntity(
				TileEntityWallBuilderCreative.class,
				new ResourceLocation(Citymaker.MODID, "wall_builder_creative")
		));
	}

	@Nullable
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityWallBuilderCreative();
	}
}
