package commu;

import commu.robot.RobotController;
import commu.server.CommUServer;

import java.io.IOException;

import jp.vstone.RobotLib.CRobotUtil;

public class Main {
    private static final String TAG = "Main";
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        int port = readPort(args);
        CRobotUtil.Log(TAG, "Start command server on port " + port);

        try (RobotController robot = RobotController.connect()) {
            CommUServer server = new CommUServer(port, robot);
            server.start();
        } catch (IOException e) {
            CRobotUtil.Log(TAG, "Server error: " + e.getMessage());
        } catch (RuntimeException e) {
            CRobotUtil.Log(TAG, "Robot error: " + e.getMessage());
        }
    }

    private static int readPort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        if (args.length == 2 && "--port".equals(args[0])) {
            return Integer.parseInt(args[1]);
        }
        return Integer.parseInt(args[0]);
    }
}
