package commu.robot;

public final class ServoSpec {
    private static final double OUTPUT_PRECISION = 1000.0;

    private final String name;
    private final byte id;
    private final double minDegrees;
    private final double maxDegrees;
    private final double reductionRatio;

    ServoSpec(String name, byte id, double minDegrees, double maxDegrees, double reductionRatio) {
        this.name = name;
        this.id = id;
        this.minDegrees = minDegrees;
        this.maxDegrees = maxDegrees;
        this.reductionRatio = reductionRatio;
    }

    public String getName() {
        return name;
    }

    public byte getId() {
        return id;
    }

    public short toRobotValue(double degrees) {
        double clamped = Math.max(minDegrees, Math.min(maxDegrees, degrees));
        return (short) Math.round(clamped * 10.0 * reductionRatio);
    }

    public double toDegrees(short robotValue) {
        double degrees = robotValue / (10.0 * reductionRatio);
        double rounded = Math.round(degrees * OUTPUT_PRECISION) / OUTPUT_PRECISION;
        return Math.max(minDegrees, Math.min(maxDegrees, rounded));
    }
}
