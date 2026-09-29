#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec3 Tint;
uniform float Amount;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec4 scene = texture(Sampler0, texCoord);
    float keep = clamp(texture(Sampler1, texCoord).a, 0.0, 1.0);
    float luminance = dot(scene.rgb, vec3(0.2126, 0.7152, 0.0722));
    // Black is exactly RGB(0,0,0), including emissive blocks, sun, fog and clouds.
    // Other colors keep some light/shade so the world's shape remains readable.
    vec3 converted = Tint * clamp(luminance * 1.10 + 0.12, 0.12, 1.0);
    fragColor = vec4(mix(scene.rgb, converted, clamp(Amount,0.0,1.0)*(1.0-keep)), scene.a);
}
