package net.deizyn.bhcinema.client.freecam;

import net.deizyn.bhcinema.client.freecam.FlightMath.Motion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightMathTest {
    private static final double EPS = 1e-9;

    private static void assertMotion(double x, double y, double z, Motion m) {
        assertMotion(x, y, z, m, EPS);
    }

    private static void assertMotion(double x, double y, double z, Motion m, double eps) {
        assertEquals(x, m.x(), eps, "x");
        assertEquals(y, m.y(), eps, "y");
        assertEquals(z, m.z(), eps, "z");
    }

    @Test
    void forwardFollowsYaw() {
        assertMotion(0, 0, 1, FlightMath.direction(0, 0, false, 1, 0, 0));    // south
        assertMotion(-1, 0, 0, FlightMath.direction(90, 0, false, 1, 0, 0));  // west
        assertMotion(0, 0, -1, FlightMath.direction(180, 0, false, 1, 0, 0)); // north
        assertMotion(1, 0, 0, FlightMath.direction(-90, 0, false, 1, 0, 0)); // east
    }

    @Test
    void strafeMatchesVanillaInputVector() {
        // facing south, A (left) is east, D (right) is west
        assertMotion(1, 0, 0, FlightMath.direction(0, 0, false, 0, 1, 0));
        assertMotion(-1, 0, 0, FlightMath.direction(0, 0, false, 0, -1, 0));
        // facing west, left is south
        assertMotion(0, 0, 1, FlightMath.direction(90, 0, false, 0, 1, 0));
    }

    @Test
    void creativeStyleIgnoresPitch() {
        assertMotion(0, 0, 1, FlightMath.direction(0, 60, false, 1, 0, 0));
    }

    @Test
    void lookStyleFollowsPitch() {
        assertMotion(0, -1, 0, FlightMath.direction(0, 90, true, 1, 0, 0));  // looking straight down
        assertMotion(0, 1, 0, FlightMath.direction(0, -90, true, 1, 0, 0));  // looking straight up
        // strafing stays level even when looking down
        assertMotion(1, 0, 0, FlightMath.direction(0, 80, true, 0, 1, 0));
    }

    @Test
    void diagonalsAreNotFaster() {
        assertEquals(1.0, FlightMath.direction(37, 0, false, 1, 1, 0).length(), EPS);
        assertEquals(1.0, FlightMath.direction(37, 20, true, 1, -1, 1).length(), EPS);
        assertEquals(1.0, FlightMath.direction(0, 0, false, 0, 0, 1).length(), EPS);
    }

    @Test
    void lookAtMatchesFlightDirection() {
        assertEquals(0.0F, FlightMath.lookAt(0, 0, 5).yaw(), 1e-4);
        assertEquals(90.0F, FlightMath.lookAt(-5, 0, 0).yaw(), 1e-4);
        assertEquals(90.0F, FlightMath.lookAt(0, -5, 0).pitch(), 1e-4);
        // flying "forward" along the angles lookAt gives must head straight at the target
        FlightMath.Angles a = FlightMath.lookAt(3, -2, -6);
        Motion m = FlightMath.direction(a.yaw(), a.pitch(), true, 1, 0, 0);
        double len = Math.sqrt(9 + 4 + 36);
        assertMotion(3 / len, -2 / len, -6 / len, m, 1e-5); // angles are floats
    }

    @Test
    void leashKeepsCameraInsideDistance() {
        Motion anchor = new Motion(0, 0, 0);
        Motion near = new Motion(3, 0, 4);
        assertMotion(3, 0, 4, FlightMath.leash(near, anchor, 10));
        assertMotion(3, 0, 4, FlightMath.leash(new Motion(6, 0, 8), anchor, 5));
        // 0 = leash off
        assertMotion(60, 0, 80, FlightMath.leash(new Motion(60, 0, 80), anchor, 0));
    }

    @Test
    void orbitKeepsDistanceAndTurns() {
        Motion behind = new Motion(0, 0, -5); // camera north of the pivot, looking south at it
        assertMotion(0, 0, -5, FlightMath.orbit(behind, 0, 0), 1e-5);
        // turning the view 90° (to face west) swings the camera round to the east side
        assertMotion(5, 0, 0, FlightMath.orbit(behind, 90, 0), 1e-5);
        // tilting the view down lifts the camera, distance unchanged
        Motion lifted = FlightMath.orbit(behind, 0, 30);
        assertEquals(5.0, lifted.length(), 1e-5);
        assertEquals(2.5, lifted.y(), 1e-5);
        // never flips over the top
        assertEquals(5.0 * Math.sin(Math.toRadians(89)), FlightMath.orbit(behind, 0, 200).y(), 1e-4);
    }

    @Test
    void viewOffsetIsRelativeToTheView() {
        // facing south: right is west, up is up, forward is south
        assertMotion(-2, 0, 0, FlightMath.viewOffset(0, 0, 2, 0, 0));
        assertMotion(0, 3, 0, FlightMath.viewOffset(0, 0, 0, 3, 0));
        assertMotion(0, 0, 1, FlightMath.viewOffset(0, 0, 0, 0, 1));
        // looking straight down: screen-up points the way the camera faces (south), forward goes down
        assertMotion(0, 0, 1, FlightMath.viewOffset(0, 90, 0, 1, 0));
        assertMotion(0, -1, 0, FlightMath.viewOffset(0, 90, 0, 0, 1));
        // facing west: right is north
        assertMotion(0, 0, -1, FlightMath.viewOffset(90, 0, 1, 0, 0));
    }

    @Test
    void behindStaysAboveGroundWhenLookingDown() {
        // facing south: 4 blocks behind is 4 north
        assertMotion(0, 0, -4, FlightMath.behind(0, 0, 4), 1e-6);
        // looking down at the ground, "behind" is up and back, never below the eyes
        Motion back = FlightMath.behind(0, 60, 4);
        assertEquals(4.0, back.length(), 1e-6);
        assertEquals(4 * Math.sin(Math.toRadians(60)), back.y(), 1e-6);
        // and the eyes are then straight ahead of the camera
        FlightMath.Angles view = FlightMath.lookAt(-back.x(), -back.y(), -back.z());
        assertEquals(0.0F, view.yaw(), 1e-4);
        assertEquals(60.0F, view.pitch(), 1e-4);
    }

    @Test
    void approachIsFrameRateIndependent() {
        double oneStep = FlightMath.approach(0, 10, 0.2, 0.1);
        double half = FlightMath.approach(0, 10, 0.2, 0.05);
        double twoSteps = FlightMath.approach(half, 10, 0.2, 0.05);
        assertEquals(oneStep, twoSteps, 1e-9);
    }

    @Test
    void approachWithoutSmoothingSnaps() {
        assertEquals(10, FlightMath.approach(0, 10, 0, 0.016), EPS);
    }
}
