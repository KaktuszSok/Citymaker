package kaktusz.citymaker.containers;

import kaktusz.citymaker.tileentities.TileEntityRoadBuilder;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class RoadBuilderContainer extends Container {

	public final TileEntityRoadBuilder tileEntity;
	public final IInventory playerInventory;

	public RoadBuilderContainer(IInventory playerInventory, TileEntityRoadBuilder te) {
		this.tileEntity = te;
		this.playerInventory = playerInventory;

		addBuilderSlots();
		addPlayerSlots(playerInventory);
	}

	private void addPlayerSlots(IInventory playerInventory) {
		//main inv
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				int x = 8 + col * 18;
				int y = row * 18 + 94;
				this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 10, x, y));
			}
		}

		//hotbar
		for (int row = 0; row < 9; ++row) {
			int x = 8 + row * 18;
			int y = 152;
			this.addSlotToContainer(new Slot(playerInventory, row, x, y));
		}
	}

	private void addBuilderSlots() {
		IItemHandler handler = tileEntity.inventory;
		addSlotToContainer(new SlotItemHandler(handler, 0, 8, 18));
		addSlotToContainer(new SlotItemHandler(handler, 1, 8, 40));
		addSlotToContainer(new SlotItemHandler(handler, 2, 8, 62));
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot = inventorySlots.get(index);

		if(slot != null && slot.getHasStack()) {
			ItemStack slotStack = slot.getStack();
			itemStack = slotStack.copy();

			if(index < TileEntityRoadBuilder.INV_SIZE) {
				if(!mergeItemStack(slotStack, TileEntityRoadBuilder.INV_SIZE, inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if(!mergeItemStack(slotStack, 0, TileEntityRoadBuilder.INV_SIZE, false)) {
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
