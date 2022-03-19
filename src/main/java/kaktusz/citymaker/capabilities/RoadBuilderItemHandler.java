package kaktusz.citymaker.capabilities;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class RoadBuilderItemHandler extends ItemStackHandler {

	public RoadBuilderItemHandler(int size) {
		super(size);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof ItemBlock;
	}

	public boolean canBuild() {
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = getStackInSlot(i);
			if(!stack.isEmpty() && stack.getCount() > 1)
				return true;
		}
		return false;
	}
}
