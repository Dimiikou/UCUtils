package de.fuzzlemann.ucutils.commands.info;

import de.fuzzlemann.ucutils.utils.command.api.Command;
import de.fuzzlemann.ucutils.utils.text.Message;
import de.fuzzlemann.ucutils.utils.text.MessagePart;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
public class InfoCommand {

    @Command("info")
    public boolean onCommand() {
        Message.Builder builder = Message.builder();

        constructText(builder, "Fraktionen", "/finfo");
        constructText(builder, "Wichtige Befehle", "/cinfo");
        constructText(builder, "Fraktionsbefehle", "/fcinfo");

        builder.send();
        return true;
    }

    private void constructText(Message.Builder builder, String text, String command) {
        builder.of("\n » ").color(TextFormatting.RED).advance()
                .of(text).color(TextFormatting.DARK_GREEN)
                .hoverEvent(HoverEvent.Action.SHOW_TEXT, MessagePart.simple("Klick mich!", TextFormatting.GREEN))
                .clickEvent(ClickEvent.Action.RUN_COMMAND, command)
                .advance();
    }
}
