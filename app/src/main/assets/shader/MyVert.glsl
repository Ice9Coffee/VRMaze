#version 300 es
precision highp float;

in vec4 a_Position;
in vec2 a_UV;
in vec3 a_Normal;
in mat4 a_Model;

uniform mat4 u_View;
uniform mat4 u_Projection;

out vec4 v_Position;
out vec3 v_Normal;
out vec2 v_UV;

void main() {
    v_UV = a_UV;

    mat4 ModelView = u_View * a_Model;
    v_Position = ModelView * a_Position;
    v_Normal = mat3(transpose(inverse(ModelView))) * a_Normal;

    gl_Position = u_Projection * v_Position;
}
