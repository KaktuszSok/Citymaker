package kaktusz.citymaker.rendering;

import kaktusz.citymaker.util.Roadmap;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.awt.image.BufferedImage;

public class MapTexture extends DynamicTexture {

	public final int width;
	public final int height;

	public MapTexture(Roadmap roadmap) {
		this(getImageFromMap(roadmap));
	}

	public MapTexture(BufferedImage img) {
		super(img);
		this.width = img.getWidth();
		this.height = img.getHeight();
	}

	public static BufferedImage getImageFromMap(Roadmap roadmap) {
		if(roadmap == null)
			return new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);

		int width = roadmap.statemap.length;
		int height = roadmap.statemap[0].length;
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		int[] rgbArray = new int[width*height];

		for (int z = 0; z < height; z++) {
			for (int x = 0; x < width; x++) {
				rgbArray[(height-1 - z)*width + x] = roadmap.getColour(x, z);
			}
		}

		img.setRGB(0, 0, width, height, rgbArray, 0, width);
		return img;
	}
}
