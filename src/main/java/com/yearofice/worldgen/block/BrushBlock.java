package com.yearofice.worldgen.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * {@code willow_brush}: the leafy brush of a willow stool. No entity collision and a slowdown inside
 * (RECONCILE 93: the {@code brush} block). The species' own stool blocks replace it when the plants land (M5).
 */
public class BrushBlock extends Block {
    static final Vec3 SLOW = new Vec3(0.75, 0.9, 0.75);

    public BrushBlock(Properties p) { super(p); }

    static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.AZALEA_LEAVES).noCollission()
                .noOcclusion().isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                .pushReaction(PushReaction.DESTROY).isValidSpawn((s, l, p, e) -> false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }

    @Override
    protected void entityInside(BlockState s, Level level, BlockPos pos, Entity e) { e.makeStuckInBlock(s, SLOW); }
}
