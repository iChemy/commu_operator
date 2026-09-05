package commu.protocol;

import commu.model.RobotAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandRequest implements ClientRequest {
    private final List<RobotAction> actions;

    public CommandRequest(List<RobotAction> actions) {
        this.actions = Collections.unmodifiableList(new ArrayList<>(actions));
    }

    public List<RobotAction> getActions() {
        return actions;
    }
}
