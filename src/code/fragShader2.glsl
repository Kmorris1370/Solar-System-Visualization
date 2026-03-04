#version 430

//Fragment Shader 2

//Input 
in vec3 varyingNormal;      //Surface normal
in vec3 varyingLightDir;    //Direction to light source
in vec3 varyingVertPos;     //Vertex position 
in vec3 varyingHalfVector;  //Halfway vector for specular
in vec4 shadowCoord;        //Position in light's clip space
in vec2 tc;                 //Texture coordinates

//Output
out vec4 fragColor;         //Final pixel color (RGBA)

//Uniforms
uniform vec4 color;               
uniform sampler2D samp;             
uniform sampler2DShadow shadowTex;  
uniform int useTexture;             
uniform int useLighting;            
uniform int useShadows;             

void main(void)
{
    //Texture Sampling
    vec4 texColor;
    if (useTexture == 1) {
        texColor = texture(samp, tc) * color;
    } else {
        texColor = color;
    }
    
    //Unlit objectss
    if (useLighting == 0) {
        fragColor = texColor;
        return;
    }
    
    //Normalize Vectore
    vec3 L = normalize(varyingLightDir);  
    vec3 N = normalize(varyingNormal);     
    vec3 H = normalize(varyingHalfVector); 
    
    //Lighting
    float cosTheta = dot(L, N);  // Diffuse factor 
    float cosPhi = dot(H, N);    // Specular factor 
    
    //Lighting Compnents
    vec3 ambient = vec3(0.2, 0.2, 0.2);   
    vec3 diffuse = vec3(0.0);
    vec3 specular = vec3(0.0);
    
    //Shadow Calculations
    float shadowFactor = 1.0;  // 1.0 = fully lit, 0.0 = fully shadowed
    
    if (useShadows == 1) {
        //Perspective divide
        vec3 projCoords = shadowCoord.xyz / shadowCoord.w;
        
        //Transform from NDC [-1,1] to texture space [0,1]
        projCoords = projCoords * 0.5 + 0.5;
        
        if (projCoords.x >= 0.0 && projCoords.x <= 1.0 && 
            projCoords.y >= 0.0 && projCoords.y <= 1.0 && 
            projCoords.z <= 1.0) {
            
            float bias = max(0.0005 * (1.0 - cosTheta), 0.0002);
            
            float shadow = 0.0;
            vec2 texelSize = 1.0 / textureSize(shadowTex, 0);
            
            for(int x = -1; x <= 1; ++x) {
                for(int y = -1; y <= 1; ++y) {
                    shadow += texture(shadowTex, vec3(
                        projCoords.xy + vec2(x, y) * texelSize,
                        projCoords.z - bias
                    ));
                }
            }
            shadowFactor = shadow / 9.0;  // Average of 9 samples
        }
    }
    
    //Diffuse and Specular calculation
    if (cosTheta > 0.0) {
        //Diffuse
        diffuse = vec3(0.7, 0.7, 0.7) * cosTheta;
        
        //Specular
        if (cosPhi > 0.0) {
            // Shininess = 32, intensity = 0.3
            specular = vec3(1.0, 1.0, 1.0) * pow(cosPhi, 32.0) * 0.3;
        }
    }
    
    //Color Composition
    vec3 lighting = ambient + (diffuse + specular) * shadowFactor;
    fragColor = vec4(texColor.rgb * lighting, texColor.a);
}