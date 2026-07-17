package commu.robot;

import commu.audio.SpeechPlayer;
import commu.model.AudioAction;
import commu.model.PoseAction;
import commu.model.RobotAction;
import commu.model.WaitAction;
import commu.protocol.CommandRequest;

import java.io.IOException;

import jp.vstone.RobotLib.CCommUMotion;
import jp.vstone.RobotLib.CRobotMem;
import jp.vstone.RobotLib.CRobotPose;
import jp.vstone.RobotLib.CRobotUtil;

public class RobotController implements AutoCloseable {
    private static final String TAG = "RobotController";

    private final CCommUMotion motion;
    private boolean servoOn;

    private RobotController(CRobotMem memory, CCommUMotion motion) {
        this.motion = motion;
    }

    public static RobotController connect() {
        CRobotMem memory = new CRobotMem();
        CCommUMotion motion = new CCommUMotion(memory);

        if (!memory.Connect()) {
            throw new IllegalStateException("failed to connect VSMD");
        }

        motion.InitRobot_CommU();
        CRobotUtil.Log(TAG, "Rev. " + memory.FirmwareRev.get());
        CRobotUtil.Log(TAG, "Servo On");
        motion.ServoOn();

        RobotController controller = new RobotController(memory, motion);
        controller.servoOn = true;
        return controller;
    }

    public synchronized void execute(CommandRequest request) throws IOException, InterruptedException {
        CRobotUtil.Log(TAG, "Received command: actions=" + request.getActions().size());

        int index = 1;
        for (RobotAction action : request.getActions()) {
            executeAction(action, index, request.getActions().size());
            index++;
        }
    }

    private void executeAction(RobotAction action, int index, int total) throws IOException {
        if (action instanceof AudioAction) {
            executeAudio((AudioAction) action, index, total);
        } else if (action instanceof PoseAction) {
            executePose((PoseAction) action, index, total);
        } else if (action instanceof WaitAction) {
            executeWait((WaitAction) action, index, total);
        } else {
            throw new IllegalArgumentException("unsupported action type: " + action.getType());
        }
    }

    private void executeAudio(AudioAction action, int index, int total) throws IOException {
        CRobotUtil.Log(TAG, "audio action " + index + "/" + total + ": " + action.getAudioFile());
        SpeechPlayer.play(action.getAudioFile());
    }

    private void executePose(PoseAction action, int index, int total) {
        CRobotPose pose = new CRobotPose();

        if (!action.getPose().isEmpty()) {
            pose.SetPose(action.getPose());
        }
        if (action.hasLed()) {
            pose.setLED_CommU(
                    action.getBodyLed(),
                    action.getLeftCheek(),
                    action.getRightCheek(),
                    action.getPowerButtonLed());
        }

        CRobotUtil.Log(TAG, "pose action " + index + "/" + total + ": " + action.getDurationMs() + " ms");
        motion.play(pose, action.getDurationMs());
        motion.waitEndinterpAll();
    }

    private void executeWait(WaitAction action, int index, int total) {
        CRobotUtil.Log(TAG, "wait action " + index + "/" + total + ": " + action.getDurationMs() + " ms");
        CRobotUtil.wait(action.getDurationMs());
    }

    @Override
    public synchronized void close() {
        if (!servoOn) {
            return;
        }

        CRobotUtil.Log(TAG, "Servo Off");
        SpeechPlayer.stop();
        motion.ServoOff();
        servoOn = false;
    }
}
