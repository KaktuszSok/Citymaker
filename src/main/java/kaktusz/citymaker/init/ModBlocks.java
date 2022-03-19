package kaktusz.citymaker.init;

import kaktusz.citymaker.blocks.BlockRoadBuilder;
import kaktusz.citymaker.blocks.BlockRoadBuilderCreative;
import kaktusz.citymaker.blocks.BlockWallBuilder;
import kaktusz.citymaker.blocks.BlockWallBuilderCreative;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

import java.util.HashSet;
import java.util.Set;

public class ModBlocks {
	public static final Set<Block> BLOCKS = new HashSet<>();
	public static final Set<Runnable> BLOCK_REGISTER_CALLBACKS = new HashSet<>();

	public static final BlockRoadBuilder ROAD_BUILDER = new BlockRoadBuilder("road_builder", Material.ROCK, CreativeTabs.TRANSPORTATION);
	public static final BlockRoadBuilderCreative ROAD_BUILDER_CREATIVE = new BlockRoadBuilderCreative("road_builder_creative", Material.ROCK, CreativeTabs.TRANSPORTATION);

	public static final BlockWallBuilder WALL_BUILDER = new BlockWallBuilder("wall_builder", Material.ROCK, CreativeTabs.TRANSPORTATION);
	public static final BlockWallBuilderCreative WALL_BUILDER_CREATIVE = new BlockWallBuilderCreative("wall_builder_creative", Material.ROCK, CreativeTabs.TRANSPORTATION);
}
