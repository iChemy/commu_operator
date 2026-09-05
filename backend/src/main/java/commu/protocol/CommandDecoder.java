package commu.protocol;

import commu.model.AudioAction;
import commu.model.PoseAction;
import commu.model.RobotAction;
import commu.model.WaitAction;
import commu.robot.ServoSpec;
import commu.robot.ServoSpecs;

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

public class CommandDecoder {
    private static final int MAX_META_BYTES = 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;

    private final Path audioDirectory;

    public CommandDecoder() {
        this(Paths.get("sound"));
    }

    public CommandDecoder(Path audioDirectory) {
        this.audioDirectory = audioDirectory;
    }

    public CommandRequest decode(InputStream inputStream) throws IOException {
        ClientRequest request = decodeRequest(inputStream);
        if (!(request instanceof CommandRequest)) {
            throw new IOException("expected command request");
        }
        return (CommandRequest) request;
    }

    public ClientRequest decodeRequest(InputStream inputStream) throws IOException {
        DataInputStream input = new DataInputStream(inputStream);
        Properties metadata = readMetadata(input);
        String requestType = metadata.getProperty("request", "command").trim().toLowerCase(Locale.ROOT);
        if ("get_pose".equals(requestType)) {
            if (readInt(metadata, "actions", 0) != 0) {
                throw new IOException("get_pose request does not accept actions");
            }
            return new GetPoseRequest();
        }
        if (!"command".equals(requestType)) {
            throw new IOException("unknown request: " + requestType);
        }
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
                rejectDuration(metadata, prefix, "audio");
                int audioLength = readInt(metadata, prefix + "audio.length", 0);
                if (audioLength <= 0) {
                    throw new IOException("audio action requires audio payload");
                }
                actions.add(new AudioAction(readAudioPayload(input, i, audioLength)));
            } else if ("pose".equals(type)) {
                actions.add(new PoseAction(
                        readInt(metadata, prefix + "duration_ms", 1000),
                        parsePose(metadata.getProperty(prefix + "pose", "")),
                        parseColor(metadata.getProperty(prefix + "led.body", "")),
                        parseColor(metadata.getProperty(prefix + "led.power_button", "")),
                        readInt(metadata, prefix + "led.left_cheek", -1),
                        readInt(metadata, prefix + "led.right_cheek", -1)));
            } else if ("led".equals(type)) {
                throw new IOException("led is not an action type; put led inside a pose action");
            } else if ("wait".equals(type)) {
                actions.add(new WaitAction(readInt(metadata, prefix + "duration_ms", 0)));
            } else {
                throw new IOException("unknown action type: " + type);
            }
        }

        return actions;
    }

    private void rejectDuration(Properties metadata, String prefix, String type) throws IOException {
        if (metadata.getProperty(prefix + "duration_ms") != null) {
            throw new IOException(type + " action does not accept duration_ms");
        }
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

            ServoSpec spec = ServoSpecs.findByName(parts[0].trim().toUpperCase(Locale.ROOT));
            if (spec == null) {
                throw new IOException("unknown servo: " + parts[0]);
            }
            pose.put(spec.getId(), spec.toRobotValue(readAngle(parts[1].trim(), trimmed)));
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

}
