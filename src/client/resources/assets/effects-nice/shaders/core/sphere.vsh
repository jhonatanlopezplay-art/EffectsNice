#version 150
in vec3 Position;
in vec2 UV0;
uniform mat4 ViewRotation;
uniform mat4 ClipProjection;
uniform vec3 Offset;
out vec3 localPosition;
out float edgeDistance;
out float panelSeed;
void main() {
    localPosition = Position;
    edgeDistance = UV0.x;
    panelSeed = UV0.y;
    gl_Position = ClipProjection * ViewRotation * vec4(Position + Offset, 1.0);
}
