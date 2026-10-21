package com.yearofice.sim.hunt;

import java.util.ArrayList;
import java.util.List;

/**
 * Arrow flight with drop and wind: gravity plus linear drag toward the moving air, {@code a = g + k (w - v)}, so a
 * crosswind pushes the shaft sideways and a headwind slows it. Semi-implicit Euler at a fixed step. Pure Java, y up.
 */
public final class Ballistics {
    private Ballistics() {}

    public record State(double x, double y, double z, double vx, double vy, double vz) {
        public double speed() { return Math.sqrt(vx * vx + vy * vy + vz * vz); }
    }

    public static State step(State s, double windX, double windZ, double dt, HuntTuning t) {
        double k = t.dragPerS();
        double vx = s.vx() + k * (windX - s.vx()) * dt;
        double vy = s.vy() - t.gravityMps2() * dt - k * s.vy() * dt;
        double vz = s.vz() + k * (windZ - s.vz()) * dt;
        return new State(s.x() + vx * dt, s.y() + vy * dt, s.z() + vz * dt, vx, vy, vz);
    }

    /** The path from {@code origin} along unit-ish {@code dir} at {@code speed}, every {@code dt} until {@code maxT}. */
    public static List<State> flight(double[] origin, double[] dir, double speed, double windX, double windZ, double dt, double maxT, HuntTuning t) {
        double n = Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        State s = new State(origin[0], origin[1], origin[2], dir[0] / n * speed, dir[1] / n * speed, dir[2] / n * speed);
        List<State> out = new ArrayList<>();
        out.add(s);
        for (double time = 0; time < maxT; time += dt) {
            s = step(s, windX, windZ, dt, t);
            out.add(s);
        }
        return out;
    }

    /** Drop in metres below the straight line at horizontal range {@code rangeM} along +z for a level shot, and the sideways drift along +x. */
    public static double[] dropAndDrift(double speed, double rangeM, double windX, HuntTuning t) {
        double dt = 0.005;
        State s = new State(0, 0, 0, 0, 0, speed);
        double time = 0;
        while (s.z() < rangeM && time < 10) { s = step(s, windX, 0, dt, t); time += dt; }
        return new double[] {-s.y(), s.x(), time};
    }
}
