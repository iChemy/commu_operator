package commu.model;

import java.nio.file.Path;

public class AudioAction implements RobotAction {
    private final Path audioFile;

    public AudioAction(Path audioFile) {
        this.audioFile = audioFile;
    }

    @Override
    public String getType() {
        return "audio";
    }

    public Path getAudioFile() {
        return audioFile;
    }
}
