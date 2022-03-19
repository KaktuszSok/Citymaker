package kaktusz.citymaker.containers;

import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.tileentities.TileEntityWallBuilder;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;

public class WallBuilderContainerGUI extends GuiContainer {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 176;

	private static final ResourceLocation BACKGROUND = new ResourceLocation(Citymaker.MODID, "textures/gui/wall_builder_container.png");

	private final TileEntityWallBuilder tileEntity;
	private final WallBuilderContainer container;

	public WallBuilderContainerGUI(TileEntityWallBuilder tileEntity, WallBuilderContainer container) {
		super(container);
		xSize = WIDTH;
		ySize = HEIGHT;
		this.tileEntity = tileEntity;
		this.container = container;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		String displayName = tileEntity.getDisplayName().getUnformattedText();
		fontRenderer.drawString(displayName, (xSize/2 - fontRenderer.getStringWidth(displayName)/2) + 3, 6, 4210752);
		fontRenderer.drawString(container.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		mc.getTextureManager().bindTexture(BACKGROUND);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		renderHoveredToolTip(mouseX, mouseY);
	}
}
