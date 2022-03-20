package kaktusz.citymaker.containers;

import kaktusz.citymaker.tileentities.TileEntityWallBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class WallBuilderContainer extends Container {

	public final TileEntityWallBuilder tileEntity;
	public final IInventory playerInventory;

	public WallBuilderContainer(IInventory playerInventory, TileEntityWallBuilder te) {
		this.tileEntity = te;
		this.playerInventory = playerInventory;

		addBuilderSlots();
		addPlayerSlots(playerInventory);
	}

	private void addPlayerSlots(IInventory playerInventory) {
		int offset = (tileEntity.inventory.getSlots()/9 - 4) * 18;

		//main inv
		for (int r = 0; r < 3; r++)
		{
			for (int c = 0; c < 9; c++)
			{
				this.addSlotToContainer(new Slot(playerInventory, c + r * 9 + 9, 8 + c * 18, 103 + r * 18 + offset));
			}
		}

		//hotbar
		for (int i1 = 0; i1 < 9; ++i1)
		{
			this.addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 161 + offset));
		}
	}

	private void addBuilderSlots() {
		IItemHandler handler = tileEntity.inventory;
		for (int j = 0; j < handler.getSlots()/9; ++j)
		{
			for (int k = 0; k < 9; ++k)
			{
				this.addSlotToContainer(new SlotItemHandler(handler, k + j * 9, 8 + k * 18, 18 + j * 18));
			}
		}

	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot = inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			itemStack = slotStack.copy();

			if(index < TileEntityWallBuilder.INV_SIZE) {
				if(!mergeItemStack(slotStack, TileEntityWallBuilder.INV_SIZE, inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if(!mergeItemStack(slotStack, 0, TileEntityWallBuilder.INV_SIZE, false)) {
				return ItemStack.EMPTY;
			}

			if(slotStack.isEmpty()) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}
		}

		return itemStack;
	}

	@Override
	public boolean canInteractWith(EntityPlayer playerIn) {
		return tileEntity.canInteractWith(playerIn);
	}
}
