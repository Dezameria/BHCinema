package net.deizyn.bhcinema.client.freecam;

/** Flight maths for the free camera, kept free of Minecraft classes so it can be unit tested. */
public final class FlightMath {
    private FlightMath() {
    }

    public record Motion(double x, double y, double z) {
        public double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }
    }

    /**
     * World-space direction for the held movement keys, using the same yaw convention as vanilla's
     * {@code Entity.getInputVector} (yaw 0 faces +Z). {@code forward}, {@code strafe} (positive = left) and
     * {@code vertical} (positive = up) are in [-1, 1]. The result is clamped to length 1 so diagonals are not faster.
     * With {@code followPitch} forward/back follows where the camera looks (spectator-style); otherwise it stays
     * horizontal like creative flight. Strafing is always horizontal.
     */
    public static Motion direction(float yawDeg, float pitchDeg, boolean followPitch,
                                   double forward, double strafe, double vertical) {
        double yaw = Math.toRadians(yawDeg);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        double fx = -sin;
        double fy = 0.0;
        double fz = cos;
        if (followPitch) {
            double pitch = Math.toRadians(pitchDeg);
            double cosPitch = Math.cos(pitch);
            fx *= cosPitch;
            fz *= cosPitch;
            fy = -Math.sin(pitch);
        }

        double x = fx * forward + cos * strafe;
        double y = fy * forward + vertical;
        double z = fz * forward + sin * strafe;

        double length = Math.sqrt(x * x + y * y + z * z);
        if (length > 1.0) {
            x /= length;
            y /= length;
            z /= length;
        }
        return new Motion(x, y, z);
    }

    public record Angles(float yaw, float pitch) {
    }

    /**
     * Offset that puts a camera {@code distance} behind a point it looks through with this yaw/pitch, so the point
     * ends up straight ahead in the middle of the view (where F5 puts the camera).
     */
    public static Motion behind(float yawDeg, float pitchDeg, double distance) {
        // direction() clamps to length 1, so scale afterwards
        Motion back = direction(yawDeg, pitchDeg, true, -1.0, 0.0, 0.0);
        return new Motion(back.x() * distance, back.y() * distance, back.z() * distance);
    }

    /** Yaw/pitch (vanilla convention, degrees) that look along the vector (dx, dy, dz). */
    public static Angles lookAt(double dx, double dy, double dz) {
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontal));
        return new Angles(yaw, pitch);
    }

    /**
     * World-space offset for a shift given in the camera's own frame: {@code right}, {@code up} and {@code forward}
     * blocks as seen through a camera with this yaw/pitch (roll ignored). Up tilts with the pitch, so "up" is always
     * the top of the screen.
     */
    public static Motion viewOffset(float yawDeg, float pitchDeg, double right, double up, double forward) {
        Motion f = direction(yawDeg, pitchDeg, true, 1.0, 0.0, 0.0);
        Motion l = direction(yawDeg, pitchDeg, false, 0.0, 1.0, 0.0);
        // screen-up = forward x left
        double ux = f.y() * l.z() - f.z() * l.y();
        double uy = f.z() * l.x() - f.x() * l.z();
        double uz = f.x() * l.y() - f.y() * l.x();
        return new Motion(
                f.x() * forward - l.x() * right + ux * up,
                f.y() * forward - l.y() * right + uy * up,
                f.z() * forward - l.z() * right + uz * up);
    }

    /**
     * Swings a camera around a pivot. {@code fromPivot} is the camera position relative to the pivot; the result keeps
     * its distance, turned by {@code dYaw} and tilted by {@code dPitch} degrees as seen by a camera looking at the
     * pivot (so it matches turning the view by the same amounts). Pitch stops just short of straight up/down.
     */
    public static Motion orbit(Motion fromPivot, float dYaw, float dPitch) {
        double distance = fromPivot.length();
        if (distance < 1.0e-6) {
            return fromPivot;
        }
        Angles view = lookAt(-fromPivot.x(), -fromPivot.y(), -fromPivot.z());
        float pitch = Math.max(-89.0F, Math.min(89.0F, view.pitch() + dPitch));
        Motion dir = direction(view.yaw() + dYaw, pitch, true, 1.0, 0.0, 0.0);
        return new Motion(-dir.x() * distance, -dir.y() * distance, -dir.z() * distance);
    }

    /**
     * Where a camera at {@code cam} must be to stay within {@code maxDistance} of {@code anchor}: unchanged while
     * inside the leash, otherwise pulled straight towards the anchor onto the leash sphere.
     */
    public static Motion leash(Motion cam, Motion anchor, double maxDistance) {
        double dx = cam.x() - anchor.x();
        double dy = cam.y() - anchor.y();
        double dz = cam.z() - anchor.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (maxDistance <= 0.0 || distance <= maxDistance) {
            return cam;
        }
        double k = maxDistance / distance;
        return new Motion(anchor.x() + dx * k, anchor.y() + dy * k, anchor.z() + dz * k);
    }

    /**
     * Frame-rate independent exponential approach: after {@code tau} seconds {@code current} has covered ~63% of the
     * way to {@code target}, whatever the frame time. {@code tau <= 0} snaps straight to the target.
     */
    public static double approach(double current, double target, double tau, double dt) {
        if (tau <= 0.0) {
            return target;
        }
        return current + (target - current) * (1.0 - Math.exp(-dt / tau));
    }
}
