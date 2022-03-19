package kaktusz.citymaker.networking;

import io.netty.buffer.ByteBuf;
import kaktusz.citymaker.tileentities.TileEntityRoadBuilder;
import kaktusz.citymaker.util.Roadmap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class RequestBuilderScanPacket implements IMessage {

	public BlockPos builderPos;
	public boolean large;

	public RequestBuilderScanPacket() {

	}

	public RequestBuilderScanPacket(BlockPos builderPos, boolean large) {
		this.builderPos = builderPos;
		this.large = large;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		builderPos = new BlockPos(buf.readInt(), buf.readShort(), buf.readInt());
		large = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(builderPos.getX());
		buf.writeShort(builderPos.getY());
		buf.writeInt(builderPos.getZ());
		buf.writeBoolean(large);
	}

	public static class RequestBuilderScanHandler implements IMessageHandler<RequestBuilderScanPacket, IMessage> {

		@Override
		public IMessage onMessage(RequestBuilderScanPacket message, MessageContext ctx) {
			EntityPlayerMP playerMP = ctx.getServerHandler().player;

			playerMP.getServerWorld().addScheduledTask(() -> {
				if(!playerMP.world.isBlockLoaded(message.builderPos))
					return;

				TileEntity te = playerMP.world.getTileEntity(message.builderPos);
				if(!(te instanceof TileEntityRoadBuilder))
					return;

				TileEntityRoadBuilder rb = (TileEntityRoadBuilder)te;
				int chunksX = message.large ? 32 : 16;
				int chunksZ = message.large ? 18 : 9;
				Roadmap scanned = rb.scanArea(chunksX, chunksZ);
				if(scanned == null)
					return;
				String scanStr = Roadmap.toBase64(scanned);
				rb.setRoadmap(scanStr, true);
			});

			return null;
		}
	}
}
