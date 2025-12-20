package io.github.lefraudeur;

import static io.github.lefraudeur.internal.patcher.MethodModifier.Type.ON_ENTRY;
import static io.github.lefraudeur.internal.patcher.MethodModifier.Type.ON_LDC_CONSTANT;
import static io.github.lefraudeur.internal.patcher.MethodModifier.Type.ON_RETURN_THROW;

import org.lwjgl.input.Keyboard;

import io.github.lefraudeur.internal.Canceler;
import io.github.lefraudeur.internal.EventHandler;
import io.github.lefraudeur.internal.Thrower;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;

public class TestClass
{
    private static boolean xrayEnabled = false;
    private static float oldValueGamma = Minecraft.getMinecraft().gameSettings.gammaSetting;
    private static int oldValue = Minecraft.getMinecraft().gameSettings.ambientOcclusion;
    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "sendChatMessage",
            targetMethodDescriptor = "(Ljava/lang/String;)V",
            targetMethodIsStatic = false)
    public static void sendChatMessage(Canceler canceler, EntityPlayerSP player, String message)
    {
        System.out.println("sendChatMessage on entry succeeded");
        System.out.println(Main.getWindowTitle());
    }

    @EventHandler(type=ON_RETURN_THROW,
            targetClass = "net/minecraft/client/entity/EntityPlayerSP",
            targetMethodName = "sendChatMessage",
            targetMethodDescriptor = "(Ljava/lang/String;)V")
    public static void sendChatMessage(Thrower thrower, EntityPlayerSP player, String message)
    {
        System.out.println("sendChatMessage on return succeeded");
    }


    @EventHandler(type=ON_ENTRY,
            targetClass = "net/minecraft/block/Block",
            targetMethodName = "shouldSideBeRendered",
            targetMethodDescriptor = "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/BlockPos;Lnet/minecraft/util/EnumFacing;)Z")
    public static boolean shouldSideBeRendered(Canceler canceler, Block thisBlock, IBlockAccess p_149646_1_, BlockPos blockPos, EnumFacing enumFacing)
    {
        if (!xrayEnabled) return false;

        canceler.cancel = true;
    if (thisBlock.getUnlocalizedName().contains("ore"))
            return true;
        return false;
    }

    @EventHandler(type=ON_LDC_CONSTANT,
            targetClass = "net/minecraft/client/renderer/EntityRenderer",
            targetMethodName = "getMouseOver",
            targetMethodDescriptor = "(F)V")
    public static Object getMouseOverVar4(Object value)
    {
        // reach
        // it changes \u26033 value to 4.0D
        if (value instanceof Double && (Double)value == 3.0D)
            return 4.0D;
        return value;
    }


    @EventHandler(type=ON_RETURN_THROW,
            targetClass = "org/lwjgl/input/Keyboard",
            targetMethodName = "next",
            targetMethodDescriptor = "()Z",
            targetMethodIsStatic = true)
    public static boolean Keyboard_next(boolean returnValue, Thrower thrower)
    {
        if (!returnValue) return false;
        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_X)
        {
            xrayEnabled = !xrayEnabled;
            Minecraft.getMinecraft().gameSettings.ambientOcclusion = xrayEnabled ? 0 : oldValue;
            Minecraft.getMinecraft().gameSettings.gammaSetting = xrayEnabled ? 100f : oldValueGamma;
            Minecraft.getMinecraft().renderGlobal.loadRenderers();
        }
        return true;
    }

}
