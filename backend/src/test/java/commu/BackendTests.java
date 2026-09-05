package commu;

import commu.model.PoseAction;
import commu.model.RobotAction;
import commu.model.WaitAction;
import commu.protocol.ClientRequest;
import commu.protocol.CommandDecoder;
import commu.protocol.CommandRequest;
import commu.protocol.GetPoseRequest;
import commu.robot.ServoSpec;
import commu.robot.ServoSpecs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BackendTests {
    private static int assertions;

    private BackendTests() {
    }

    public static void main(String[] args) throws Exception {
        testServoDefinitions();
        testRepresentativeAndBoundaryConversions();
        testConversionRoundTrips();
        testGetPoseRequestDecode();
        testLegacyCommandDecode();
        testUnknownRequestRejected();
        testGetPoseActionsRejected();

        System.out.println("BackendTests: OK (" + assertions + " assertions)");
    }

    private static void testServoDefinitions() {
        String[] expectedNames = {
                "BODY_P",
                "BODY_Y",
                "L_SHOULDER_P",
                "L_SHOULDER_R",
                "R_SHOULDER_P",
                "R_SHOULDER_R",
                "HEAD_P",
                "HEAD_R",
                "HEAD_Y",
                "EYE_P",
                "L_EYE_Y",
                "R_EYE_Y",
                "EYELIDS"
        };

        Collection<ServoSpec> specs = ServoSpecs.all();
        checkEquals(13, specs.size(), "there must be exactly 13 client-compatible servos");

        Set<String> names = new HashSet<>();
        Set<Byte> ids = new HashSet<>();
        for (ServoSpec spec : specs) {
            names.add(spec.getName());
            ids.add(spec.getId());
        }

        checkEquals(13, names.size(), "servo names must be unique");
        checkEquals(13, ids.size(), "servo IDs must be unique");
        for (int index = 0; index < expectedNames.length; index++) {
            String name = expectedNames[index];
            ServoSpec spec = requiredSpec(name);
            check(names.contains(name), "all() must contain " + name);
            checkEquals((byte) (index + 1), spec.getId(), name + " must use the CommU servo ID");
        }
    }

    private static void testRepresentativeAndBoundaryConversions() {
        ServoSpec headYaw = requiredSpec("HEAD_Y");
        checkEquals((short) 123, headYaw.toRobotValue(12.3), "HEAD_Y degree-to-raw conversion");
        checkClose(12.3, headYaw.toDegrees((short) 123), 1.0e-12, "HEAD_Y raw-to-degree conversion");
        checkEquals((short) -850, headYaw.toRobotValue(-85.0), "HEAD_Y lower boundary");
        checkEquals((short) 850, headYaw.toRobotValue(85.0), "HEAD_Y upper boundary");
        checkEquals((short) -850, headYaw.toRobotValue(-100.0), "HEAD_Y below range must clamp");
        checkEquals((short) 850, headYaw.toRobotValue(100.0), "HEAD_Y above range must clamp");

        ServoSpec bodyPitch = requiredSpec("BODY_P");
        checkEquals((short) 383, bodyPitch.toRobotValue(10.0), "BODY_P must apply its reduction ratio");
        checkClose(9.992, bodyPitch.toDegrees((short) 383), 1.0e-12,
                "BODY_P must invert its reduction ratio and use JSON-friendly precision");
        checkEquals((short) -575, bodyPitch.toRobotValue(-15.0), "BODY_P lower boundary");
        checkEquals((short) 575, bodyPitch.toRobotValue(15.0), "BODY_P upper boundary");
        checkClose(-15.0, bodyPitch.toDegrees((short) -575), 1.0e-12,
                "BODY_P decoded lower boundary must stay in the client range");
        checkClose(15.0, bodyPitch.toDegrees((short) 575), 1.0e-12,
                "BODY_P decoded upper boundary must stay in the client range");
        checkEquals((short) -575, bodyPitch.toRobotValue(-100.0), "BODY_P below range must clamp");
        checkEquals((short) 575, bodyPitch.toRobotValue(100.0), "BODY_P above range must clamp");

        ServoSpec eyelids = requiredSpec("EYELIDS");
        checkEquals((short) -650, eyelids.toRobotValue(-100.0), "EYELIDS lower clamp");
        checkEquals((short) 30, eyelids.toRobotValue(100.0), "EYELIDS upper clamp");
    }

    private static void testConversionRoundTrips() {
        double[][] ranges = {
                {-15, 15},
                {-67, 67},
                {-108, 108},
                {-45, 30},
                {-108, 108},
                {-30, 45},
                {-20, 25},
                {-15, 15},
                {-85, 85},
                {-22, 22},
                {-35, 20},
                {-20, 35},
                {-65, 3}
        };

        int index = 0;
        for (ServoSpec spec : ServoSpecs.all()) {
            double min = ranges[index][0];
            double max = ranges[index][1];
            double[] degrees = {min, min / 2.0, 0.0, max / 2.0, max};
            for (double degree : degrees) {
                short firstRaw = spec.toRobotValue(degree);
                double restoredDegrees = spec.toDegrees(firstRaw);
                short secondRaw = spec.toRobotValue(restoredDegrees);
                checkEquals(firstRaw, secondRaw,
                        spec.getName() + " degree -> raw -> degree -> raw must be stable");
            }
            index++;
        }
        checkEquals(ranges.length, index, "round-trip ranges must cover every servo");
    }

    private static void testGetPoseRequestDecode() throws Exception {
        ClientRequest request = decode("request=get_pose\n");
        check(request instanceof GetPoseRequest, "request=get_pose must decode as GetPoseRequest");
    }

    private static void testLegacyCommandDecode() throws Exception {
        String metadata = "actions=2\n"
                        + "action.0.type=pose\n"
                        + "action.0.duration_ms=750\n"
                        + "action.0.pose=HEAD_Y=12.3,BODY_P=-4\n"
                        + "action.1.type=wait\n"
                        + "action.1.duration_ms=125\n";
        ClientRequest request = decode(metadata);

        check(request instanceof CommandRequest, "metadata without request must remain a command");
        check(new CommandDecoder().decode(new ByteArrayInputStream(encodeMetadata(metadata))) != null,
                "the existing command-only decode API must remain compatible");
        List<RobotAction> actions = ((CommandRequest) request).getActions();
        checkEquals(2, actions.size(), "legacy command action count");
        check(actions.get(0) instanceof PoseAction, "first legacy action must be pose");
        check(actions.get(1) instanceof WaitAction, "second legacy action must be wait");

        PoseAction pose = (PoseAction) actions.get(0);
        checkEquals(750, pose.getDurationMs(), "legacy pose duration");
        Map<Byte, Short> values = pose.getPose();
        checkEquals((short) 123, values.get(requiredSpec("HEAD_Y").getId()), "legacy HEAD_Y value");
        checkEquals((short) -153, values.get(requiredSpec("BODY_P").getId()), "legacy BODY_P value");
        checkEquals(125, ((WaitAction) actions.get(1)).getDurationMs(), "legacy wait duration");
    }

    private static void testUnknownRequestRejected() {
        expectIOException("unknown request must be rejected", () -> decode("request=teleport\n"));
    }

    private static void testGetPoseActionsRejected() {
        expectIOException("get_pose with actions must be rejected", () -> decode(
                "request=get_pose\n"
                        + "actions=1\n"
                        + "action.0.type=wait\n"
                        + "action.0.duration_ms=10\n"));
    }

    private static ClientRequest decode(String metadata) throws IOException {
        return new CommandDecoder().decodeRequest(new ByteArrayInputStream(encodeMetadata(metadata)));
    }

    private static byte[] encodeMetadata(String metadata) throws IOException {
        byte[] metadataBytes = metadata.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(output)) {
            data.writeInt(metadataBytes.length);
            data.write(metadataBytes);
        }
        return output.toByteArray();
    }

    private static ServoSpec requiredSpec(String name) {
        ServoSpec spec = ServoSpecs.findByName(name);
        check(spec != null, "missing servo definition: " + name);
        return spec;
    }

    private static void expectIOException(String message, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (IOException expected) {
            assertions++;
            return;
        } catch (Exception other) {
            throw new AssertionError(message + ": expected IOException but got "
                    + other.getClass().getSimpleName(), other);
        }
        throw new AssertionError(message + ": expected IOException");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void checkEquals(int expected, int actual, String message) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void checkEquals(byte expected, byte actual, String message) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void checkEquals(short expected, short actual, String message) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void checkClose(double expected, double actual, double tolerance, String message) {
        assertions++;
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
