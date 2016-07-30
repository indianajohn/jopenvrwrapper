package vrprovider;

import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import jopenvr.*;
import jopenvr.JOpenVRLibrary.EVREventType;

import java.nio.IntBuffer;

import static vrprovider.ControllerListener.LEFT_CONTROLLER;
import static vrprovider.ControllerListener.RIGHT_CONTROLLER;

public class VRProvider implements Runnable {
    private static boolean initialized = false;
    private static VR_IVRSystem_FnTable vrsystem;
    private static VR_IVRCompositor_FnTable vrCompositor;
    private static VR_IVROverlay_FnTable vrOverlay;
    private static VR_IVRSettings_FnTable vrSettings;
    public VRState vrState = new VRState();
    private static int[] controllerDeviceIndex = new int[2];
    private static VRControllerState_t.ByReference[] inputStateRefernceArray = new VRControllerState_t.ByReference[2];
    private static VRControllerState_t[] controllerStateReference = new VRControllerState_t[2];
    private static IntBuffer hmdErrorStore;
    private static TrackedDevicePose_t.ByReference hmdTrackedDevicePoseReference;
    private static TrackedDevicePose_t[] hmdTrackedDevicePoses;
    // TextureIDs of framebuffers for each eye
    private final static VRTextureBounds_t texBounds = new VRTextureBounds_t();
    private final static Texture_t texType0 = new Texture_t();
    private final static Texture_t texType1 = new Texture_t();
    private static boolean[] controllerTracking = new boolean[2];
    //keyboard
    private static boolean keyboardShowing = false;
    private static boolean headIsTracking = false;

    private boolean init() {
        try {
            if (!initializeOpenVRLibrary())
                return false;
            initializeJOpenVR();
            initOpenVRCompositor(true);
            initOpenVROverlay();
            initOpenVROSettings();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void shutdown() {
        initialized = false;
    }

    @Override
    public void run() {
        initialized = init();
        while (initialized) {
            OpenVRUtil.sleepNanos(10000);
            updatePose();
            pollControllers();
            pollInputEvents();
        }
    }

    private void pollControllers() {
        for (int c = 0; c < 2; c++) {
            if (controllerDeviceIndex[c] != -1) {
                vrsystem.GetControllerState.apply(controllerDeviceIndex[c], inputStateRefernceArray[c]);
                inputStateRefernceArray[c].read();
                controllerStateReference[c] = inputStateRefernceArray[c];
                vrState.updateControllerButtonState(controllerStateReference);
            }
        }
    }

    private boolean initializeOpenVRLibrary() throws Exception {
        if (initialized)
            return true;
        System.out.println("Adding OpenVR search path: " + OSValidator.getLibPath());
        NativeLibrary.addSearchPath("openvr_api", OSValidator.getLibPath());

        if (jopenvr.JOpenVRLibrary.VR_IsHmdPresent() == 1) {
            System.out.println("VR Headset detected.");
        } else {
            System.out.println("VR Headset not detected.");
            return false;
        }
        return true;
    }

    public VRProvider() {
        for (int c = 0; c < 2; c++) {
            controllerDeviceIndex[c] = -1;
            controllerStateReference[c] = new VRControllerState_t();
            inputStateRefernceArray[c] = new VRControllerState_t.ByReference();
            inputStateRefernceArray[c].setAutoRead(false);
            inputStateRefernceArray[c].setAutoWrite(false);
            inputStateRefernceArray[c].setAutoSynch(false);
        }
    }

    private static void initializeJOpenVR() throws Exception {
        hmdErrorStore = IntBuffer.allocate(1);
        vrsystem = null;
        JOpenVRLibrary.VR_InitInternal(hmdErrorStore, JOpenVRLibrary.EVRApplicationType.EVRApplicationType_VRApplication_Scene);
        if (hmdErrorStore.get(0) == 0) {
            // ok, try and get the vrsystem pointer..
            vrsystem = new VR_IVRSystem_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSystem_Version, hmdErrorStore));
        }
        if (vrsystem == null || hmdErrorStore.get(0) != 0) {
            throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));
        } else {

            vrsystem.setAutoSynch(false);
            vrsystem.read();

            System.out.println("OpenVR initialized & VR connected.");

            hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
            hmdTrackedDevicePoses = (TrackedDevicePose_t[]) hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);

            // disable all this stuff which kills performance
            hmdTrackedDevicePoseReference.setAutoRead(false);
            hmdTrackedDevicePoseReference.setAutoWrite(false);
            hmdTrackedDevicePoseReference.setAutoSynch(false);
            for (int i = 0; i < JOpenVRLibrary.k_unMaxTrackedDeviceCount; i++) {
                hmdTrackedDevicePoses[i].setAutoRead(false);
                hmdTrackedDevicePoses[i].setAutoWrite(false);
                hmdTrackedDevicePoses[i].setAutoSynch(false);
            }
        }
    }

    // needed for in-game keyboard
    private static void initOpenVROverlay() throws Exception {
        vrOverlay = new VR_IVROverlay_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVROverlay_Version, hmdErrorStore));
        if (hmdErrorStore.get(0) == 0) {
            vrOverlay.setAutoSynch(false);
            vrOverlay.read();
            System.out.println("OpenVR Overlay initialized OK");
        } else {
            throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));
        }
    }

    private static void initOpenVROSettings() throws Exception {
        vrSettings = new VR_IVRSettings_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSettings_Version, hmdErrorStore));
        if (hmdErrorStore.get(0) == 0) {
            vrSettings.setAutoSynch(false);
            vrSettings.read();
            System.out.println("OpenVR Settings initialized OK");
        } else {
            throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));
        }
    }

    private static void initOpenVRCompositor(boolean set) throws Exception {
        if (set && vrsystem != null) {
            vrCompositor = new VR_IVRCompositor_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStore));
            if (hmdErrorStore.get(0) == 0) {
                System.out.println("OpenVR Compositor initialized OK.");
                vrCompositor.setAutoSynch(false);
                vrCompositor.read();
                vrCompositor.SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseStanding);
            } else {
                throw new Exception(jopenvr.JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));
            }
        }
        if (vrCompositor == null) {
            System.out.println("Skipping VR Compositor...");
        }

        // left eye
        texBounds.uMax = 1f;
        texBounds.uMin = 0f;
        texBounds.vMax = 1f;
        texBounds.vMin = 0f;
        texBounds.setAutoSynch(false);
        texBounds.setAutoRead(false);
        texBounds.setAutoWrite(false);
        texBounds.write();
        // texture type
        texType0.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
        texType0.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
        texType0.setAutoSynch(false);
        texType0.setAutoRead(false);
        texType0.setAutoWrite(false);
        texType0.handle = -1;
        texType0.write();
        // texture type
        texType1.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
        texType1.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
        texType1.setAutoSynch(false);
        texType1.setAutoRead(false);
        texType1.setAutoWrite(false);
        texType1.handle = -1;
        texType1.write();
        System.out.println("OpenVR Compositor initialized OK.");

    }

    public static boolean setKeyboardOverlayShowing(boolean showingState) {
        int ret = 1;
        if (showingState) {
            Pointer pointer = new Memory(3);
            pointer.setString(0, "mc");
            Pointer empty = new Memory(1);
            empty.setString(0, "");

            ret = vrOverlay.ShowKeyboard.apply(0, 0, pointer, 256, empty, (byte) 1, 0);

            keyboardShowing = 0 == ret; //0 = no error, > 0 see EVROverlayError


            if (ret != 0) {
                System.out.println("VR Overlay Error: " + vrOverlay.GetOverlayErrorNameFromEnum.apply(ret).getString(0));
            }

        } else {
            try {
                vrOverlay.HideKeyboard.apply();
            } catch (Error e) {
            }
            keyboardShowing = false;
        }

        return keyboardShowing;
    }

    public void destroy() {
        if (this.initialized) {
            JOpenVRLibrary.VR_ShutdownInternal();
            this.initialized = false;
        }
    }

    private static void findControllerDevices() {
        controllerDeviceIndex[RIGHT_CONTROLLER] = -1;
        controllerDeviceIndex[LEFT_CONTROLLER] = -1;

        controllerDeviceIndex[RIGHT_CONTROLLER] = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_LeftHand);
        controllerDeviceIndex[LEFT_CONTROLLER] = vrsystem.GetTrackedDeviceIndexForControllerRole.apply(JOpenVRLibrary.ETrackedControllerRole.ETrackedControllerRole_TrackedControllerRole_RightHand);
    }

    //jrbuda:: oh hello there you are.
    private static void pollInputEvents() {
        if (vrsystem == null) return;

        jopenvr.VREvent_t event = new jopenvr.VREvent_t();

        while (vrsystem.PollNextEvent.apply(event, event.size()) > 0) {

            switch (event.eventType) {
                case EVREventType.EVREventType_VREvent_KeyboardClosed:
                    //'huzzah'
                    keyboardShowing = false;
                    break;
                case EVREventType.EVREventType_VREvent_KeyboardCharInput:
                    byte[] inbytes = event.data.getPointer().getByteArray(0, 8);
                    int len = 0;
                    for (byte b : inbytes) {
                        if (b > 0) len++;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void updatePose() {
        if (vrsystem == null || vrCompositor == null)
            return;

        vrCompositor.WaitGetPoses.apply(hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount, null, 0);
        for (int nDevice = 0; nDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; ++nDevice) {
            hmdTrackedDevicePoses[nDevice].read();
        }

        if (hmdTrackedDevicePoses[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd].bPoseIsValid != 0) {
            HmdMatrix34_t matL = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left);
            vrState.setLeftEyePoseWRTHead(matL);
            HmdMatrix34_t matR = vrsystem.GetEyeToHeadTransform.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right);
            vrState.setRightEyePoseWRTHead(matR);
            vrState.setHeadPose(hmdTrackedDevicePoses[JOpenVRLibrary.k_unTrackedDeviceIndex_Hmd].mDeviceToAbsoluteTracking);
            headIsTracking = true;
        } else {
            headIsTracking = false;
        }

        findControllerDevices();

        for (int c = 0; c < 2; c++) {
            if (controllerDeviceIndex[c] != -1) {
                controllerTracking[c] = true;
                vrState.setControllerPose(hmdTrackedDevicePoses[controllerDeviceIndex[c]].mDeviceToAbsoluteTracking, c);
            } else {
                controllerTracking[c] = false;
            }
        }
    }

    static void triggerHapticPulse(int controller, int strength) {
        if (controllerDeviceIndex[controller] == -1)
            return;
        vrsystem.TriggerHapticPulse.apply(controllerDeviceIndex[controller], 0, (short) strength);
    }

}
