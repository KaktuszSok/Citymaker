package kaktusz.citymaker.handlers;

import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.networking.RequestBuilderScanPacket;
import kaktusz.citymaker.networking.SetBuilderRoadmapPacket;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class ModPacketHandler {
	public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Citymaker.MODID);
	private static int highestPacketId = 0;

	public static void init() {
		INSTANCE.registerMessage(
				SetBuilderRoadmapPacket.SetBuilderRoadmapHandler.class,
				SetBuilderRoadmapPacket.class,
				highestPacketId++,
				Side.SERVER
				);
		INSTANCE.registerMessage(
				RequestBuilderScanPacket.RequestBuilderScanHandler.class,
				RequestBuilderScanPacket.class,
				highestPacketId++,
				Side.SERVER
		);
	}
}
