package commu.protocol;

import commu.model.RobotAction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandRequest {
    private final Path audioFile;
    private final List<RobotAction> actions;

    public CommandRequest(Path audioFile, List<RobotAction> actions) {
        this.audioFile = audioFile;
        this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
    }

    public Path getAudioFile() {
        return audioFile;
    }

    public List<RobotAction> getActions() {
        return actions;
    }

    public void debug() {
        Boolean isAudioNull = audioFile == null;
        Boolean isActionsEmpty = actions.size() == 0;

        if (isAudioNull & isActionsEmpty) {
            System.out.println("no actions");
            return;
        }

        if (!isActionsEmpty) {
            System.out.println(audioFile);
        }

        if (!isActionsEmpty) {
            StringBuilder builder = new StringBuilder();
            for (RobotAction action: actions) {
                builder.append(action.getType() + ";");
            }
            System.out.println(builder.toString());
        }
    }
}
