#version 300 es
precision highp float;

struct Material {
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};

struct Light {
    vec4 position;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

uniform mat4 u_View;

in vec4 v_Position;
in vec3 v_Normal;
in vec2 v_UV;
uniform sampler2D u_Texture;


uniform Material material;
uniform Light light;

out vec4 fragColor;


void main() {
    vec4 texColor = texture(u_Texture, vec2(v_UV.x, v_UV.y));

    // ambient
    vec3 ambient = light.ambient * material.ambient * texColor.xyz;


    // 在View下的光源位置
    vec4 lightPos = u_View * light.position;

    // diffuse
    vec3 norm = normalize(v_Normal);
    vec3 lightDir = normalize(lightPos.xyz - v_Position.xyz);
    float diff = max(dot(norm, lightDir), 0.0);
    //float diff = abs(dot(norm, lightDir));
    vec3 diffuse = light.diffuse * ( diff * material.diffuse * texColor.xyz);

    // specular
    vec3 viewDir = normalize(-v_Position.xyz);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), material.shininess);
    vec3 specular = light.specular * (spec * material.specular);

    fragColor = vec4(ambient + diffuse + specular, 1.0);
}
