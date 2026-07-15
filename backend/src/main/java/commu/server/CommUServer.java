package commu.server;

import commu.protocol.CommandDecoder;
import commu.robot.RobotController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import jp.vstone.RobotLib.CRobotUtil;

public class CommUServer {
    private static final String TAG = "CommUServer";

    private final int port;
    private final RobotController robot;
    private final CommandDecoder decoder;

    public CommUServer(int port, RobotController robot) {
        this(port, robot, new CommandDecoder());
    }

    public CommUServer(int port, RobotController robot, CommandDecoder decoder) {
        this.port = port;
        this.robot = robot;
        this.decoder = decoder;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            CRobotUtil.Log(TAG, "Listening on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, decoder, robot).handle();
            }
        }
    }
}
