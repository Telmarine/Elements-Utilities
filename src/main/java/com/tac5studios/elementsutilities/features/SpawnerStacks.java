package com.tac5studios.elementsutilities.features;

import com.tac5studios.elementsutilities.ElementsUtilities;
import com.tac5studios.elementsutilities.config.Feature;
import com.tac5studios.elementsutilities.config.Features;
import com.tac5studios.elementsutilities.config.Settings;
import com.tac5studios.elementsutilities.messages.Msg;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Spawner stacking.
 *  - Silk Touch keeps the spawner's mob. Spawner items only carry the mob, so items of the same mob stack in inventories.
 *  - Right-clicking a placed spawner with a spawner item of the same mob adds it to that spawner's stack (up to 12).
 *    Runs late, so claim mods can stop it. Sneak + right-click places the spawner as a new block instead.
 *  - Settings don't merge: the placed spawner keeps its own; the stack only multiplies its output.
 *  - Breaking with Silk Touch: the whole stack, or one with sneak (break_mode). Without Silk Touch one spawner of
 *    the stack is destroyed and nothing drops.
 *  - Explosions and Create drills and saws can't break a stack of 2 or more.
 */
@EventBusSubscriber(modid = ElementsUtilities.MOD_ID, value = Dist.DEDICATED_SERVER)
@SuppressWarnings("unused") // handlers are called by the event bus
public final class SpawnerStacks {

    static final String COUNT = ElementsUtilities.MOD_ID + ":spawners";
    static final String PAUSED = ElementsUtilities.MOD_ID + ":paused";

    /** Player -> (game tick, mob id) of the spawner item they are placing right now. */
    private static final Map<UUID, Object[]> PLACING = new HashMap<>();

    private SpawnerStacks() {}

    // ---------- stack data on the block entity ----------

    public static int count(SpawnerBlockEntity be) {
        return Math.max(1, be.getPersistentData().getInt(COUNT));
    }

    static void setCount(SpawnerBlockEntity be, int count) {
        if (count <= 1) be.getPersistentData().remove(COUNT);
        else be.getPersistentData().putInt(COUNT, count);
        be.setChanged();
    }

    static boolean paused(SpawnerBlockEntity be) {
        return be.getPersistentData().getBoolean(PAUSED);
    }

    static void setPaused(SpawnerBlockEntity be, boolean paused) {
        if (paused) be.getPersistentData().putBoolean(PAUSED, true);
        else be.getPersistentData().remove(PAUSED);
        be.setChanged();
    }

    /** True for a placed spawner holding 2 or more (used by the explosion and Create guards). */
    public static boolean isStack(Level level, BlockPos pos) {
        return level != null && level.getBlockEntity(pos) instanceof SpawnerBlockEntity be && count(be) > 1
                && Features.on(Feature.SPAWNER_STACKING);
    }

    // ---------- mob ids ----------

    /** The mob a placed spawner makes, e.g. "minecraft:zombie", or "" when it has none. */
    static String mobId(SpawnerBlockEntity be) {
        CompoundTag tag = be.getSpawner().save(new CompoundTag());
        return tag.getCompound("SpawnData").getCompound("entity").getString("id");
    }

    /** The mob a spawner item carries, or "". */
    static String mobId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null) return "";
        return data.copyTag().getCompound("SpawnData").getCompound("entity").getString("id");
    }

    /** A spawner item that carries only its mob, so items of the same mob always stack. */
    static ItemStack item(String mobId, int count) {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", mobId);
        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entity);
        CompoundTag be = new CompoundTag();
        be.putString("id", "minecraft:mob_spawner");
        be.put("SpawnData", spawnData);
        ItemStack stack = new ItemStack(Items.SPAWNER, count);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(be));
        return stack;
    }

    // ---------- adding to a stack, placing ----------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock e) {
        if (e.isCanceled() || e.getUseBlock() == TriState.FALSE || e.getLevel().isClientSide()) return;
        ItemStack held = e.getItemStack();
        if (!held.is(Items.SPAWNER) || !Features.on(Feature.SPAWNER_STACKING)) return;
        String mob = mobId(held);
        if (mob.isEmpty()) return;
        Player player = e.getEntity();

        if (!player.isShiftKeyDown() && e.getLevel().getBlockEntity(e.getPos()) instanceof SpawnerBlockEntity be && mob.equals(mobId(be))) {
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
            int n = count(be);
            int max = Settings.SPAWNER_MAX_STACK.get();
            if (n >= max) {
                if (player instanceof ServerPlayer sp) Msg.send(sp, "spawner.full", "max", max);
                return;
            }
            setCount(be, n + 1);
            if (!player.getAbilities().instabuild) held.shrink(1);
            e.getLevel().playSound(null, e.getPos(), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            if (player instanceof ServerPlayer sp) Msg.send(sp, "spawner.added", "count", n + 1, "max", max);
            return;
        }
        // Placing it as a new block: vanilla only applies item data for operators, so the mob is set after placing.
        PLACING.put(player.getUUID(), new Object[] {e.getLevel().getGameTime(), mob});
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof Player player) || !(e.getLevel() instanceof ServerLevel level)) return;
        Object[] placing = PLACING.remove(player.getUUID());
        if (placing == null || (long) placing[0] != level.getGameTime()) return;
        if (!(level.getBlockEntity(e.getPos()) instanceof SpawnerBlockEntity be)) return;
        String mob = (String) placing[1];
        if (mob.equals(mobId(be))) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(mob)).orElse(null);
        if (type == null) return;
        be.setEntityId(type, level.random);
        be.setChanged();
        level.sendBlockUpdated(e.getPos(), e.getPlacedBlock(), e.getPlacedBlock(), Block.UPDATE_ALL);
    }

    // ---------- breaking ----------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.isCanceled() || !(e.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(e.getPos()) instanceof SpawnerBlockEntity be) || !Features.on(Feature.SPAWNER_STACKING)) return;
        Player player = e.getPlayer();
        if (player.getAbilities().instabuild) return; // creative: plain vanilla break
        int n = count(be);
        String mob = mobId(be);
        boolean silk = silkTouch(level, player.getMainHandItem()) && !mob.isEmpty();

        if (!silk) {
            if (n <= 1) return; // vanilla: the spawner is destroyed
            setCount(be, n - 1);
            e.setCanceled(true);
            level.levelEvent(2001, e.getPos(), Block.getId(e.getState()));
            return;
        }
        String mode = Settings.SPAWNER_BREAK_MODE.get();
        boolean one = "one".equals(mode) || ("both".equals(mode) && player.isShiftKeyDown());
        e.setCanceled(true);
        if (one && n > 1) {
            setCount(be, n - 1);
            level.levelEvent(2001, e.getPos(), Block.getId(e.getState()));
            Block.popResource(level, e.getPos(), item(mob, 1));
            return;
        }
        level.destroyBlock(e.getPos(), false, player);
        Block.popResource(level, e.getPos(), item(mob, n));
    }

    private static boolean silkTouch(Level level, ItemStack tool) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                .map(h -> tool.getEnchantmentLevel(h) > 0)
                .orElse(false);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate e) {
        e.getAffectedBlocks().removeIf(pos -> isStack(e.getLevel(), pos));
    }
}
