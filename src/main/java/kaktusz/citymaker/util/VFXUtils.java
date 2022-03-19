package kaktusz.citymaker.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class VFXUtils {
	/**
	 * Spawns a particle slightly above the given block for all players.
	 * Must be called on server side.
	 */
	public static void serverSpawnParticle(World world, BlockPos pos, EnumParticleTypes particleType, boolean longDistance) {
		serverSpawnParticle(world, pos.getX() + 0.5D, pos.getY() + 1.25D, pos.getZ() + 0.5D, particleType, longDistance);
	}

	/**
	 * Spawns a particle for all players.
	 * Must be called on server side.
	 */
	public static void serverSpawnParticle(World world, double x, double y, double z, EnumParticleTypes particleType, boolean longDistance) {
		for (EntityPlayer player : world.playerEntities) {
			if (player instanceof EntityPlayerMP) {
				((WorldServer)world).spawnParticle(
					(EntityPlayerMP) player,
					particleType,
					longDistance,
					x,
					y,
					z,
					1,
					0, 0, 0,
					0);
			}
		}
	}
}
