package de.fuzzlemann.ucutils.commands.mobile;

import de.fuzzlemann.ucutils.utils.command.Command;
import de.fuzzlemann.ucutils.utils.command.CommandExecutor;
import de.fuzzlemann.ucutils.utils.text.TextUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.sound.SoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class DoNotDisturbCommand implements CommandExecutor {

    private static boolean doNotDisturb;

    @Override
    @Command(labels = {"donotdisturb", "stumm", "nichtstören"})
    public boolean onCommand(EntityPlayerSP p, String[] args) {
        doNotDisturb = !doNotDisturb;

        if (doNotDisturb) {
            TextUtils.simplePrefixMessage("Du hast den Ton deines Handys ausgeschalten.");
        } else {
            TextUtils.simplePrefixMessage("Du hast den Ton deines Handys eingeschalten.");
        }
        return true;
    }

    @SubscribeEvent
    public static void onSound(SoundEvent.SoundSourceEvent e) {
        if (!doNotDisturb) return;

        String name = e.getName();
        if (!name.equals("record.cat") && !name.equals("record.stal") && !name.equals("entity.sheep.ambient")) return;

        e.setCanceled(true);
    }
}
