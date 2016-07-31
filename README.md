JOpenVRWrapper
==============

This package is a wrapper around jopenvr, which in turn wraps the binary 
libraries distributed with the OpenVR C API. jopenvr can be found as part 
of the Vivecraft project: 

https://github.com/jrbudda/Vivecraft_110/tree/

When looking for a way to get an OpenGL project of mine working with the Vive, 
I looked around for a suitable OpenVR wrapper and could find none at the time. 
Vivecraft was the closest I could find, but it was not designed to be 
encapsulated and removed for use in different projects. This wrapper 
is meant to allow my project and maybe others work easily without adding 
a lot of application-specific adaptation.

The package is designed to insulate the user from making calls to OpenVR
directly to avoid the need to deal with native data structures or debug the
underlying C API calls. The basic usage is:

1. Instantiate OpenVRPRovider.

2. Register any classes that you would like as ControllerListener implementers.

3. Instantiate OpenVRStereoRenderer in your render thread after creating your
   OpenGL context.

4. Every iteration of your render thread, bind the left eye frame buffer,
  retrieve the projection matrix and pose matrix for the left eye, pass them to
  your shaders, render, call glFinish() and any other frame clean up code, and
  then do the same for the right eye.

This process is demonstrated in examples/HelloWorld.java.

A build.gradle file is included as well as a pre-made gradle wrapper; on any
machine with the JDK on it, running this should be as simple as running

```
./gradlew run
```

Or, for IDE support:

```
./gradlew idea
```
