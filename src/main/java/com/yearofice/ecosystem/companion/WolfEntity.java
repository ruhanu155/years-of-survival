package com.yearofice.ecosystem.companion;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.ai.ThreatSources;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.species.BearEntity;
import com.yearofice.ecosystem.species.rig.PredatorEntity;
import com.yearofice.sim.companion.CompanionTuning;
import com.yearofice.sim.companion.Trust;
import com.yearofice.sim.ecosystem.Awareness;
import com.yearofice.sim.ecosystem.Gaits;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * {@code yearofice:wolf}, wild or tamed (Open world in depth › Taming a wolf; Ideas from viewers › Taming a wolf
 * without bones). A wolf is only ever tamed through trust: it must be a den pup, an orphan or a lone yearling
 * ({@link #tameable}), and the player builds trust by leaving meat on the ground, hand-feeding, sitting near and
 * sharing kills ({@link Trust}). Bones do nothing. Wary and Tolerant wolves keep their distance; from Bonded (50)
 * the wolf follows, and from Companion (80) it hunts small game with the player and warns of bears. A tamed wolf
 * saves with its chunk. Every number is in {@link CompanionConfig}.
 */
public class WolfEntity extends PredatorEntity {
    public enum Command { FOLLOW, STAY, HUNT }

    public static final TagKey<Item> WOLF_FOOD = TagKey.create(Registries.ITEM, yoi("wolf_food"));
    static final double WARN_M = 60;
    static final int HUNT_RADIUS_M = 40;

    private static ResourceLocation yoi(String path) { return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path); }

    private UUID ownerId;
    private ServerPlayer ownerRef;
    private Trust trust;
    private boolean tameable, autoDays = true;
    private Command command = Command.FOLLOW;
    private String wolfName = "";
    private int ageDays = 60, warnings, sharedKills;
    private long lastDay = Long.MIN_VALUE, lastWarnTick = -1000, sitStartTick = -1, huntCooldownUntil;
    private int feedMinuteToday = -1, feedMinuteYesterday = -1;
    private boolean fedToday;
    private UUID sledId;

    public WolfEntity(EntityType<? extends WolfEntity> type, Level level) { super(type, level); }

    // ---- state ---------------------------------------------------------------------------------------------------------

    /** Marks this wolf as one that can be tamed (a den pup, an orphan or a lone yearling) at a starting trust. */
    public void makeTameable(double startTrust, int ageDays) {
        tameable = true;
        trust = new Trust(startTrust);
        this.ageDays = ageDays;
        applyScale();
    }

    public boolean tameable() { return tameable; }

    public boolean owned() { return ownerId != null; }

    public Trust trust() { return trust; }

    public Command command() { return command; }

    public void setCommand(Command c) { command = c; }

    public String wolfName() { return wolfName; }

    public void setName(String n) {
        wolfName = n;
        setCustomName(Component.literal(n));
    }

    public int ageDays() { return ageDays; }

    public int warnings() { return warnings; }

    public int sharedKills() { return sharedKills; }

    public UUID ownerId() { return ownerId; }

    public UUID sledId() { return sledId; }

    /** The sled places its harnessed wolves each tick. */
    void harnessPose(Vec3 at, float yaw, boolean running) {
        setPos(at.x, getY(), at.z);
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        setMove(running ? MOVE_RUN : MOVE_IDLE);
    }

    public void setSled(UUID id) { sledId = id; }

    public boolean harnessed() { return sledId != null; }

    /** GameTests drive the days themselves. */
    public void setAutoDays(boolean on) { autoDays = on; }

    public void setAgeDays(int d) {
        ageDays = d;
        applyScale();
    }

    public void setOwner(ServerPlayer p) {
        // M9 proxy: the slot spawner's demote pass removed a tamed wolf when its owner walked 934 m to an outpost (a wild body beyond
        // 160 m of every player is discarded); a companion is the player's, not the slot's, from the first trust action
        if (!slotKey().isEmpty()) com.yearofice.ecosystem.spawn.SlotSpawner.untrack(slotKey());
        setPersistenceRequired();
        ownerId = p.getUUID();
        ownerRef = p;
    }

    public ServerPlayer owner(ServerLevel level) {
        if (ownerRef != null && ownerRef.isAlive() && ownerRef.level() == level) return ownerRef;
        return ownerId != null && level.getPlayerByUUID(ownerId) instanceof ServerPlayer sp ? ownerRef = sp : null;
    }

    /** Pulls a sled: a Companion that is grown (Taming a wolf: 12 months on, 35-45 kg). */
    public boolean canPull() { return owned() && trust != null && trust.stage() == Trust.Stage.COMPANION && ageDays >= 365; }

    /** Pup 0.35 of full size, full grown at a year. */
    public double growth() { return Mth.clamp(ageDays / 365.0, 0, 1); }

    private void applyScale() {
        var attr = getAttribute(Attributes.SCALE);
        if (attr != null) attr.setBaseValue(0.35 + 0.65 * growth());
    }

    // ---- trust actions -------------------------------------------------------------------------------------------------

    /** One trust action by a player; the first one makes the player the owner. Returns the trust gained. */
    public double act(ServerPlayer p, Trust.Action a) {
        if (trust == null) return 0;
        if (ownerId == null) {
            if (!tameable) return 0;
            setOwner(p);
        }
        double g = trust.apply(a, CompanionConfig.tuning());
        if (g > 0) {
            long fm = SliceClock.minute(p.serverLevel());
            if (trust.value() >= Trust.TOLERANT) com.yearofice.story.ambition.Ambitions.fire(p, "wolf.tolerant", fm);
            if (trust.value() >= Trust.BONDED) com.yearofice.story.ambition.Ambitions.fire(p, "wolf.bonded", fm);
            if (a == Trust.Action.FEED_GROUND || a == Trust.Action.HAND_FEED || a == Trust.Action.SHARE_KILL) noteFed(p);
            if (trust.value() >= Trust.TOLERANT && wolfName.isEmpty()) {
                wolfName = WolfNames.pick(random);
                setCustomName(Component.literal(wolfName));
            }
        }
        return g;
    }

    private void noteFed(ServerPlayer p) {
        if (!(level() instanceof ServerLevel level)) return;
        long minute = SliceClock.minute(level);
        int mod = Calendar.minuteOfDayOfMinute(minute);
        if (!fedToday) {
            fedToday = true;
            feedMinuteToday = mod;
            if (feedMinuteYesterday >= 0 && Math.abs(feedMinuteYesterday - mod) <= 60) trust.apply(Trust.Action.ROUTINE, CompanionConfig.tuning());
            com.yearofice.story.Journal.record(p, "wolf", "feed", "Day " + (Calendar.absDayOfMinute(minute) + 1) + ". "
                    + (wolfName.isEmpty() ? "The wolf" : wolfName) + " ate at " + Math.round(distanceTo(p)) + " m today.", minute,
                    (int) Calendar.absDayOfMinute(minute));
        }
    }

    /** A strike costs 30 trust and does no harm (a tamed wolf is not hunted by its owner). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player && (owned() || tameable)) {
            if (trust != null) trust.lose(30);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide || !(player instanceof ServerPlayer sp) || trust == null || !(tameable || owned())) return InteractionResult.PASS;
        if (owned() && !sp.getUUID().equals(ownerId)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.is(WOLF_FOOD)) {
            if (trust.value() < Trust.TOLERANT) {
                sp.displayClientMessage(Component.literal("It backs away. Leave the meat on the ground and step back."), true);
                return InteractionResult.CONSUME;
            }
            if (act(sp, Trust.Action.HAND_FEED) > 0 && !player.getAbilities().instabuild) held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && owned() && trust.rank().ordinal() >= Trust.Rank.BONDED.ordinal()) {
            if (player.isShiftKeyDown()) {
                command = Command.values()[(command.ordinal() + 1) % Command.values().length];
                sp.displayClientMessage(Component.literal((wolfName.isEmpty() ? "The wolf" : wolfName) + ": " + command.name().toLowerCase()), true);
            } else {
                act(sp, Trust.Action.PET);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS; // bones, and anything else, do nothing
    }

    /** Trust comes from the attachment once it has been deserialised (it can land after readAdditionalSaveData). */
    private void adoptTrust() {
        if (adopted || !hasData(CompanionAttachments.TRUST.get())) return;
        List<Double> l = getData(CompanionAttachments.TRUST.get());
        if (l.isEmpty()) return;
        double[] d = new double[l.size()];
        for (int i = 0; i < d.length; i++) d[i] = l.get(i);
        trust = Trust.load(d);
        adopted = true;
    }

    private boolean adopted;

    // ---- the tick ------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        adoptTrust();
        if (!(level() instanceof ServerLevel level) || trust == null) return;
        if (tickCount == 1) applyScale();
        if (autoDays) {
            long day = Calendar.absDayOfMinute(SliceClock.minute(level));
            if (lastDay == Long.MIN_VALUE) lastDay = day;
            while (lastDay < day && isAlive()) {
                lastDay++;
                dailyStep(level, lastDay);
            }
        }
        if (tickCount % 20 == 0 && (tameable || owned())) {
            eatDroppedFood(level);
            sitNear(level);
        }
    }

    /** A new game day: trust settles, the wolf grows, and in late January and February it may leave. */
    public void dailyStep(ServerLevel level, long absDay) {
        if (trust == null) return;
        CompanionTuning t = CompanionConfig.tuning();
        trust.endDay(t);
        ageDays++;
        applyScale();
        boolean fed = fedToday;
        feedMinuteYesterday = fedToday ? feedMinuteToday : -1;
        fedToday = false;
        feedMinuteToday = -1;
        ServerPlayer owner = owner(level);
        if (owner == null || t.wolfLeaves() == CompanionTuning.NEVER || trust.value() >= Trust.COMPANION) return;
        int doy = Calendar.dayOfYearOfDay(absDay);
        int month = Calendar.month(doy);
        String who = wolfName.isEmpty() ? "your wolf" : wolfName;
        if (month == 1 && Calendar.dayOfMonth(doy) == 25) {
            owner.displayClientMessage(Component.literal("Late January: " + who + " is restless and watches the ridge."), false);
            com.yearofice.story.Journal.record(owner, "wolf", "restless", "Day " + (absDay + 1) + ". " + who + " is restless; the breeding season is near.",
                    absDay * 1440, (int) absDay);
        }
        if (month != 2) return;
        boolean roofed = !level.canSeeSky(blockPosition());
        double p = com.yearofice.sim.companion.WolfLeaving.dailyP(t, trust.value(), fed && roofed);
        if (random.nextDouble() < p) leave(owner, com.yearofice.sim.companion.WolfLeaving.returnOffset(t, new java.util.SplittableRandom(random.nextLong())));
    }

    /** The wolf walks off to a pack. {@code returnInDays} 0 means for good; else a record waits on the player. */
    public void leave(ServerPlayer owner, int returnInDays) {
        ServerLevel level = (ServerLevel) level();
        long day = Calendar.absDayOfMinute(SliceClock.minute(level));
        String who = wolfName.isEmpty() ? "Your wolf" : wolfName;
        if (returnInDays > 0) Companions.store(owner, this, day + returnInDays);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.yearofice.api.event.WolfLeavesEvent(owner, who, returnInDays));
        com.yearofice.story.Journal.record(owner, "wolf", "companion.wolf_leaves", "Day " + (day + 1) + ". " + who + " left in the night to find a pack"
                + (returnInDays > 0 ? "; it may come back in the spring." : "."), SliceClock.minute(level), (int) day);
        owner.displayClientMessage(Component.literal(who + " has gone to find a pack."), false);
        discard();
    }

    private void eatDroppedFood(ServerLevel level) {
        if (trust == null) return;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(6, 2, 6), i -> i.getItem().is(WOLF_FOOD))) {
            if (!(item.getOwner() instanceof ServerPlayer thrower) || (ownerId != null && !thrower.getUUID().equals(ownerId))) continue;
            // the wolf eats only while the nearest player is beyond 15 m minus 1 m for every 5 trust
            if (distanceTo(thrower) <= 15 - trust.value() / 5) continue;
            ItemStack st = item.getItem();
            if (act(thrower, Trust.Action.FEED_GROUND) <= 0) continue; // the day's limit: it leaves the meat
            st.shrink(1);
            if (st.isEmpty()) item.discard();
            return;
        }
    }

    /** Sit still (crouched, looking away) within 20 m for 10 game minutes: +1, twice a day. */
    private void sitNear(ServerLevel level) {
        ServerPlayer o = ownerId == null ? null : owner(level);
        Player p = o != null ? o : level.getNearestPlayer(this, 20);
        if (!(p instanceof ServerPlayer sp) || distanceTo(p) > 20 || !p.isCrouching() || lookAngle(p) <= 30) {
            sitStartTick = -1;
            return;
        }
        long t = level.getGameTime();
        if (sitStartTick < 0) sitStartTick = t;
        if (t - sitStartTick >= 10 * SliceClock.TICKS_PER_MINUTE) {
            act(sp, Trust.Action.SIT);
            sitStartTick = -1;
        }
    }

    private double lookAngle(Player p) {
        Vec3 to = position().subtract(p.getEyePosition()).normalize();
        double dot = Mth.clamp(p.getLookAngle().dot(to), -1, 1);
        return Math.toDegrees(Math.acos(dot));
    }

    // ---- behaviour -----------------------------------------------------------------------------------------------------

    private boolean companionAi() { return trust != null && (tameable || owned()); }

    /** A wolf the player is taming never flees the player: Wary ones back off, the rest stay and learn. */
    @Override
    public void think(net.minecraft.server.level.ServerLevel level) {
        if (companionAi()) {
            awareness().decay(level.getGameTime());
            return;
        }
        super.think(level);
    }

    @Override
    protected void tickRoutine(ServerLevel level) {
        if (!companionAi()) {
            super.tickRoutine(level);
            return;
        }
        if (harnessed()) return;
        long t = level.getGameTime();
        ServerPlayer o = owner(level);
        if (chase != null) {
            tickChase(level, species().predator());
            return;
        }
        warnOfBears(level, o, t);
        if (o == null) {
            Player near = level.getNearestPlayer(this, 40);
            if (near != null && distanceTo(near) < 10) stepAway(near);
            else idle();
            return;
        }
        double d = distanceTo(o);
        switch (trust.rank()) {
            case WARY -> {
                if (d < 10) stepAway(o);
                else idle();
            }
            case TOLERANT -> {
                getLookControl().setLookAt(o, 30, 30);
                if (d < 3) stepAway(o);
                else idle();
            }
            default -> {
                if (command == Command.STAY) {
                    idle();
                    return;
                }
                if (command == Command.HUNT) huntWith(level, o, t);
                if (chase != null) return;
                follow(o, d);
            }
        }
    }

    private void idle() {
        getNavigation().stop();
        setMove(MOVE_IDLE);
    }

    private void stepAway(LivingEntity from) {
        Vec3 away = position().subtract(from.position());
        away = new Vec3(away.x, 0, away.z).normalize().scale(12);
        if (getNavigation().isDone()) getNavigation().moveTo(getX() + away.x, getY(), getZ() + away.z, Gaits.input(species().walkMps()));
        setMove(MOVE_WALK);
    }

    private void follow(ServerPlayer o, double d) {
        if (d > 48 && trust.stage() == Trust.Stage.COMPANION) {
            teleportTo(o.getX() + 3, o.getY(), o.getZ() + 3);
            getNavigation().stop();
            return;
        }
        if (d > 10) {
            if (getNavigation().isDone() || tickCount % 10 == 0)
                getNavigation().moveTo(o, Gaits.input(d > 25 ? species().runMps() : species().trotMps()));
            setMove(d > 25 ? MOVE_RUN : MOVE_WALK);
        } else if (d < 6) {
            idle();
        }
    }

    /** Hunt-with: the wolf takes small game near its owner and flushes ground birds ahead of them. */
    private void huntWith(ServerLevel level, ServerPlayer o, long t) {
        var rule = species().predator();
        if (tickCount % 10 == 0) {
            for (com.yearofice.ecosystem.YoiBird b : level.getEntitiesOfClass(com.yearofice.ecosystem.YoiBird.class, getBoundingBox().inflate(15, 4, 15),
                    b -> b.isAlive() && java.util.Objects.equals(b.testGroup(), testGroup())))
                b.awareness().spike(this, Awareness.MAX, t);
        }
        if (rule == null || t < huntCooldownUntil || tickCount % 20 != 0) return;
        YoiAnimal prey = findPrey(level, rule);
        if (prey != null && prey.distanceTo(o) <= HUNT_RADIUS_M) {
            huntCooldownUntil = t + 200;
            beginChase(prey, rule);
        }
    }

    @Override
    protected void endChase(boolean killed) {
        boolean had = chase != null;
        super.endChase(killed);
        if (had && killed && owned() && level() instanceof ServerLevel level) {
            ServerPlayer o = owner(level);
            if (o != null && distanceTo(o) <= HUNT_RADIUS_M) {
                sharedKills++;
                act(o, Trust.Action.SHARE_KILL);
            }
        }
    }

    /** Stands, faces a bear within 60 m and growls (a juvenile or better, the owner's wolf only). */
    private void warnOfBears(ServerLevel level, ServerPlayer o, long t) {
        if (!owned() || trust.stage() == Trust.Stage.PUP || t - lastWarnTick < 100) return;
        List<LivingEntity> threats = new ArrayList<>(ThreatSources.near(level, position(), WARN_M, this));
        for (BearEntity b : level.getEntitiesOfClass(BearEntity.class, getBoundingBox().inflate(WARN_M, 16, WARN_M),
                b -> b.isAlive() && java.util.Objects.equals(b.testGroup(), testGroup()))) threats.add(b);
        BearEntity bear = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : threats) {
            if (e instanceof BearEntity b && distanceTo(b) <= WARN_M && distanceTo(b) < bd) {
                bear = b;
                bd = distanceTo(b);
            }
        }
        if (bear == null) return;
        lastWarnTick = t;
        warnings++;
        idle();
        getLookControl().setLookAt(bear, 60, 60);
        level.playSound(null, blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.NEUTRAL, 1.0f, 0.9f);
        if (o != null) o.displayClientMessage(Component.literal((wolfName.isEmpty() ? "Your wolf" : wolfName) + " growls: a bear, " + Math.round(bd) + " m."), true);
    }

    // ---- save ----------------------------------------------------------------------------------------------------------

    /** A tamed wolf saves with its chunk (Minecraft development › The wolf); the wild ones are re-drawn from the slots. */
    @Override
    public boolean shouldBeSaved() { return owned(); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (trust == null) return;
        if (ownerId != null) tag.putUUID("yoi_owner", ownerId);
        java.util.List<Double> tl = new java.util.ArrayList<>();
        for (double d : trust.save()) tl.add(d);
        setData(CompanionAttachments.TRUST.get(), tl);
        tag.putBoolean("yoi_has", true);
        tag.putBoolean("yoi_tameable", tameable);
        tag.putString("yoi_command", command.name());
        tag.putString("yoi_name", wolfName);
        tag.putInt("yoi_age", ageDays);
        tag.putInt("yoi_feed_yesterday", feedMinuteYesterday);
        tag.putLong("yoi_last_day", lastDay);
        if (sledId != null) tag.putUUID("yoi_sled", sledId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (!tag.contains("yoi_trust") && !tag.contains("yoi_has")) return;
        // 0.4.0: trust is the yearofice:trust attachment (read lazily, see adoptTrust); the old NBT list is read for one version
        if (tag.contains("yoi_trust")) {
            ListTag l = tag.getList("yoi_trust", Tag.TAG_DOUBLE);
            double[] d = new double[l.size()];
            for (int i = 0; i < d.length; i++) d[i] = l.getDouble(i);
            trust = Trust.load(d);
        } else if (hasData(CompanionAttachments.TRUST.get())) adoptTrust();
        else trust = new Trust(0);
        ownerId = tag.hasUUID("yoi_owner") ? tag.getUUID("yoi_owner") : null;
        tameable = tag.getBoolean("yoi_tameable");
        command = Command.valueOf(tag.getString("yoi_command"));
        wolfName = tag.getString("yoi_name");
        ageDays = tag.getInt("yoi_age");
        feedMinuteYesterday = tag.getInt("yoi_feed_yesterday");
        lastDay = tag.contains("yoi_last_day") ? tag.getLong("yoi_last_day") : Long.MIN_VALUE;
        sledId = tag.hasUUID("yoi_sled") ? tag.getUUID("yoi_sled") : null;
        if (!wolfName.isEmpty()) setCustomName(Component.literal(wolfName));
        applyScale();
    }
}
