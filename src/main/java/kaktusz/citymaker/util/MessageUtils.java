package kaktusz.citymaker.util;

import kaktusz.citymaker.Citymaker;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class MessageUtils {
	public static void sendErrorMessage(ICommandSender target, String errorKey, Object... args) {
		target.sendMessage(new TextComponentTranslation(
				Citymaker.MODID + ".error." + errorKey, args)
				.setStyle(FormattingUtils.errorStyle));
	}

	public static void sendInfoMessage(ICommandSender target, String infoKey, Object... args) {
		target.sendMessage(new TextComponentTranslation(
				Citymaker.MODID + ".info." + infoKey, args)
				.setStyle(FormattingUtils.infoStyle));
	}

	public static void sendInfoMessage(ICommandSender target, ITextComponent message) {
		target.sendMessage(message.setStyle(FormattingUtils.infoStyle));
	}
}
