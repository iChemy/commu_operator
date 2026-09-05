package commu.server;

import com.google.gson.Gson;

import commu.protocol.ClientRequest;
import commu.protocol.CommandDecoder;
import commu.protocol.CommandRequest;
import commu.protocol.GetPoseRequest;
import commu.robot.RobotController;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import jp.vstone.RobotLib.CRobotUtil;

public class ClientHandler {
    private static final String TAG = "ClientHandler";
    private static final Gson GSON = new Gson();

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
                ClientRequest request = decoder.decodeRequest(input);
                if (request instanceof CommandRequest) {
                    robot.execute((CommandRequest) request);
                    writeResponse(clientSocket, "OK\n");
                } else if (request instanceof GetPoseRequest) {
                    writeResponse(clientSocket, GSON.toJson(robot.getCurrentPose()) + "\n");
                } else {
                    throw new IllegalArgumentException("unsupported request");
                }
            } catch (Exception e) {
                CRobotUtil.Log(TAG, "Request failed: " + formatError(e));
                writeError(clientSocket, e);
            }
        } catch (IOException e) {
            CRobotUtil.Log(TAG, "Socket close failed: " + e.getMessage());
        }
    }

    private void writeError(Socket clientSocket, Exception error) {
        try {
            writeResponse(clientSocket, "ERR " + formatError(error) + "\n");
        } catch (Exception ignored) {
        }
    }

    private String formatError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return message;
    }

    private void writeResponse(Socket clientSocket, String response) throws IOException {
        clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
    }
}
