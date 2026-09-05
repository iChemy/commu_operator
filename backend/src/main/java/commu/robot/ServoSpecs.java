package commu.robot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jp.vstone.RobotLib.CCommUMotion;

public final class ServoSpecs {
    private static final List<ServoSpec> ALL;
    private static final Map<String, ServoSpec> BY_NAME;

    static {
        List<ServoSpec> specs = new ArrayList<>();
        specs.add(new ServoSpec("BODY_P", CCommUMotion.SV_BODY_P, -15, 15, 3.833));
        specs.add(new ServoSpec("BODY_Y", CCommUMotion.SV_BODY_Y, -67, 67, 1.0));
        specs.add(new ServoSpec("L_SHOULDER_P", CCommUMotion.SV_L_SHOULDER_P, -108, 108, 1.364));
        specs.add(new ServoSpec("L_SHOULDER_R", CCommUMotion.SV_L_SHOULDER_R, -45, 30, 1.0));
        specs.add(new ServoSpec("R_SHOULDER_P", CCommUMotion.SV_R_SHOULDER_P, -108, 108, 1.364));
        specs.add(new ServoSpec("R_SHOULDER_R", CCommUMotion.SV_R_SHOULDER_R, -30, 45, 1.0));
        specs.add(new ServoSpec("HEAD_P", CCommUMotion.SV_HEAD_P, -20, 25, 1.0));
        specs.add(new ServoSpec("HEAD_R", CCommUMotion.SV_HEAD_R, -15, 15, 4.333));
        specs.add(new ServoSpec("HEAD_Y", CCommUMotion.SV_HEAD_Y, -85, 85, 1.0));
        specs.add(new ServoSpec("EYE_P", CCommUMotion.SV_EYE_P, -22, 22, 1.0));
        specs.add(new ServoSpec("L_EYE_Y", CCommUMotion.SV_L_EYE_Y, -35, 20, 1.0));
        specs.add(new ServoSpec("R_EYE_Y", CCommUMotion.SV_R_EYE_Y, -20, 35, 1.0));
        specs.add(new ServoSpec("EYELIDS", CCommUMotion.SV_EYELIDs, -65, 3, 1.0));
        ALL = Collections.unmodifiableList(specs);

        Map<String, ServoSpec> byName = new LinkedHashMap<>();
        for (ServoSpec spec : specs) {
            byName.put(spec.getName(), spec);
        }
        BY_NAME = Collections.unmodifiableMap(byName);
    }

    private ServoSpecs() {
    }

    public static List<ServoSpec> all() {
        return ALL;
    }

    public static ServoSpec findByName(String name) {
        return BY_NAME.get(name);
    }
}
