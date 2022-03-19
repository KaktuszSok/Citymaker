package kaktusz.citymaker.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.function.BiConsumer;

/**
 * Draws a standard GuiButton with a given icon over it and, optionally, hover text.
 */
public class GuiButtonStandardIcon extends GuiButton {

	private final ResourceLocation icon;
	private final BiConsumer<Integer, Integer> hoverAction;

	public GuiButtonStandardIcon(int buttonId, int x, int y, int widthIn, int heightIn, ResourceLocation icon, BiConsumer<Integer, Integer> hoverAction) {
		super(buttonId, x, y, widthIn, heightIn, "");
		this.icon = icon;
		this.hoverAction = hoverAction;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		super.drawButton(mc, mouseX, mouseY, partialTicks);
		if (visible)
		{
			mc.getTextureManager().bindTexture(icon);
			GlStateManager.disableDepth();

			drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
			GlStateManager.enableDepth();
		}
	}

	@Override
	public void drawButtonForegroundLayer(int mouseX, int mouseY) {
		super.drawButtonForegroundLayer(mouseX, mouseY);
		if(this.hovered && hoverAction != null) {
			hoverAction.accept(mouseX, mouseY);
		}
	}
}
