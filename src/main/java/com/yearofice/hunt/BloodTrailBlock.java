package com.yearofice.hunt;

import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.hunt.HuntTuning;
import com.yearofice.sim.hunt.WoundModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One patch of blood on the ground (Blueprint › The hit-zone model › Blood trail data). {@code kind} is the look
 * (bright with froth from the lungs, dark from the liver, green-flecked from the gut), {@code amount} 1 to 3 is how
 * many drops fell on the cell. It fades on a scheduled tick after two game hours, halved in rain and doubled in snow.
 * Right mouse reads it: the look, and the way the trail heads.
 */
public final class BloodTrailBlock extends Block {
    public enum Kind implements StringRepresentable {
        BRIGHT("bright", "Bright red, with pink froth: a lung hit."),
        DARK("dark", "Dark red and steady: the liver, or a limb."),
        GUT("gut", "Sparse, with green stomach matter: a gut hit.");

        private final String id, read;

        Kind(String id, String read) { this.id = id; this.read = read; }

        @Override
        public String getSerializedName() { return id; }

        public String read() { return read; }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, 3);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 0.5, 15);

    public BloodTrailBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.BRIGHT).setValue(AMOUNT, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(KIND, AMOUNT); }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override
    protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) { return l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP); }

    @Override
    protected BlockState updateShape(BlockState s, Direction d, BlockState n, net.minecraft.world.level.LevelAccessor l, BlockPos p, BlockPos np) {
        return d == Direction.DOWN && !canSurvive(s, l, p) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : s;
    }

    @Override
    protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) { l.removeBlock(p, false); }

    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level l, BlockPos p, Player pl, BlockHitResult h) {
        if (pl instanceof ServerPlayer sp) {
            BlockPos next = BloodTrail.next(sp.serverLevel(), p, 6);
            String way = next == null ? "The trail goes no further here." : "The drops lead " + BloodTrail.compass(next.getX() - p.getX(), next.getZ() - p.getZ()) + ".";
            sp.displayClientMessage(Component.literal(s.getValue(KIND).read() + " " + way), true);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    /** Ticks a drop lasts under the weather, from the game-minute rate now. */
    static int lifeTicks(ServerLevel level, BlockPos pos, HuntTuning t) {
        WoundModel.Weather w = WoundModel.Weather.CLEAR;
        if (level.isRainingAt(pos.above())) w = WoundModel.Weather.RAIN;
        else if (level.isRaining() && level.getBiome(pos).value().coldEnoughToSnow(pos)) w = WoundModel.Weather.SNOW;
        int ds = ClockService.installed() ? Math.max(1, ClockService.get().dsPerTick()) : 1;
        return (int) Math.min(Integer.MAX_VALUE / 2, Math.max(20, WoundModel.dropLifeMin(w, t) * 600.0 / ds));
    }

    static double minute(ServerLevel l) { return SliceClock.minuteD(l); }
}
