package com.lach.apothicpots.client;

import com.lach.apothicpots.ApothicPots;
import com.lach.apothicpots.Net;
import com.lach.apothicpots.SpawnerInfo;
import com.lach.apothicpots.XpMath;
import java.util.List;
import net.darkhax.botanypots.common.impl.block.entity.AbstractBotanyPotBlockEntity;
import net.darkhax.botanypots.common.impl.block.menu.BotanyPotScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** XP tab on the left of the pot screen. Click it to take all the stored XP. */
@EventBusSubscriber(modid = ApothicPots.MODID, value = Dist.CLIENT)
public final class XpBar {
    private static final int W = 22, H = 74;
    private static final int BAR_X = 7, BAR_Y = 6, BAR_W = 8, BAR_H = 50;

    private XpBar() {}

    private static AbstractBotanyPotBlockEntity pot(BotanyPotScreen screen) {
        try {
            var ctx = screen.getMenu().getContext();
            return ctx == null ? null : ctx.pot();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean visible(BotanyPotScreen screen, AbstractBotanyPotBlockEntity pot) {
        Net.XpSync xp = Net.clientXp;
        boolean samePot = xp.pos().equals(pot.getBlockPos());
        return SpawnerInfo.of(pot.getSeedItem()) != null || (samePot && xp.xp() > 0);
    }

    private static int stored(AbstractBotanyPotBlockEntity pot) {
        Net.XpSync xp = Net.clientXp;
        return xp.pos().equals(pot.getBlockPos()) ? xp.xp() : 0;
    }

    private static final int LW = 22, LH = 30;

    private static int left(BotanyPotScreen s) { return s.getGuiLeft() - W + 1; }
    private static int leverTop(BotanyPotScreen s) { return top(s) + H + 3; }

    private static boolean hasLever(AbstractBotanyPotBlockEntity pot) {
        SpawnerInfo info = SpawnerInfo.of(pot.getSeedItem());
        return info != null && info.redstone();
    }

    private static boolean switchOn(AbstractBotanyPotBlockEntity pot) {
        Net.XpSync sync = Net.clientXp;
        return sync.pos().equals(pot.getBlockPos()) ? sync.switchOn() : com.lach.apothicpots.PotXp.switchOn(pot);
    }

    private static boolean insideLever(BotanyPotScreen s, double mx, double my) {
        int x = left(s), y = leverTop(s);
        return mx >= x && mx < x + LW - 1 && my >= y && my < y + LH;
    }

    private static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 2, y + h - 2, x + w, y + h - 1, 0xFF555555);
    }

    /** Lever tab: a lever with a lamp, lit while on. */
    private static void renderLever(GuiGraphics g, BotanyPotScreen screen, AbstractBotanyPotBlockEntity pot, int mx, int my) {
        int x = left(screen), y = leverTop(screen);
        boolean on = switchOn(pot);
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        panel(g, x, y, LW, LH);
        // The vanilla lever, flipped to match the state.
        var state = net.minecraft.world.level.block.Blocks.LEVER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                .setValue(net.minecraft.world.level.block.LeverBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(net.minecraft.world.level.block.LeverBlock.POWERED, on);
        g.pose().pushPose();
        g.pose().translate(x + 11, y + 7, 100);
        g.pose().scale(18, -18, 18);
        g.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));
        g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
        g.pose().translate(-0.5, -0.5, -0.5);
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, g.pose(), g.bufferSource(), 0xF000F0,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        g.flush();
        com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        g.pose().popPose();
        var font = Minecraft.getInstance().font;
        String label = on ? "ON" : "OFF";
        g.drawString(font, label, x + (LW - font.width(label)) / 2 + 1, y + LH - 10, on ? 0xFFE02020 : 0xFF404040, false);
        g.pose().popPose();
        if (insideLever(screen, mx, my)) {
            List<Component> tip = List.of(
                    Component.translatable(on ? "gui.apothicpots.switch_on" : "gui.apothicpots.switch_off")
                            .withStyle(on ? ChatFormatting.RED : ChatFormatting.DARK_GRAY),
                    Component.translatable("gui.apothicpots.switch_click").withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.apothicpots.switch_redstone").withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(Minecraft.getInstance().font, tip, mx, my);
        }
    }
    private static int top(BotanyPotScreen s) { return s.getGuiTop() + 4; }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof BotanyPotScreen screen)) return;
        AbstractBotanyPotBlockEntity pot = pot(screen);
        if (pot == null) return;
        if (hasLever(pot)) renderLever(event.getGuiGraphics(), screen, pot, event.getMouseX(), event.getMouseY());
        if (!visible(screen, pot)) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = left(screen), y = top(screen);
        int points = stored(pot);
        int capacity = Math.max(1, Net.clientXp.capacity());

        // Panel in the vanilla container style.
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(x, y, x + W, y + H, 0xFF000000);
        g.fill(x + 1, y + 1, x + W, y + H - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + W - 1, y + 2, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 2, y + H - 1, 0xFFFFFFFF);
        g.fill(x + 2, y + H - 2, x + W, y + H - 1, 0xFF555555);

        // Bar: fill tracks levels, so a little XP still shows.
        int bx = x + BAR_X, by = y + BAR_Y;
        g.fill(bx - 1, by - 1, bx + BAR_W + 1, by + BAR_H + 1, 0xFF373737);
        g.fill(bx, by, bx + BAR_W, by + BAR_H, 0xFF1A2A12);
        double frac = points <= 0 ? 0 : XpMath.exactLevels(points) / Math.max(1e-6, XpMath.exactLevels(capacity));
        int fill = points <= 0 ? 0 : Math.max(1, (int) Math.round(BAR_H * Math.min(1, frac)));
        if (fill > 0) {
            g.fillGradient(bx, by + BAR_H - fill, bx + BAR_W, by + BAR_H, 0xFFB8F84A, 0xFF3E9E14);
            g.fill(bx, by + BAR_H - fill, bx + 1, by + BAR_H, 0x40FFFFFF);
        }
        String lvl = Integer.toString(XpMath.levels(points));
        var font = Minecraft.getInstance().font;
        int tx = x + (W - font.width(lvl)) / 2 + 1, ty = y + BAR_Y + BAR_H + 6;
        g.drawString(font, lvl, tx + 1, ty, 0xFF000000, false);
        g.drawString(font, lvl, tx - 1, ty, 0xFF000000, false);
        g.drawString(font, lvl, tx, ty + 1, 0xFF000000, false);
        g.drawString(font, lvl, tx, ty - 1, 0xFF000000, false);
        g.drawString(font, lvl, tx, ty, 0xFF80FF20, false);
        g.pose().popPose();

        if (inside(screen, event.getMouseX(), event.getMouseY())) {
            List<Component> tip = List.of(
                    Component.translatable("gui.apothicpots.xp", points, XpMath.levels(points)).withStyle(ChatFormatting.GREEN),
                    Component.translatable(points > 0 ? "gui.apothicpots.xp_collect" : "gui.apothicpots.xp_empty").withStyle(ChatFormatting.GRAY));
            g.renderComponentTooltip(font, tip, event.getMouseX(), event.getMouseY());
        }
    }

    private static boolean inside(BotanyPotScreen s, double mx, double my) {
        int x = left(s), y = top(s);
        return mx >= x && mx < x + W - 1 && my >= y && my < y + H;
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof BotanyPotScreen screen)) return;
        AbstractBotanyPotBlockEntity pot = pot(screen);
        if (pot == null) return;
        if (hasLever(pot) && insideLever(screen, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            PacketDistributor.sendToServer(new Net.ToggleSwitch(pot.getBlockPos()));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
            return;
        }
        if (!visible(screen, pot) || !inside(screen, event.getMouseX(), event.getMouseY())) return;
        event.setCanceled(true);
        if (stored(pot) <= 0) return;
        PacketDistributor.sendToServer(new Net.CollectXp(pot.getBlockPos()));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }
}
