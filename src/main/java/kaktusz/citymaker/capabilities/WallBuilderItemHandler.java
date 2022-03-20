package kaktusz.citymaker.capabilities;

import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

public class WallBuilderItemHandler extends ItemStackHandler {

	public WallBuilderItemHandler(int size) {
		super(size);
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof ItemBlock;
	}

	public boolean canBuild() {
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = getStackInSlot(i);
			if(!stack.isEmpty())
				return true;
		}
		return false;
	}

	public boolean tryConsumeItemLike(ItemStack requiredItem) {
		for (ItemStack stack : stacks) {
			if(ItemStack.areItemsEqual(stack, requiredItem) && ItemStack.areItemStackTagsEqual(stack, requiredItem)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}
}
