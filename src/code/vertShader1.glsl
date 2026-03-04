#version 430

//Vertex Shader 1 

layout (location = 0) in vec3 position; 

uniform mat4 shadowMVP;  //Model-View-Projection matrix from light's perspective

void main(void)
{
    gl_Position = shadowMVP * vec4(position, 1.0);
}