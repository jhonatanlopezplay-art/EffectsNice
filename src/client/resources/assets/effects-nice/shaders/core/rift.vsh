#version 150
in vec3 Position;
in vec2 UV0;
uniform mat4 ViewRotation;
uniform mat4 ClipProjection;
out vec2 texCoord;
void main() {
    texCoord = UV0;
    gl_Position = ClipProjection * ViewRotation * vec4(Position, 1.0);
    gl_Position.z = gl_Position.w * 0.99998;
}
