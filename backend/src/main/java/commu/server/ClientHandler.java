package commu.server;

import commu.protocol.CommandDecoder;
import commu.protocol.CommandRequest;
import commu.robot.RobotController;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import jp.vstone.RobotLib.CRobotUtil;

public class ClientHandler {
    private static final String TAG = "ClientHandler";

    private final Socket socket;
    private final CommandDecoder decoder;
    private final RobotController robot;

    public ClientHandler(Socket socket, CommandDecoder decoder, RobotController robot) {
        this.socket = socket;
        this.decoder = decoder;
        this.robot = robot;
    }

    public void handle() {
        try (Socket clientSocket = socket) {
            try {
                BufferedInputStream input = new BufferedInputStream(clientSocket.getInputStream());
                CommandRequest request = decoder.decode(input);
                request.debug();
                robot.execute(request);
                writeResponse(clientSocket, "OK\n");
            } catch (Exception e) {
                CRobotUtil.Log(TAG, "Command failed: " + e.getMessage());
                writeError(clientSocket, e);
            }
        } catch (IOException e) {
            CRobotUtil.Log(TAG, "Socket close failed: " + e.getMessage());
        }
    }

    private void writeError(Socket clientSocket, Exception error) {
        try {
            writeResponse(clientSocket, "ERR " + error.getMessage() + "\n");
        } catch (Exception ignored) {
        }
    }

    private void writeResponse(Socket clientSocket, String response) throws IOException {
        clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
    }
}
