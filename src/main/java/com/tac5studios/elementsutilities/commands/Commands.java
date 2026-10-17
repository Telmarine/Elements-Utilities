package com.tac5studios.elementsutilities.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.core.Mods;
import com.tac5studios.elementsutilities.features.ItemCleanup;
import com.tac5studios.elementsutilities.features.LagScan;
import com.tac5studios.elementsutilities.messages.Msg;
import com.tac5studios.elementsutilities.perms.Perm;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /utilities lag [page]   busiest chunks, 3 per page, only to the person who asked
 * /utilities cleanup      clear dropped items now (after the chat countdown)
 * /utilities reload       re-read messages.toml and re-check the overlap guard
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class Commands {

    private static final int PER_PAGE = 3;
    /** A page number reuses the last scan for this long, so paging shows the same list. */
    private static final long REUSE_MS = 120_000;

    private record Report(long madeAt, List<LagScan.ChunkStat> chunks) {}

    private static final Map<String, Report> REPORTS = new HashMap<>();

    private Commands() {}

    @SubscribeEvent
    @SuppressWarnings("unused") // called by the event bus
    public static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(build());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = net.minecraft.commands.Commands.literal("utilities")
                .requires(s -> Perm.has(s, Perm.RELOAD) || Perm.has(s, Perm.LAG) || Perm.has(s, Perm.CLEANUP));
        root.then(net.minecraft.commands.Commands.literal("lag")
                .requires(s -> Perm.has(s, Perm.LAG))
                .executes(c -> lag(c, 1, true))
                .then(net.minecraft.commands.Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(c -> lag(c, IntegerArgumentType.getInteger(c, "page"), false))));
        root.then(net.minecraft.commands.Commands.literal("cleanup")
                .requires(s -> Perm.has(s, Perm.CLEANUP))
                .executes(Commands::cleanup));
        root.then(net.minecraft.commands.Commands.literal("reload")
                .requires(s -> Perm.has(s, Perm.RELOAD))
                .executes(Commands::reload));
        return root;
    }

    private static int lag(CommandContext<CommandSourceStack> c, int page, boolean fresh) {
        CommandSourceStack source = c.getSource();
        if (!Features.on(Feature.LAG_REPORT)) {
            Msg.fail(source, "lag.off");
            return 0;
        }
        String who = source.getTextName();
        long now = System.currentTimeMillis();
        Report report = REPORTS.get(who);
        if (fresh || report == null || now - report.madeAt() > REUSE_MS) {
            List<LagScan.ChunkStat> all = LagScan.scan(source.getServer());
            report = new Report(now, List.copyOf(all.subList(0, Math.min(all.size(), Settings.LAG_TOP.get()))));
            REPORTS.put(who, report);
        }
        List<LagScan.ChunkStat> chunks = report.chunks();
        if (chunks.isEmpty()) {
            Msg.send(source, "lag.empty");
            return 1;
        }
        int pages = (chunks.size() + PER_PAGE - 1) / PER_PAGE;
        int p = Math.min(page, pages);
        Msg.send(source, "lag.header", "page", p, "pages", pages);
        for (int i = (p - 1) * PER_PAGE; i < Math.min(chunks.size(), p * PER_PAGE); i++) {
            source.sendSuccess(line(i + 1, chunks.get(i)), false);
        }
        if (pages > 1) source.sendSuccess(() -> pager(p, pages), false);
        return 1;
    }

    private static java.util.function.Supplier<Component> line(int rank, LagScan.ChunkStat s) {
        int x = s.chunkX * 16 + 8;
        int z = s.chunkZ * 16 + 8;
        String top = s.topEntity().isEmpty() ? Msg.raw("lag.none") : s.topEntity();
        String tp = "/execute in " + s.dimension.location() + " run tp @s " + x + " ~ " + z;
        return () -> Msg.text("lag.line",
                        "rank", rank, "dim", s.dimension.location(), "x", x, "z", z,
                        "entities", s.entities, "block_entities", s.blockEntities, "hoppers", s.hoppers, "top", top)
                .withStyle(st -> st.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Msg.text("lag.teleport_hover").withStyle(ChatFormatting.GRAY))));
    }

    private static Component pager(int page, int pages) {
        MutableComponent out = Component.empty();
        if (page > 1) out.append(Msg.button("lag.prev", "lag.prev_hover", "/utilities lag " + (page - 1)));
        if (page > 1 && page < pages) out.append(" ");
        if (page < pages) out.append(Msg.button("lag.next", "lag.next_hover", "/utilities lag " + (page + 1)));
        return out;
    }

    private static int cleanup(CommandContext<CommandSourceStack> c) {
        CommandSourceStack source = c.getSource();
        if (!Features.on(Feature.ITEM_CLEANUP)) {
            Msg.fail(source, "cleanup.off");
            return 0;
        }
        int wait = ItemCleanup.start(source.getServer());
        if (wait < 0) {
            Msg.fail(source, "cleanup.running");
            return 0;
        }
        if (wait > 0) Msg.send(source, "cleanup.started", "seconds", wait);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> c) {
        Msg.load();
        Mods.detect();
        Mods.log();
        Msg.sendLogged(c.getSource(), "cmd.reloaded", "guarded", Mods.guardedCount());
        return 1;
    }
}
