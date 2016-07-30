package openvrprovider;

import jopenvr.JOpenVRLibrary;
import org.lwjgl.opengl.GL11;

/* This class is designed to manage the framebuffers for the headset. */
public class OpenVRStereoRenderer {
    public OpenVRProvider vrProvider;

    // TextureIDs of framebuffers for each eye
    private int eyeTextureIDs[] = new int[2];

	OpenVRStereoRenderer( OpenVRProvider _vrProvider, int lwidth, int lheight) {
		vrProvider = _vrProvider;
		createRenderTexture(lwidth,lheight);
	}

    public void deleteRenderTextures() {
        if (eyeTextureIDs[0] > 0) GL11.glDeleteTextures(eyeTextureIDs[0]);
    }

	public void createRenderTexture(int lwidth, int lheight)
	{
		for (int nEye = 0; nEye < 2; nEye++) {
			eyeTextureIDs[nEye] = GL11.glGenTextures();
			int boundTextureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, eyeTextureIDs[nEye]);
			GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, lwidth, lheight, 0, GL11.GL_RGBA, GL11.GL_INT, (java.nio.ByteBuffer) null);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTextureId);
			vrProvider.texType[nEye].handle = eyeTextureIDs[nEye];
			vrProvider.texType[nEye].eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
			vrProvider.texType[nEye].eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
			vrProvider.texType[nEye].write();
		}
	}

	public int getTextureHandleForEyeFramebuffer(int nEye) {
		return eyeTextureIDs[nEye];
	}

}
