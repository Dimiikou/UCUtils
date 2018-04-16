package de.fuzzlemann.ucutils.commands.faction;

import de.fuzzlemann.ucutils.Main;
import de.fuzzlemann.ucutils.utils.command.Command;
import de.fuzzlemann.ucutils.utils.command.CommandExecutor;
import de.fuzzlemann.ucutils.utils.location.navigation.NavigationUtil;
import de.fuzzlemann.ucutils.utils.text.Message;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class CallReinforcementCommand implements CommandExecutor {

    private static Pattern REINFORCEMENT_PATTERN = Pattern.compile("^.+ [\\[UC\\]]*[a-zA-Z0-9_]+: Ben\u00f6tige Verst\u00e4rkung! -> X: -*\\d+ \\| Y: -*\\d+ \\| Z: -*\\d+$");

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent e) {
        String message = e.getMessage().getUnformattedText();

        if (!REINFORCEMENT_PATTERN.matcher(message).find()) return;

        String[] splitMessage = StringUtils.split(message, ":", 2);
        String name = splitMessage[0];
        String fChatMessage = splitMessage[1];

        int posX;
        int posY;
        int posZ;

        try {
            String[] fullLocString = StringUtils.split(fChatMessage.substring(25), "|", 3);

            posX = parseLoc(fullLocString[0]);
            posY = parseLoc(fullLocString[1]);
            posZ = parseLoc(fullLocString[2]);
        } catch (Exception exc) {
            return;
        }

        Message.MessageBuilder builder = Message.builder()
                .of(name).color(TextFormatting.DARK_GREEN).advance()
                .of(" ben\u00f6tigt Unterst\u00fctzung bei X: " + posX + " | Y: " + posY + " | Z: " + posZ + "!").color(TextFormatting.GREEN).advance();

        EntityPlayer p = Main.MINECRAFT.player;

        p.sendMessage(builder.build().toTextComponent());
        p.sendMessage(NavigationUtil.getNavigationText(posX, posY, posZ));

        e.setCanceled(true);
    }

    private static int parseLoc(String locString) {
        locString = locString.substring(4).trim();
        return Integer.parseInt(locString);
    }

    @Override
    @Command(labels = {"reinforcement", "callreinforcement", "verst\u00e4rkung"})
    public boolean onCommand(EntityPlayerSP p, String[] args) {
        int posX = (int) p.posX;
        int posY = (int) p.posY;
        int posZ = (int) p.posZ;

        String chatType;
        if (args.length == 1 && args[0].equalsIgnoreCase("-d")) {
            chatType = "d";
        } else {
            chatType = "f";
        }

        p.sendChatMessage("/" + chatType + " Ben\u00f6tige Verst\u00e4rkung! -> X: " + posX + " | Y: " + posY + " | Z: " + posZ);
        return true;
    }
}