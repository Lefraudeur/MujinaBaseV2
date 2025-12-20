package io.github.lefraudeur;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class Main
{
    // warning called from c++ thread
    public static void onLoad()
    {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Hello from Mujina"));
    }

    // warning called from c++ thread
    public static void onUnload()
    {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentText("Bye from Mujina"));
    }

    public static String getWindowTitle()
    {
        return org.lwjgl.opengl.Display.getTitle();
    }
}