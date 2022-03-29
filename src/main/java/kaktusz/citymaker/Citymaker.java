package kaktusz.citymaker;

import kaktusz.citymaker.handlers.ModPacketHandler;
import kaktusz.citymaker.handlers.RegistryHandler;
import kaktusz.citymaker.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = Citymaker.MODID, name = Citymaker.NAME, version = Citymaker.VERSION)
public class Citymaker
{
    public static final String MODID = "citymaker";
    public static final String NAME = "Citymaker";
    public static final String VERSION = "1.0";

    @Mod.Instance
    public static Citymaker INSTANCE;
    public static Logger logger;

    public static final String CLIENT_PROXY_CLASS = "kaktusz.citymaker.proxy.ClientProxy";
    public static final String COMMON_PROXY_CLASS = "kaktusz.citymaker.proxy.CommonProxy";
    @SidedProxy(clientSide = CLIENT_PROXY_CLASS, serverSide = COMMON_PROXY_CLASS)
    public static CommonProxy PROXY;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        RegistryHandler.init(event);
        ModPacketHandler.init();
    }
}
