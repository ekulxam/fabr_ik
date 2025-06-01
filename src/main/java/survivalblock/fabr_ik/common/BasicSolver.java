package survivalblock.fabr_ik.common;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Java (Minecraft-specific) implementation of the FABRIK Inverse Kinematics Algorithm
 * @author Survivalblock
 */
@SuppressWarnings({"unused", "ProtectedMemberInFinalClass"})
public final class BasicSolver {

    public static void solve(List<Vec3d> positions, Vec3d t) {
        solve(positions, t, Integer.MAX_VALUE);
    }

    public static void solve(List<Vec3d> positions, Vec3d t, final int maxIterations) {
        solve(positions, t, 0.1, maxIterations);
    }

    public static void solve(List<Vec3d> positions, Vec3d t, final double tolerance, final int maxIterations) {
        if (positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("positions cannot be empty!");
        }
        solveInternal(positions, getDistances(positions), t, tolerance, maxIterations);
    }

    public static void solve(List<Vec3d> positions, List<Double> distances, Vec3d t, final double tolerance, final int maxIterations) {
        if (positions == null || positions.isEmpty()) {
            throw new IllegalArgumentException("positions cannot be empty!");
        }
        if (distances == null || distances.isEmpty()) {
            throw new IllegalArgumentException("distances cannot be empty!");
        }
        if (positions.size() - 1 != distances.size()) {
            throw new IllegalStateException("Invalid size for distances was given! Expected " + (positions.size() - 1) + " but found " + distances.size());
        }
        solveInternal(positions, distances, t, tolerance, maxIterations);
    }

    /**
     * Implementation as described by
     * <a href="http://www.andreasaristidou.com/publications/papers/FABRIK.pdf">
     *     http://www.andreasaristidou.com/</a>
     * @param positions The <i>mutable</i> joint positions p<sub>i</sub> for i = 1,...,n. The first position should be the root chain.
     *                  Note that the distance d<sub>i</sub> between each joint is equivalent to |p<sub>i+1</sub> - p<sub>i</sub>| for i = 1,...,n-1.
     * @param t the target position
     * The positions parameter will have the updated positions (the new joint positions p<sub>i</sub> for i = 1,...,n).
     */
    protected static void solveInternal(List<Vec3d> positions, List<Double> distances, Vec3d t, final double tolerance, final int maxIterations) {
        int size = positions.size();

        // if max chain length > root to t distance
        if (distances.stream().mapToDouble(d -> d).sum() < positions.getFirst().distanceTo(t)) {
            for (int i = 0; i < size - 1; i++) {
                Vec3d pi = positions.get(i);
                double ri = t.distanceTo(pi);
                double lambdai = distances.get(i) / ri; // λi
                positions.set(i + 1, pi
                        .multiply((1 - lambdai))
                        .add( t.multiply(lambdai) ));
            }
            return;
        }

        final Vec3d b = positions.getFirst();
        double difA = positions.getLast().distanceTo(t);
        int iterations = 0;
        while (difA > tolerance) {
            if (iterations >= maxIterations) {
                break;
            }
            iterations++;
            // stage 1: forward reaching
            positions.set(size - 1, t); // pn = t
            Vec3d previous = positions.getLast();
            Vec3d current;
            for (int i = size - 2; i >= 0; i--) {  // n - 1 = (size - 1) - 1
                current = positions.get(i);
                double ri = previous.distanceTo(current);
                double lambdai = distances.get(i) / ri;
                positions.set(i, previous
                        .multiply((1 - lambdai))
                        .add( current.multiply(lambdai) ));
                previous = positions.get(i);
            }
            // stage 2: backward reaching
            positions.set(0, b);
            previous = positions.getFirst();
            for (int i = 1; i < size - 1; i++) {
                current = positions.get(i);
                double ri = current.distanceTo(previous);
                double lambdai = distances.get(i) / ri;
                positions.set(i, previous
                        .multiply((1 - lambdai))
                        .add( current.multiply(lambdai) ));
                previous = positions.get(i);
            }
            difA = positions.getLast().distanceTo(t);
        }
    }

    public static List<Double> getDistances(List<Vec3d> positions) {
        List<Double> distances = new ArrayList<>();
        Vec3d previous = positions.getFirst();
        Vec3d current;
        // [1,2,3,4,5], size = 5. position.getfirst = 1. i = 1, current = 2, i = 2, current = 3, i = 3, current = 4, i = 4, current = 5
        for (int i = 1; i < positions.size(); i++) {
            current = positions.get(i);
            distances.add(current.distanceTo(previous));
            previous = current;
        }
        return distances;
    }
}
