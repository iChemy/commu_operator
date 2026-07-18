package commu.audio;

import java.nio.file.Path;

import jp.vstone.RobotLib.CPlayWave;

public class SpeechPlayer {
    private static final Object LOCK = new Object();
    private static CPlayWave player;

    private SpeechPlayer() {
    }

    public static CPlayWave play(Path path) {
        synchronized (LOCK) {
            stop();
            player = CPlayWave.PlayWave(path.toString());
            return player;
        }
    }

    public static CPlayWave play(byte[] bytes) {
        synchronized (LOCK) {
            stop();
            player = CPlayWave.PlayWave(bytes);
            return player;
        }
    }

    public static void waitUntilFinished(CPlayWave target) throws InterruptedException {
        if (target == null) {
            return;
        }
        while (target.isPlaying()) {
            Thread.sleep(10);
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            if (player == null || !player.isPlaying()) {
                player = null;
                return;
            }

            player.stop();
            waitForStop(player);
            player = null;
        }
    }

    private static void waitForStop(CPlayWave target) {
        while (target.isPlaying()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
