package kaktusz.citymaker.blocks;

import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.init.ModBlocks;
import kaktusz.citymaker.init.ModItems;
import kaktusz.citymaker.util.IHasModel;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

public class BlockBase extends Block implements IHasModel {

	public BlockBase(String name, Material material, CreativeTabs tab) {
		super(material);
		setUnlocalizedName(name);
		setRegistryName(name);
		setCreativeTab(tab);

		ModBlocks.BLOCKS.add(this);
		//noinspection ConstantConditions
		ModItems.ITEMS.add(new ItemBlock(this).setRegistryName(this.getRegistryName()));
	}

	@Override
	public void registerModels() {
		Citymaker.proxy.registerItemRenderer(Item.getItemFromBlock(this), 0, "inventory");
	}
}
