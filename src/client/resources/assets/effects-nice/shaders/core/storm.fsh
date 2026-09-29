#version 150
uniform float Time;
uniform float Progress;
uniform vec3 Origin;
in vec3 skyDirection;
out vec4 fragColor;
float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.11, 0.37, 0.71));
    p *= 17.0; return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}
float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p); f = f*f*(3.0-2.0*f);
    return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),
               mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float fbm(vec3 p) {
    float result = 0.0, amplitude = 0.53;
    for (int i=0; i<5; i++) { result += amplitude*noise(p); p = p*2.03+vec3(7.2,3.8,1.7); amplitude *= 0.5; }
    return result;
}
void main() {
    vec3 d = normalize(skyDirection);
    float t = Time * 0.022;
    vec3 p = d*3.7 + vec3(t, -t*0.4, t*0.7);
    float warp = fbm(p*0.85 + vec3(0,t,0));
    float clouds = fbm(p + vec3(warp*2.4, warp*1.1, -warp*1.7));
    float wisps = fbm(p*2.1 + clouds*2.0);
    float density = smoothstep(0.27,0.72,clouds*0.82+wisps*0.18);
    vec3 red = vec3(0.40, 0.004, 0.057);
    vec3 dark = vec3(0.016, 0.001, 0.017);
    vec3 color = mix(red, dark, smoothstep(0.20,0.80,density));
    color += vec3(0.105,0.001,0.012)*pow(1.0-density,3.0);
    // Spherical expanding front with soft, turbulent edges: no screen-space overlay.
    float angle = acos(clamp(dot(d, normalize(Origin)), -1.0, 1.0));
    float radius = Progress*3.85 - 0.30;
    float feather = 0.22;
    float mask = 1.0-smoothstep(radius-feather,radius+feather,angle+(clouds-0.5)*0.22);
    mask *= smoothstep(0.0,0.06,Progress);
    mask *= smoothstep(-0.18,0.035,d.y);
    fragColor = vec4(color,mask);
}
