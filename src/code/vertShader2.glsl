#version 430

//Vertex Shader 2 

//Vertex attributes
layout (location = 0) in vec3 position;   
layout (location = 1) in vec2 tex_coord;  
layout (location = 2) in vec3 normal;     

//Output to fragment shader
out vec3 varyingNormal;      //Transformed normal 
out vec3 varyingLightDir;    //Direction from vertex to light
out vec3 varyingVertPos;     //Vertex position 
out vec3 varyingHalfVector;  //Halfway vector for specular 
out vec4 shadowCoord;        //Vertex position in light's clip space
out vec2 tc;                 //Texture coordinates (pass-through)

//Transformation matrices
uniform mat4 mv_matrix;    
uniform mat4 p_matrix;     
uniform mat4 norm_matrix;  
uniform mat4 shadowMVP;    

//Light Position
uniform vec3 lightPos;   

void main(void)
{
    //Vertex to view space 
    vec4 worldPos = mv_matrix * vec4(position, 1.0);
    varyingVertPos = worldPos.xyz;
    
    //Diffuse lighting calculation
    varyingLightDir = lightPos - varyingVertPos;
    
    //Transform normal vector using normal matrix
    varyingNormal = (norm_matrix * vec4(normal, 1.0)).xyz;
    
    //Calculate halfway vector
    vec3 viewDir = -normalize(varyingVertPos);  // View direction (toward camera)
    varyingHalfVector = normalize(normalize(varyingLightDir) + viewDir);
    
    //Calculate shadow coordinate for shadow mapping
    shadowCoord = shadowMVP * vec4(position, 1.0);
    
    //Final vertex position in clip space
    gl_Position = p_matrix * worldPos;
    
    //Pass texture coordinates to fragment shader
    tc = tex_coord;
}