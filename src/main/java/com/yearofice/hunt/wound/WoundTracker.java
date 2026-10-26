package com.yearofice.hunt.wound;

import com.yearofice.api.event.AnimalWoundedEvent;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.hunt.BloodTrail;
import com.yearofice.hunt.HuntConfig;
import com.yearofice.hunt.HuntItems;
import com.yearofice.sim.hunt.HuntData;
import com.yearofice.sim.hunt.Shot.Channel;
import com.yearofice.sim.hunt.WoundModel;
import com.yearofice.sim.hunt.WoundModel.Wound;
import com.yearofice.story.ambition.Ambitions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * Every wounded animal on the server and what its wound does (Blueprint › The hit-zone model; Wounds that last). One
 * {@link State} per animal: the {@link Wound}, the way it has run, whether it has bedded, and the arrows in it. The
 * tracker steps each animal every tick: blood patches along its path at the wound's rate, the run to its first bed,
 * the bed itself, the push when the player comes close too soon, death at the timer and the limp of a leg wound. The
 * state is also written to the animal's persistent data ({@code yoi_wound}), so a wound outlasts a reload and a leg
 * wound is still there weeks later.
 */
public final class WoundTracker {
    private WoundTracker() {}

    static final ResourceLocation LEG_SLOW = ResourceLocation.fromNamespaceAndPath("yearofice", "leg_wound");
    /** A player closer than this to a bedded wounded animal before the wait is up pushes it (m). */
    public static final double PUSH_DISTANCE_M = 30;

    public static final class State {
        public Wound wound;
        public final List<String> arrows = new ArrayList<>();
        public final WoundModel.Emitter emitter = new WoundModel.Emitter();
        public Vec3 last;
        public double travelledM, legM, runTargetM, dropsPlaced;
        public boolean bedded, driven = true;
        /** False counts drops without laying blocks (long synthetic walks in tests). */
        public boolean place = true;
        public @Nullable UUID shooter;
        public Vec3 awayFrom = Vec3.ZERO;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static void clear() { STATES.clear(); }

    public static @Nullable State of(LivingEntity e) { return STATES.get(e.getUUID()); }

    public static boolean isWounded(LivingEntity e) { return STATES.containsKey(e.getUUID()); }

    /** Opens a wound on {@code e}. {@code driven} false leaves the animal where the caller moves it (tests). */
    public static State wound(ServerLevel level, LivingEntity e, Wound w, @Nullable ServerPlayer shooter, String headId, double runRoll, boolean driven) {
        State s = STATES.computeIfAbsent(e.getUUID(), k -> new State());
        s.wound = w;
        s.arrows.add(headId);
        s.last = e.position();
        s.driven = driven;
        s.runTargetM = WoundModel.runM(w, runRoll);
        s.shooter = shooter == null ? null : shooter.getUUID();
        s.awayFrom = shooter == null ? e.position() : shooter.position();
        if (w.channel() == Channel.LEG) slow(e);
        if (driven && e instanceof Mob m) flee(m, s, s.runTargetM);
        save(e, s);
        NeoForge.EVENT_BUS.post(new AnimalWoundedEvent(e, shooter, w.channel().name(), w.channel().name().toLowerCase()));
        return s;
    }

    /** Runs the animal away from where the shot came from. */
    static void flee(Mob m, State s, double metres) {
        Vec3 away = m.position().subtract(s.awayFrom);
        if (away.horizontalDistanceSqr() < 1e-4) away = new Vec3(1, 0, 0);
        Vec3 t = m.position().add(away.multiply(1, 0, 1).normalize().scale(metres));
        m.getNavigation().moveTo(t.x, t.y, t.z, 1.6);
    }

    static void slow(LivingEntity e) {
        var attr = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && !attr.hasModifier(LEG_SLOW)) attr.addPermanentModifier(new AttributeModifier(LEG_SLOW, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    /**
     * Steps one animal to {@code now}: used by the tick and, with a moved position, by GameTests. Returns the drops placed.
     */
    public static int advance(ServerLevel level, LivingEntity e, State s, Vec3 now, double minute) {
        double moved = Math.sqrt((now.x - s.last.x) * (now.x - s.last.x) + (now.z - s.last.z) * (now.z - s.last.z));
        int drops = s.emitter.drops(s.wound, s.travelledM, moved);
        if (drops > 0) {
            if (s.place) {
                var kind = BloodTrail.kindOf(s.wound.channel());
                Vec3 dir = moved < 1e-6 ? new Vec3(1, 0, 0) : new Vec3((now.x - s.last.x) / moved, 0, (now.z - s.last.z) / moved);
                for (int i = 0; i < drops; i++) {
                    double f = (i + 1.0) / drops, side = (i % 2 == 0 ? 1 : -1) * 0.35;
                    Vec3 p = s.last.add((now.x - s.last.x) * f, 0, (now.z - s.last.z) * f).add(-dir.z * side, 0, dir.x * side);
                    BloodTrail.drop(level, BlockPos.containing(p.x, now.y, p.z), kind);
                }
            }
            s.dropsPlaced += drops;
        }
        s.travelledM += moved;
        s.last = now;
        if (s.wound.lethal() && minute >= s.wound.deathMin()) { die(level, e, s); return drops; }
        if (s.wound.channel() == Channel.VITALS && s.travelledM >= s.runTargetM) { die(level, e, s); return drops; }
        if (s.wound.channel() == Channel.LEG && WoundModel.healed(s.wound, minute)) heal(e);
        return drops;
    }

    /** Pushes a bedded (or running) animal: runs again 300-800 m, death timer +2 h, trail halved. */
    public static void push(ServerLevel level, LivingEntity e, State s, double roll) {
        s.wound = s.wound.pushed(HuntConfig.tuning());
        s.bedded = false;
        s.travelledM = 0;
        s.runTargetM = WoundModel.runM(s.wound, roll);
        if (e instanceof Mob m && s.driven) flee(m, s, s.runTargetM);
        save(e, s);
    }

    static void die(ServerLevel level, LivingEntity e, State s) {
        STATES.remove(e.getUUID());
        ServerPlayer sp = s.shooter == null ? null : level.getServer().getPlayerList().getPlayer(s.shooter);
        DamageSource src = level.damageSources().generic();
        for (String head : s.arrows) {
            var h = HuntData.head(head);
            if (level.random.nextDouble() < h.reuse()) {
                ItemEntity ie = new ItemEntity(level, e.getX(), e.getY() + 0.3, e.getZ(), HuntItems.arrow(head, 1));
                level.addFreshEntity(ie);
            }
        }
        if (e instanceof YoiAnimal a) {
            if (sp != null && a.speciesPath().equals("grizzly")) {
                Ambitions.fire(sp, "hunt.grizzly_bow", (long) SliceClock.minuteD(level));
            }
            DeathPipeline.kill(a, src, "arrow");
        } else e.kill();
    }

    static void heal(LivingEntity e) {
        STATES.remove(e.getUUID());
        var attr = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null) attr.removeModifier(LEG_SLOW);
        e.getPersistentData().remove("yoi_wound");
    }

    public static void tick(ServerLevel level) {
        if (STATES.isEmpty()) return;
        double minute = SliceClock.minuteD(level);
        for (var it : new ArrayList<>(STATES.entrySet())) {
            State s = it.getValue();
            LivingEntity e = level.getEntity(it.getKey()) instanceof LivingEntity le ? le : null;
            if (e == null || e.isRemoved()) continue; // unloaded animals keep their state
            if (!s.driven) continue; // a test moves it and calls advance itself
            advance(level, e, s, e.position(), minute);
            if (e.isRemoved()) continue;
            if (s.wound.channel() == Channel.LIVER || s.wound.channel() == Channel.GUT) {
                double leg = s.wound.channel() == Channel.GUT && s.wound.bedEveryM() > 0 ? s.wound.bedEveryM() : s.runTargetM;
                if (!s.bedded && s.travelledM >= Math.min(s.runTargetM, leg > 0 ? leg : s.runTargetM)) {
                    s.bedded = true;
                    if (e instanceof Mob m) m.getNavigation().stop();
                    save(e, s);
                }
                if (s.bedded && !s.wound.waitOver(minute)) {
                    ServerPlayer near = level.getNearestPlayer(e, PUSH_DISTANCE_M) instanceof ServerPlayer p ? p : null;
                    if (near != null) push(level, e, s, level.random.nextDouble());
                }
            }
        }
    }

    // ---- persistence ----

    static void save(LivingEntity e, State s) {
        CompoundTag t = new CompoundTag();
        Wound w = s.wound;
        t.putString("channel", w.channel().name());
        t.putDouble("hit", w.hitMin());
        t.putDouble("die", w.dieAfterMin());
        t.putInt("pushes", w.pushes());
        t.putDouble("trail", w.trailMult());
        t.putDouble("travelled", s.travelledM);
        t.putBoolean("bedded", s.bedded);
        ListTag a = new ListTag();
        for (String h : s.arrows) a.add(StringTag.valueOf(h));
        t.put("arrows", a);
        e.getPersistentData().put("yoi_wound", t);
    }

    /** Rebuilds a state from an animal's saved wound when it joins a level. */
    public static void onJoin(ServerLevel level, LivingEntity e) {
        CompoundTag t = e.getPersistentData().getCompound("yoi_wound");
        if (t.isEmpty() || STATES.containsKey(e.getUUID())) return;
        Channel c = Channel.valueOf(t.getString("channel"));
        double roll = 0.5;
        Wound base = WoundModel.open(c, t.getDouble("hit"), 1, roll);
        double die = t.getDouble("die");
        Wound w = new Wound(c, t.getDouble("hit"), c == Channel.LEG ? Double.POSITIVE_INFINITY : die, base.dropsPer10m(), base.trailLimitM(),
                base.waitMin(), base.runMinM(), base.runMaxM(), base.bedEveryM(), base.healDays(), t.getInt("pushes"), t.getDouble("trail"), base.look());
        State s = new State();
        s.wound = w;
        s.last = e.position();
        s.travelledM = t.getDouble("travelled");
        s.bedded = t.getBoolean("bedded");
        s.runTargetM = WoundModel.runM(w, roll);
        for (var el : t.getList("arrows", 8)) s.arrows.add(el.getAsString());
        STATES.put(e.getUUID(), s);
        if (c == Channel.LEG) slow(e);
    }
}
