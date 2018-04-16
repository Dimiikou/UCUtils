package de.fuzzlemann.ucutils.utils;

import de.fuzzlemann.ucutils.Main;
import de.fuzzlemann.ucutils.utils.text.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
public class ForgeUtils {

    public static List<String> getOnlinePlayers() {
        Minecraft minecraft = Main.MINECRAFT;
        NetHandlerPlayClient connection = minecraft.getConnection();

        if (connection == null) return Collections.emptyList();

        Collection<NetworkPlayerInfo> playerInfoList = connection.getPlayerInfoMap();

        return playerInfoList.stream()
                .map(playerInfo -> playerInfo.getGameProfile().getName())
                .map(TextUtils::stripColor)
                .map(TextUtils::stripPrefix)
                .sorted()
                .collect(Collectors.toList());
    }

    public static void shutdownPC() {
        String shutdownCommand;

        if (SystemUtils.IS_OS_AIX) {
            shutdownCommand = "shutdown -Fh now";
        } else if (SystemUtils.IS_OS_SOLARIS || SystemUtils.IS_OS_SUN_OS) {
            shutdownCommand = "shutdown -y -i5 -gnow";
        } else if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_UNIX) {
            shutdownCommand = "shutdown -h now";
        } else if (SystemUtils.IS_OS_HP_UX) {
            shutdownCommand = "shutdown -hy now";
        } else if (SystemUtils.IS_OS_IRIX) {
            shutdownCommand = "shutdown -y -g now";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            shutdownCommand = "shutdown -s -t 0";
        } else {
            return;
        }

        try {
            Runtime.getRuntime().exec(shutdownCommand);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
