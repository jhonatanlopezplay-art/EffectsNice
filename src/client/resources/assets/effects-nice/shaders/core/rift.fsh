#version 150
uniform sampler2D Sampler0;
uniform float Time;
uniform float Fracture;
uniform float Opening;
uniform float ImageAspect;
uniform float RiftAspect;
in vec2 texCoord;
out vec4 fragColor;

float hash(float x) { return fract(sin(x*127.1+311.7)*43758.5453); }

// Unevenly spaced fracture vertices give angular, stable chips rather than a wave.
float jag(float x, float scale, float seed) {
    float p = (x+1.0)*scale;
    float cell = floor(p);
    float knot = cell+0.18+hash(cell+seed+71.0)*0.64;
    if (p < knot) cell -= 1.0;
    float a = cell+0.18+hash(cell+seed+71.0)*0.64;
    float b = cell+1.18+hash(cell+seed+72.0)*0.64;
    return mix(hash(cell+seed),hash(cell+1.0+seed),(p-a)/(b-a))*2.0-1.0;
}
float seam(float x) {
    return jag(x,6.5,4.0)*0.095+jag(x,19.0,8.0)*0.021;
}
float envelope(float x) { return pow(max(0.0,1.0-abs(x)),0.78); }
vec2 edges(float x) {
    float shape = envelope(x);
    float center = seam(x)*(1.0-Opening*0.20);
    // Both lips share one closed seam, then separate into different broken edges.
    float upper = shape*max(0.25,0.79+jag(x,8.5,37.0)*0.13+jag(x,26.0,41.0)*0.026);
    float lower = shape*max(0.25,0.82+jag(x,10.0,93.0)*0.12+jag(x,29.0,53.0)*0.024);
    return vec2(center-upper*Opening,center+lower*Opening);
}

// Return distance and position along a segment, accounting for the rift's aspect.
vec2 segment(vec2 p, vec2 a, vec2 b) {
    vec2 metric=vec2(max(RiftAspect,0.01),1.0);
    vec2 pa=(p-a)*metric, ba=(b-a)*metric;
    float t=clamp(dot(pa,ba)/max(dot(ba,ba),0.000001),0.0,1.0);
    return vec2(length(pa-ba*t),t);
}
float taperedCrack(vec2 p, vec2 a, vec2 b, float startWidth, float endWidth, float aa) {
    vec2 hit=segment(p,a,b);
    float width=mix(startWidth,endWidth,hit.y);
    return 1.0-smoothstep(width,width+aa,hit.x);
}
void main() {
    vec2 p=(texCoord*2.0-1.0)*vec2(1.18,1.3);
    float x=p.x, ax=abs(x);
    vec2 lip=edges(x);
    float lowerField=lip.x-p.y, upperField=p.y-lip.y;
    float boundary=max(lowerField,upperField);
    float pixel=max(length(vec2(dFdx(boundary),dFdy(boundary))),0.0005);
    float verticalPixel=max(length(vec2(dFdx(p.y),dFdy(p.y))),0.0005);
    float aa=pixel*0.85;
    float distance=abs(boundary);
    float front=Fracture*1.055;
    float visible=(1.0-smoothstep(front-0.035,front,ax))*(1.0-smoothstep(0.985,1.02,ax));

    // About one third thinner than the initial rim, with local thickness variation.
    // Derivatives retain a readable, antialiased core when the crack is far away.
    float grain=hash(floor((x+1.0)*91.0)+3.0);
    float width=max(0.011+0.004*(jag(x,15.0,57.0)*0.5+0.5),pixel*0.8);
    float core=1.0-smoothstep(width,width+aa,distance);
    float hot=1.0-smoothstep(width*0.28,width*0.58+aa,distance);
    float glow=exp(-distance/max(width*2.7,pixel*2.5))*0.47
              +exp(-distance/max(width*6.0,pixel*4.0))*0.10;

    // Short secondary cracks stay attached to the moving lips. Irregular spacing,
    // tapered ends and a few forks resemble stress fractures instead of lightning.
    float branches=0.0, branchGlow=0.0;
    for (int i=0;i<10;i++) {
        float seed=float(i)*19.31;
        float bx=-0.82+(float(i)+hash(seed+2.0)*0.62)*0.164;
        float side=hash(seed+5.0)>0.48?1.0:-1.0;
        vec2 rootEdges=edges(bx);
        vec2 a=vec2(bx,side>0.0?rootEdges.y:rootEdges.x);
        float growth=smoothstep(abs(bx)+0.035,abs(bx)+0.17,Fracture);
        float lengthY=0.065+hash(seed+7.0)*0.15;
        float lean=(hash(seed+9.0)*2.0-1.0)*0.045;
        vec2 b=a+vec2(lean,lengthY*side*0.57)*growth;
        vec2 c=b+vec2(lean*0.40+(hash(seed+11.0)-0.5)*0.037,lengthY*side*0.43)*growth;
        float branchWidth=max(width*0.38,verticalPixel*0.45);
        float line=max(taperedCrack(p,a,b,branchWidth,branchWidth*0.62,verticalPixel*0.65),
                       taperedCrack(p,b,c,branchWidth*0.62,0.0,verticalPixel*0.65));
        float dist=min(segment(p,a,b).x,segment(p,b,c).x);
        if (i==2 || i==5 || i==8) {
            vec2 fork=b+vec2(-0.025-lean*0.5,lengthY*side*0.42)*growth;
            line=max(line,taperedCrack(p,b,fork,branchWidth*0.55,0.0,verticalPixel*0.65));
            dist=min(dist,segment(p,b,fork).x);
        }
        float strength=growth*(0.68+hash(seed+17.0)*0.24);
        branches=max(branches,line*strength);
        branchGlow=max(branchGlow,exp(-dist/max(branchWidth*3.5,verticalPixel))*0.26*strength);
    }

    float inside=(1.0-smoothstep(-aa,aa,boundary))*step(ax,0.99)*smoothstep(0.0,0.035,Opening);
    vec2 uv=vec2(x*0.5+0.5,p.y*0.5+0.5);
    float ratio=RiftAspect/max(ImageAspect,0.01);
    if (ratio>1.0) uv.y=(uv.y-0.5)/ratio+0.5; else uv.x=(uv.x-0.5)*ratio+0.5;
    vec4 image=texture(Sampler0,clamp(uv,0.0,1.0));
    vec3 interior=mix(vec3(0.005,0.002,0.012),image.rgb,image.a);

    // Dark inner bevel adds a hint of depth; the warm hot core never fills the halo.
    float bevel=(1.0-smoothstep(width*1.1,width*4.8,distance))*inside*(1.0-core);
    interior*=1.0-bevel*0.53;
    float pulse=0.97+0.03*sin(Time*2.1+x*7.0);
    vec3 neon=vec3(1.0,0.025,0.014);
    vec3 warm=mix(neon,vec3(1.0,0.27,0.065),core);
    vec3 rim=mix(warm,vec3(1.0,0.83,0.61),hot*(0.82+grain*0.12));
    float edgeAlpha=max(core,glow*pulse)*visible;
    float branchAlpha=max(branches,branchGlow)*visible*(1.0-inside);
    float alpha=max(inside*visible,max(edgeAlpha,branchAlpha));
    vec3 color=mix(interior,rim,clamp(edgeAlpha,0.0,1.0));
    if (inside<0.01) color=rim;
    color=mix(color,mix(neon,vec3(1.0,0.43,0.15),branches*0.65),branchAlpha*(1.0-core));
    fragColor=vec4(color,clamp(alpha,0.0,1.0));
}
