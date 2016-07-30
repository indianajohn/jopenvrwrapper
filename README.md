This class is a wrapper around jopenvr, which in turn wraps the binary libraries distributed with the OpenVR C API. jopenvr can be found as part of the Vivecraft project: 

https://github.com/jrbudda/Vivecraft_110/tree/

When looking for a way to get an OpenGL project of mine working with the Vive,
I looked around for a suitable OpenVR wrapper and could find none at the time. Vivecraft was the closest I could find, but it was not designed to be encapsulated and removed for use in different projects. This wrapper is meant to allow my project and maybe others work easily without adding a lot of application-specific adaptation.
