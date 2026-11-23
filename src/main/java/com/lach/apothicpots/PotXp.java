package com.lach.apothicpots;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** XP points stored in a Botany Pot, kept as a block entity attachment. */
public final class PotXp {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ApothicPots.MODID);
    public static final Supplier<AttachmentType<Integer>> XP = ATTACHMENTS.register("xp",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    /** Redstone-control pots: the on/off lever in the pot screen. */
    public static final Supplier<AttachmentType<Boolean>> SWITCH = ATTACHMENTS.register("switch",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());
    /** Last redstone signal seen, so a change in signal flips the lever. */
    public static final Supplier<AttachmentType<Boolean>> LAST_SIGNAL = ATTACHMENTS.register("last_signal",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    private PotXp() {}

    public static boolean switchOn(BlockEntity be) {
        return be.getExistingData(SWITCH).orElse(false);
    }

    public static void setSwitch(BlockEntity be, boolean on) {
        if (switchOn(be) == on) return;
        be.setData(SWITCH, on);
        be.setChanged();
        if (be.getLevel() != null && !be.getLevel().isClientSide) {
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    /** Server: a change in the outside redstone signal sets the lever to match. */
    public static void syncSignal(BlockEntity be, boolean powered) {
        boolean last = be.getExistingData(LAST_SIGNAL).orElse(false);
        if (last == powered) return;
        be.setData(LAST_SIGNAL, powered);
        be.setChanged();
        setSwitch(be, powered);
    }

    public static void init(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        modBus.addListener(PotXp::registerCaps);
    }

    public static int get(BlockEntity be) {
        return be.getExistingData(XP).orElse(0);
    }

    public static void set(BlockEntity be, int xp) {
        int clamped = Math.max(0, Math.min(Config.XP_CAPACITY.get(), xp));
        if (clamped == get(be)) return;
        if (clamped == 0) be.removeData(XP);
        else be.setData(XP, clamped);
        be.setChanged();
    }

    public static void add(BlockEntity be, int xp) {
        if (xp > 0) set(be, (int) Math.min(Integer.MAX_VALUE, (long) get(be) + xp));
    }

    /** Fluid that pipes drain, or null when no XP fluid mod is installed. */
    @Nullable
    public static Fluid fluid() {
        String id = Config.XP_FLUID.get();
        if (!id.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(id.trim());
            if (rl != null) {
                Fluid f = BuiltInRegistries.FLUID.get(rl);
                if (f != Fluids.EMPTY) return f;
            }
        }
        for (var holder : BuiltInRegistries.FLUID.getTagOrEmpty(Tags.Fluids.EXPERIENCE)) {
            Fluid f = holder.value();
            if (f != Fluids.EMPTY && f.isSource(f.defaultFluidState())) return f;
        }
        return null;
    }

    private static void registerCaps(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BotanyPotBlockEntity.TYPE.get(), (be, side) -> {
            Fluid fluid = fluid();
            return fluid == null ? null : new Handler(be, fluid);
        });
    }

    /** Drain-only tank view over the stored XP. */
    private record Handler(BlockEntity be, Fluid fluid) implements IFluidHandler {
        private int mbPerXp() { return Config.MB_PER_XP.get(); }

        @Override public int getTanks() { return 1; }

        @Override public FluidStack getFluidInTank(int tank) {
            int mb = get(be) * mbPerXp();
            return mb <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, mb);
        }

        @Override public int getTankCapacity(int tank) { return Config.XP_CAPACITY.get() * mbPerXp(); }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != fluid) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            int points = Math.min(get(be), maxDrain / mbPerXp());
            if (points <= 0) return FluidStack.EMPTY;
            if (action.execute()) set(be, get(be) - points);
            return new FluidStack(fluid, points * mbPerXp());
        }
    }
}
