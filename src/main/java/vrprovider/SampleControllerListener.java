package vrprovider;

import jopenvr.VRControllerState_t;

import static vrprovider.OpenVRUtil.switchedDown;
import static vrprovider.OpenVRUtil.switchedUp;

/**
 * Created by John on 7/29/2016.
 */
public class SampleControllerListener implements ControllerListener {
    @Override
    public void buttonStateChanged(VRControllerState_t stateBefore, VRControllerState_t stateAfter, int nController) {
        System.out.println("controller state changed.");
        if (switchedDown(k_buttonTrigger, stateBefore.ulButtonPressed, stateAfter.ulButtonPressed)) {
            System.out.println("Trigger down.");
        }
        if (switchedUp(k_buttonTrigger, stateBefore.ulButtonPressed, stateAfter.ulButtonPressed)) {
            System.out.println("Trigger up.");
        }
    }
}
