package vrprovider;

import jopenvr.HmdMatrix34_t;
import jopenvr.VRControllerAxis_t;
import jopenvr.VRControllerState_t;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static vrprovider.ControllerListener.LEFT_CONTROLLER;
import static vrprovider.ControllerListener.RIGHT_CONTROLLER;


/**
 * Created by John on 7/29/2016.
 */
public class VRState {
    private List<ControllerListener> controllerListeners = new ArrayList<>();

    public void addControllerListener(ControllerListener toAdd) {
        controllerListeners.add(toAdd);
    }

    // In the head frame
    private Matrix4f leftEyePose = new Matrix4f(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);
    private Matrix4f rightEyePose = new Matrix4f(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);

    // In the tracking system intertial frame
    private Matrix4f headPose = new Matrix4f(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1);

    // Controllers
    private static Matrix4f[] controllerPose = new Matrix4f[2];
    private static VRControllerState_t[] lastControllerState = new VRControllerState_t[2];

    VRState() {
        for (int c = 0; c < 2; c++) {
            lastControllerState[c] = new VRControllerState_t();
            for (int i = 0; i < 5; i++) {
                lastControllerState[c].rAxis[i] = new VRControllerAxis_t();
                controllerPose[c] = new Matrix4f();
            }

        }

    }

    public void setHeadPose(HmdMatrix34_t inputPose) {
        OpenVRUtil.setSteamVRMatrix3ToMatrix4f(inputPose, headPose);
    }

    public void setLeftEyePoseWRTHead(HmdMatrix34_t inputPose) {
        OpenVRUtil.setSteamVRMatrix3ToMatrix4f(inputPose, leftEyePose);
    }

    public void setRightEyePoseWRTHead(HmdMatrix34_t inputPose) {
        OpenVRUtil.setSteamVRMatrix3ToMatrix4f(inputPose, rightEyePose);
    }

    public void setLeftControllerPose(HmdMatrix34_t inputPose) {
        setControllerPose(inputPose, LEFT_CONTROLLER);
    }

    public void setRightControllerPose(HmdMatrix34_t inputPose) {
        setControllerPose(inputPose, RIGHT_CONTROLLER);
    }

    public void setControllerPose(HmdMatrix34_t inputPose, int nIndex) {
        OpenVRUtil.setSteamVRMatrix3ToMatrix4f(inputPose, controllerPose[nIndex]);
    }

    public Matrix4f getRightEyePose(HmdMatrix34_t inputPose) {
        return headPose.mul(rightEyePose);
    }

    public Matrix4f getLeftEyePose(HmdMatrix34_t inputPose) {
        return headPose.mul(leftEyePose);
    }

    public void updateControllerButtonState(
            VRControllerState_t[] controllerStateReference) {
        for (int c = 0; c < 2; c++) //each controller
        {
            // store previous state
            if (lastControllerState[c].ulButtonPressed != controllerStateReference[c].ulButtonPressed) {
                for (ControllerListener listener : controllerListeners) {
                    listener.buttonStateChanged(lastControllerState[c], controllerStateReference[c], c);
                }
            }
            lastControllerState[c].unPacketNum = controllerStateReference[c].unPacketNum;
            lastControllerState[c].ulButtonPressed = controllerStateReference[c].ulButtonPressed;
            lastControllerState[c].ulButtonTouched = controllerStateReference[c].ulButtonTouched;

            for (int i = 0; i < 5; i++) //5 axes but only [0] and [1] is anything, trigger and touchpad
            {
                if (controllerStateReference[c].rAxis[i] != null) {
                    lastControllerState[c].rAxis[i].x = controllerStateReference[c].rAxis[i].x;
                    lastControllerState[c].rAxis[i].y = controllerStateReference[c].rAxis[i].y;
                }
            }
        }
    }

}
