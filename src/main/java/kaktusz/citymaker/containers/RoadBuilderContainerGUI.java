package kaktusz.citymaker.containers;

import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.handlers.ModPacketHandler;
import kaktusz.citymaker.networking.RequestBuilderScanPacket;
import kaktusz.citymaker.networking.SetBuilderRoadmapPacket;
import kaktusz.citymaker.rendering.GuiButtonStandardIcon;
import kaktusz.citymaker.rendering.MapTexture;
import kaktusz.citymaker.tileentities.TileEntityRoadBuilder;
import kaktusz.citymaker.util.FormattingUtils;
import kaktusz.citymaker.util.Roadmap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class RoadBuilderContainerGUI extends GuiContainer {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 176;

	private static final ResourceLocation BACKGROUND = new ResourceLocation(Citymaker.MODID, "textures/gui/road_builder_container.png");
	private static final ResourceLocation ICON_SCAN = new ResourceLocation(Citymaker.MODID, "textures/gui/icons/road_builder_scan.png");
	private static final ResourceLocation ICON_IMPORT = new ResourceLocation(Citymaker.MODID, "textures/gui/icons/road_builder_import.png");
	private static final ResourceLocation ICON_EXPORT = new ResourceLocation(Citymaker.MODID, "textures/gui/icons/road_builder_export.png");

	private final TileEntityRoadBuilder tileEntity;
	private final RoadBuilderContainer container;

	public RoadBuilderContainerGUI(TileEntityRoadBuilder tileEntity, RoadBuilderContainer container) {
		super(container);
		xSize = WIDTH;
		ySize = HEIGHT;
		this.tileEntity = tileEntity;
		this.container = container;
	}

	@Override
	public void initGui() {
		super.initGui();
		addButton(new GuiButtonStandardIcon(0, guiLeft+151, guiTop+17, 18, 18, ICON_SCAN,
				(x,y) -> {
			drawHoveringText("Scan Terrain", x, y);
					System.out.println("pogersy");
				}));
		addButton(new GuiButtonStandardIcon(1, guiLeft+151, guiTop+39, 18, 18, ICON_IMPORT,
				(x,y) -> {
					drawHoveringText("Import from Clipboard", x, y);
				}));
		addButton(new GuiButtonStandardIcon(2, guiLeft+151, guiTop+61, 18, 18, ICON_EXPORT,
				(x,y) -> {
					drawHoveringText("Export to Clipboard", x, y);
				}));
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
			case 0:
				ModPacketHandler.INSTANCE.sendToServer(
						new RequestBuilderScanPacket(tileEntity.getPos(), isShiftKeyDown())
				);
				break;
			case 1:
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				String clipboardText;
				try {
					clipboardText = (String) clipboard.getData(DataFlavor.stringFlavor);
				} catch (UnsupportedFlavorException e) {
					Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation(
							Citymaker.MODID + ".error.clipboard_contents_invalid")
							.setStyle(FormattingUtils.errorStyle));
					return;
				}
				ModPacketHandler.INSTANCE.sendToServer(
						new SetBuilderRoadmapPacket(tileEntity.getPos(), Roadmap.fromBase64(clipboardText))
				);
				break;
			case 2:
				if(StringUtils.isNullOrEmpty(tileEntity.getRoadmapStr()))
					break;
				clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection roadmapStr = new StringSelection(tileEntity.getRoadmapStr());
				clipboard.setContents(roadmapStr, roadmapStr);
				Minecraft.getMinecraft().player.sendMessage(new TextComponentTranslation(Citymaker.MODID + ".info.map_copied_to_clipboard"));
				break;
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		String displayName = tileEntity.getDisplayName().getUnformattedText();
		fontRenderer.drawString(displayName, (xSize/2 - fontRenderer.getStringWidth(displayName)/2) + 3, 6, 4210752);
		fontRenderer.drawString(container.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
		MapTexture tex = tileEntity.getGuiMapTexture();
		ResourceLocation textureLocation = mc.getTextureManager().getDynamicTextureLocation(Citymaker.MODID + ":gui_roadmap", tex);
		mc.getTextureManager().bindTexture(textureLocation);
		GL11.glColor4f(1f, 1f, 1f, 1f);
		drawScaledCustomSizeModalRect(
				33,17,
				0, 0,
				tex.width, tex.height,
				110, 62,
				tex.width, tex.height
		);
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
