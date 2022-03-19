package kaktusz.citymaker.util;

import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;

public class InventoryUtils {
	public static void dropInventoryItems(World worldIn, BlockPos pos, IItemHandler inventory)
	{
		dropInventoryItems(worldIn, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), inventory);
	}

	private static void dropInventoryItems(World worldIn, double x, double y, double z, IItemHandler inventory)
	{
		for (int i = 0; i < inventory.getSlots(); ++i)
		{
			ItemStack itemstack = inventory.getStackInSlot(i);

			if (!itemstack.isEmpty())
			{
				InventoryHelper.spawnItemStack(worldIn, x, y, z, itemstack);
			}
		}
	}
}
