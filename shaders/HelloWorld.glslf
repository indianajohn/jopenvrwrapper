#version 330 core

// corresponds with output from vertex shader
in vec3 Color;

out vec4 FragColor;

void main()
{
    // assign vertex color to pixel color
    FragColor = vec4(Color, 1.0);
}
