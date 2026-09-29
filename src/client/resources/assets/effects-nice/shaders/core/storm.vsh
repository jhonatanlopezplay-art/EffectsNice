#version 150
in vec3 Position;
uniform mat4 ViewRotation;
uniform mat4 ClipProjection;
out vec3 skyDirection;
void main() {
    skyDirection = Position;
    gl_Position = ClipProjection * ViewRotation * vec4(Position, 1.0);
    gl_Position.z = gl_Position.w * 0.99999;
}
