package de.fuzzlemann.ucutils.commands.info;

import de.fuzzlemann.ucutils.utils.command.Command;
import de.fuzzlemann.ucutils.utils.command.CommandExecutor;
import de.fuzzlemann.ucutils.utils.info.FactionInfo;
import de.fuzzlemann.ucutils.utils.info.FactionInfoEnum;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
public class FCInfoCommand implements CommandExecutor {

    @Override
    @Command(labels = {"fcinfo", "factioncommandinfo", "fcommandinfo", "factioncinfo"})
    public boolean onCommand(EntityPlayerSP p, String[] args) {
        if (args.length == 0) {
            TextComponentString text = new TextComponentString("");

            for (FactionInfoEnum factionInfoEnum : FactionInfoEnum.values()) {
                FactionInfo factionInfo = factionInfoEnum.getFactionInfo();

                text.appendText("\n").appendSibling(factionInfo.constructClickableMessage("/fcinfo " + factionInfo.getShortName()));
            }

            p.sendMessage(text);
            return true;
        }

        FactionInfoEnum factionInfoEnum = FactionInfoEnum.getFactionInfoEnum(args[0]);

        if (factionInfoEnum == null) {
            TextComponentString text = new TextComponentString("Die Fraktion wurde nicht gefunden.");
            text.getStyle().setColor(TextFormatting.RED);

            p.sendMessage(text);
            return true;
        }

        p.sendMessage(factionInfoEnum.getFactionInfo().constructCommandHelpMessage());
        return true;
    }
}