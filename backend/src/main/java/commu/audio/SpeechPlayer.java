package commu.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SpeechPlayer {
    private static final Object LOCK = new Object();
    private static Process process;

    private SpeechPlayer() {
    }

    public static Process play(Path path) throws IOException {
        synchronized (LOCK) {
            stop();
            process = new ProcessBuilder("aplay", path.toString()).start();
            return process;
        }
    }

    public static Process play(byte[] bytes) throws IOException {
        synchronized (LOCK) {
            Path path = Paths.get("sound", "current_command.wav");
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, bytes);
            return play(path);
        }
    }

    public static void waitUntilFinished(Process target) throws InterruptedException {
        if (target == null) {
            return;
        }
        target.waitFor();
    }

    public static void stop() {
        synchronized (LOCK) {
            if (process == null || !process.isAlive()) {
                process = null;
                return;
            }

            process.destroy();
            waitForStop(process);
            process = null;
        }
    }

    private static void waitForStop(Process target) {
        while (target.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
