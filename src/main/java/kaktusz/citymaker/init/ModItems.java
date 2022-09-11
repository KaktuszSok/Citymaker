package kaktusz.citymaker.init;

import kaktusz.citymaker.items.ItemRoadStaff;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
	public static final List<Item> ITEMS = new ArrayList<>();

	public static final ItemRoadStaff ROAD_STAFF = new ItemRoadStaff("road_staff", CreativeTabs.TRANSPORTATION);
}
