package kaktusz.citymaker.init;

import kaktusz.citymaker.Citymaker;
import net.minecraftforge.common.config.Config;

@Config(modid = Citymaker.MODID)
public class ModConfig {
	@Config.Comment("How much FE does the road builder consume per block placed/removed?")
	public static int roadBuilderFEPerBlock = 512;
	@Config.Comment("How much FE does the wall builder consume per block placed/removed?")
	public static int wallBuilderFEPerBlock = 512;

	@Config.Comment({"Blocks which the wall builder will ignore when scanning for column templates.",
	"These blocks may be used as parts of the column but may not be the bottom block."})
	public static String[] wallBuilderIgnoredBlocks = new String[] {
			"minecraft:dirt",
			"minecraft:grass",
			"minecraft:sand",
			"minecraft:gravel"
	};
}
