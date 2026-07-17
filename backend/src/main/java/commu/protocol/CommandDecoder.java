package commu.protocol;

import commu.model.AudioAction;
import commu.model.LedAction;
import commu.model.PoseAction;
import commu.model.RobotAction;
import commu.model.WaitAction;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import jp.vstone.RobotLib.CCommUMotion;

public class CommandDecoder {
    private static final int MAX_META_BYTES = 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;
    private static final Map<String, ServoSpec> SERVO_SPECS = createServoSpecs();

    private final Path audioDirectory;

    public CommandDecoder() {
        this(Paths.get("sound"));
    }

    public CommandDecoder(Path audioDirectory) {
        this.audioDirectory = audioDirectory;
    }

    public CommandRequest decode(InputStream inputStream) throws IOException {
        DataInputStream input = new DataInputStream(inputStream);
        Properties metadata = readMetadata(input);
        List<RobotAction> actions = readActions(input, metadata);
        return new CommandRequest(actions);
    }

    private Properties readMetadata(DataInputStream input) throws IOException {
        int metadataLength = input.readInt();
        if (metadataLength <= 0 || metadataLength > MAX_META_BYTES) {
            throw new IOException("invalid metadata length: " + metadataLength);
        }

        byte[] metadataBytes = new byte[metadataLength];
        input.readFully(metadataBytes);

        Properties metadata = new Properties();
        String text = new String(metadataBytes, StandardCharsets.UTF_8);
        metadata.load(new StringReader(text));
        return metadata;
    }

    private List<RobotAction> readActions(DataInputStream input, Properties metadata) throws IOException {
        int count = readInt(metadata, "actions", 0);
        List<RobotAction> actions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String prefix = "action." + i + ".";
            String type = metadata.getProperty(prefix + "type", "").trim().toLowerCase(Locale.ROOT);
            if ("audio".equals(type)) {
                int audioLength = readInt(metadata, prefix + "audio.length", 0);
                if (audioLength <= 0) {
                    throw new IOException("audio action requires audio payload");
                }
                actions.add(new AudioAction(readAudioPayload(input, i, audioLength)));
            } else if ("pose".equals(type)) {
                actions.add(new PoseAction(
                        readInt(metadata, prefix + "duration_ms", 1000),
                        parsePose(metadata.getProperty(prefix + "pose", ""))));
            } else if ("led".equals(type)) {
                actions.add(new LedAction(
                        readInt(metadata, prefix + "duration_ms", 0),
                        parseColor(metadata.getProperty(prefix + "led.body", "")),
                        parseColor(metadata.getProperty(prefix + "led.power_button", "")),
                        readInt(metadata, prefix + "led.left_cheek", -1),
                        readInt(metadata, prefix + "led.right_cheek", -1)));
            } else if ("wait".equals(type)) {
                actions.add(new WaitAction(readInt(metadata, prefix + "duration_ms", 0)));
            } else {
                throw new IOException("unknown action type: " + type);
            }
        }

        return actions;
    }

    private Path readAudioPayload(DataInputStream input, int actionIndex, int audioLength) throws IOException {
        Files.createDirectories(audioDirectory);
        Path audioFile = audioDirectory.resolve("action_" + actionIndex + ".wav");

        byte[] buffer = new byte[BUFFER_SIZE];
        int remaining = audioLength;
        try (java.io.OutputStream output = Files.newOutputStream(audioFile)) {
            while (remaining > 0) {
                int read = input.read(buffer, 0, Math.min(buffer.length, remaining));
                if (read < 0) {
                    throw new IOException("audio payload ended early");
                }
                output.write(buffer, 0, read);
                remaining -= read;
            }
        }
        return audioFile;
    }

    private Map<Byte, Short> parsePose(String value) throws IOException {
        Map<Byte, Short> pose = new HashMap<>();
        if (value == null || value.trim().isEmpty()) {
            return pose;
        }

        for (String entry : value.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split("=", 2);
            if (parts.length != 2) {
                throw new IOException("invalid pose entry: " + trimmed);
            }

            ServoSpec spec = SERVO_SPECS.get(parts[0].trim().toUpperCase(Locale.ROOT));
            if (spec == null) {
                throw new IOException("unknown servo: " + parts[0]);
            }
            pose.put(spec.id, spec.toRobotValue(readAngle(parts[1].trim(), trimmed)));
        }

        return pose;
    }

    private Color parseColor(String value) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String color = value.trim();
        if (color.startsWith("#")) {
            color = color.substring(1);
        }
        if (color.length() != 6) {
            throw new IOException("invalid color: " + value);
        }

        try {
            return new Color(Integer.parseInt(color, 16));
        } catch (NumberFormatException e) {
            throw new IOException("invalid color: " + value, e);
        }
    }

    private int readInt(Properties metadata, String key, int defaultValue) throws IOException {
        String value = metadata.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IOException("invalid integer " + key + "=" + value, e);
        }
    }

    private double readAngle(String value, String entry) throws IOException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IOException("invalid pose entry: " + entry, e);
        }
    }

    private static Map<String, ServoSpec> createServoSpecs() {
        Map<String, ServoSpec> specs = new HashMap<>();
        putSpec(specs, new ServoSpec(CCommUMotion.SV_BODY_P, -15, 15, 3.833), "BODY_P");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_BODY_Y, -67, 67, 1.0), "BODY_Y");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_L_SHOULDER_P, -108, 108, 1.364), "L_SHOULDER_P");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_L_SHOULDER_R, -45, 30, 1.0), "L_SHOULDER_R");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_R_SHOULDER_P, -108, 108, 1.364), "R_SHOULDER_P");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_R_SHOULDER_R, -30, 45, 1.0), "R_SHOULDER_R");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_HEAD_P, -20, 25, 1.0), "HEAD_P");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_HEAD_R, -15, 15, 4.333), "HEAD_R");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_HEAD_Y, -85, 85, 1.0), "HEAD_Y");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_EYE_P, -22, 22, 1.0), "EYE_P");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_L_EYE_Y, -35, 20, 1.0), "L_EYE_Y");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_R_EYE_Y, -20, 35, 1.0), "R_EYE_Y");
        putSpec(specs, new ServoSpec(CCommUMotion.SV_EYELIDs, -65, 3, 1.0), "EYELIDS");
        return specs;
    }

    private static void putSpec(Map<String, ServoSpec> specs, ServoSpec spec, String... names) {
        for (String name : names) {
            specs.put(name, spec);
        }
    }

    private static class ServoSpec {
        private final byte id;
        private final double minDegrees;
        private final double maxDegrees;
        private final double reductionRatio;

        ServoSpec(byte id, double minDegrees, double maxDegrees, double reductionRatio) {
            this.id = id;
            this.minDegrees = minDegrees;
            this.maxDegrees = maxDegrees;
            this.reductionRatio = reductionRatio;
        }

        short toRobotValue(double degrees) {
            double clamped = Math.max(minDegrees, Math.min(maxDegrees, degrees));
            return (short) Math.round(clamped * 10.0 * reductionRatio);
        }
    }
}
