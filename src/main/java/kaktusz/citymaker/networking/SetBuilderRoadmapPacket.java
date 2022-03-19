package kaktusz.citymaker.networking;

import io.netty.buffer.ByteBuf;
import kaktusz.citymaker.Citymaker;
import kaktusz.citymaker.tileentities.TileEntityRoadBuilder;
import kaktusz.citymaker.util.FormattingUtils;
import kaktusz.citymaker.util.Roadmap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.nio.charset.StandardCharsets;

public class SetBuilderRoadmapPacket implements IMessage {

	public BlockPos builderPos;
	public String roadmapStr;

	public SetBuilderRoadmapPacket() {

	}

	public SetBuilderRoadmapPacket(BlockPos builderPos, Roadmap roadmap) {
		this.builderPos = builderPos;
		String roadmapStr = Roadmap.toBase64(roadmap, false);
		this.roadmapStr = roadmapStr == null ? "" : roadmapStr;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		builderPos = new BlockPos(buf.readInt(), buf.readShort(), buf.readInt());
		roadmapStr = buf.readCharSequence(buf.readInt(), StandardCharsets.US_ASCII).toString();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(builderPos.getX());
		buf.writeShort(builderPos.getY());
		buf.writeInt(builderPos.getZ());
		buf.writeInt(roadmapStr.length());
		buf.writeCharSequence(roadmapStr, StandardCharsets.US_ASCII);
	}

	public static class SetBuilderRoadmapHandler implements IMessageHandler<SetBuilderRoadmapPacket, IMessage> {
		@Override
		public IMessage onMessage(SetBuilderRoadmapPacket message, MessageContext ctx) {
			EntityPlayerMP playerMP = ctx.getServerHandler().player;

			playerMP.getServerWorld().addScheduledTask(() -> {
				if(!playerMP.world.isBlockLoaded(message.builderPos))
					return;

				TileEntity te = playerMP.world.getTileEntity(message.builderPos);
				if(!(te instanceof TileEntityRoadBuilder))
					return;

				Roadmap roadmap = Roadmap.fromBase64(message.roadmapStr);
				if(roadmap == null)
					return;
				TileEntityRoadBuilder rb = (TileEntityRoadBuilder)te;
				Roadmap fullInfoRoadmap = rb.scanArea(roadmap.statemap.length/16, roadmap.statemap[0].length/16);
				Roadmap.mergeGridData(roadmap, fullInfoRoadmap);
				rb.setRoadmap(fullInfoRoadmap, true);

				if(rb.getRoadmap() != null)
					playerMP.sendMessage(new TextComponentTranslation(Citymaker.MODID + ".info.roadmap_import_valid"));
				else
					playerMP.sendMessage(new TextComponentTranslation(Citymaker.MODID + ".error.roadmap_import_invalid")
							.setStyle(FormattingUtils.errorStyle));
			});

			return null;
		}
	}
}
