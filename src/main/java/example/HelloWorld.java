package example;

import openvrprovider.OpenVRProvider;
import openvrprovider.OpenVRStereoRenderer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Sys;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

/* Just an example demonstrating how everything works. */
public class HelloWorld {

    // We need to strongly reference callback instances.
    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback   keyCallback;

    // The window handle
    private long window;
    private OpenVRProvider vrProvider;
    private OpenVRStereoRenderer vrRenderer;

    public void setVRProvider(OpenVRProvider _vrProvider) { vrProvider = _vrProvider;}

    public void run() {
        System.out.println("Hello LWJGL " + Sys.getVersion() + "!");
        try {
            init();
            loop();

            // Release window and window callbacks
            glfwDestroyWindow(window);
            keyCallback.release();
        } finally {
            // Terminate GLFW and release the GLFWerrorfun
            glfwTerminate();
            errorCallback.release();
        }
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        glfwSetErrorCallback(errorCallback = errorCallbackPrint(System.err));

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( glfwInit() != GL11.GL_TRUE )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure our window
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE); // the window will be resizable
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        int WIDTH = 1280;
        int HEIGHT = 720;

        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                    glfwSetWindowShouldClose(window, GL_TRUE); // We will detect this in our rendering loop
            }
        });

        // Get the resolution of the primary monitor
        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        // Center our window
        glfwSetWindowPos(
                window,
                (GLFWvidmode.width(vidmode) - WIDTH) / 2,
                (GLFWvidmode.height(vidmode) - HEIGHT) / 2
        );

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Disable v-sync
        glfwSwapInterval(0);

        // Make the window visible
        glfwShowWindow(window);
    }

    public double getTime() {
        return System.nanoTime() / 1000000000;
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the ContextCapabilities instance and makes the OpenGL
        // bindings available for use.
        GLContext.createFromCurrent();

        // compile and link vertex and fragment shaders into
        // a "program" that resides in the OpenGL driver
        ShaderProgram shader = new ShaderProgram();

        // do the heavy lifting of loading, compiling and linking
        // the two shaders into a usable shader program
        String userDir = System.getProperty("user.dir");


        int vaoHandle = constructVertexArrayObject();

        // Need to bind VAO before linking shader
        GL30.glBindVertexArray(vaoHandle);
        GL20.glEnableVertexAttribArray(0); // VertexPosition
        GL20.glEnableVertexAttribArray(1); // VertexColor
        shader.init(userDir + "/shaders/HelloWorld.glslv",userDir + "/shaders/HelloWorld.glslf");

        vrRenderer = new OpenVRStereoRenderer(vrProvider,1280,720);

        long nFrames = 0;
        double fTime = getTime();

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( glfwWindowShouldClose(window) == GL_FALSE ) {
            nFrames++;
            double fps = nFrames / (getTime() - fTime);
            if (nFrames % 1000 == 0)
                System.out.println("FPS: " + fps);
            for (int nEye = 0; nEye < 2; nEye++)
            {
                EXTFramebufferObject.glBindFramebufferEXT(GL_FRAMEBUFFER_EXT,vrRenderer.getTextureHandleForEyeFramebuffer(nEye));

                // tell OpenGL to use the shader
                GL20.glUseProgram(shader.getProgramId());

                glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

                Matrix4f matMVP = vrProvider.vrState.getEyeProjectionMatrix(nEye).mul(vrProvider.vrState.getEyePose(nEye));
                shader.setUniformMatrix("MVP",false,matMVP);

                // bind vertex and color data
                GL30.glBindVertexArray(vaoHandle);
                GL20.glEnableVertexAttribArray(0); // VertexPosition
                GL20.glEnableVertexAttribArray(1); // VertexColor

                // draw VAO
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
                vrProvider.submitFrame();


                glfwSwapBuffers(window); // swap the color buffers
                // Poll for window events. The key callback above will only be
                // invoked during this call.
                glfwPollEvents();
                glFinish();
            }
        }
    }

    private int constructVertexArrayObject()
    {
        // create vertex data
        float[] positionData = new float[] {
                1f,		-1f,		0f,
                -1f,	0f, 	0f,
                0f,		1f,		0f
        };

        // create color data
        float[] colorData = new float[]{
                0f,			0f,			1f,
                1f,			0f,			0f,
                0f,			1f,			0f
        };

        // convert vertex array to buffer
        FloatBuffer positionBuffer = BufferUtils.createFloatBuffer(positionData.length);
        positionBuffer.put(positionData);
        positionBuffer.flip();

        // convert color array to buffer
        FloatBuffer colorBuffer = BufferUtils.createFloatBuffer(colorData.length);
        colorBuffer.put(colorData);
        colorBuffer.flip();

        // create vertex byffer object (VBO) for vertices
        int positionBufferHandle = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBufferHandle);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positionBuffer, GL15.GL_STATIC_DRAW);

        // create VBO for color values
        int colorBufferHandle = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorBufferHandle);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colorBuffer, GL15.GL_STATIC_DRAW);

        // unbind VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // create vertex array object (VAO)
        int vaoHandle = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoHandle);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);

        // assign vertex VBO to slot 0 of VAO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionBufferHandle);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);

        // assign vertex VBO to slot 1 of VAO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorBufferHandle);
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);

        // unbind VBO
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        return vaoHandle;
    }

    public static void main(String[] args) {
        OpenVRProvider provider = new OpenVRProvider();
        try {
            provider.vrState.addControllerListener(new SampleControllerListener());
            Thread vrPoller = new Thread(provider, "vrPoller");
            vrPoller.start();
            SharedLibraryLoader.load();
            HelloWorld app = new HelloWorld();
            app.setVRProvider(provider);
            app.run();
            vrPoller.join();
        } catch (Exception e) {
            System.out.println("Unhandled exception: " + e.toString());
        }
        System.out.println("Exited normally.");
    }
}