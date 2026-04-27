#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 resolution;
uniform float time;
uniform float amount;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec2 texCoord;

out vec4 fragColor;

float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

mat4 transposeMat(mat4 m) {
    return mat4(
            m[0][0], m[1][0], m[2][0], m[3][0],
            m[0][1], m[1][1], m[2][1], m[3][1],
            m[0][2], m[1][2], m[2][2], m[3][2],
            m[0][3], m[1][3], m[2][3], m[3][3]
    );
}

vec3 getCameraPosition() {
    return - (ModelViewMat[3].xyz) * mat3(transpose(ModelViewMat));
}

vec3 snowLayer(vec2 uv, float scale, float speed, float density, float layer) {
    vec4 clipPos = vec4(texCoord * 2.0 - 1.0, 1.0, 1.0);
    vec4 viewPos = inverse(ProjMat) * clipPos;
    vec3 rayDir = normalize(viewPos.xyz / viewPos.w);

    vec3 camPos = getCameraPosition();

    float t = abs(layer / rayDir.z);

    if (t < 0.0) {
        return vec3(0.0);
    }

    vec3 pos = camPos + rayDir * t;
    vec2 layerUV = pos.xz * scale;
    layerUV.y -= time * speed;

    vec2 gv = fract(layerUV) - 0.5;
    vec2 id = floor(layerUV);

    float n = rand(id);
    vec2 flakePos = vec2((rand(id + 0.1) - 0.5) * 0.8, (rand(id + 0.2) - 0.5) * 0.4);
    float size = density + rand(id + 0.3) * 0.02;
    float d = length(gv - flakePos);
    float flake = smoothstep(size, size * 0.5, d) * (0.6 + 0.4 * sin(time * 3.0 + n * 6.28));
    return vec3(flake);
}

//vec3 snowLayer(vec2 uv, float scale, float speed, float density, float layer) {
//    vec2 layerUV = uv * scale;
//    layerUV.y -= time * speed; // Animate downward
//
//    vec2 gv = fract(layerUV) - 0.5;
//    vec2 id = floor(layerUV);
//
//    float n = rand(id);
//    vec2 flakePos = vec2((rand(id + 0.1) - 0.5) * 0.8, (rand(id + 0.2) - 0.5) * 0.4);
//    float size = density + rand(id + 0.3) * 0.02;
//    float d = length(gv - flakePos);
//    float flake = smoothstep(size, size * 0.5, d) * (0.6 + 0.4 * sin(time * 3.0 + n * 6.28));
//    return vec3(flake);
//}

void main() {
    vec4 sceneColor = texture(DiffuseSampler, texCoord);
    vec2 uv = texCoord/* * resolution / max(resolution.x, resolution.y)*/;

    vec3 snowAccum = vec3(0.0);

    snowAccum += snowLayer(uv, 20.0, 0.8, 0.02, 1.0) * 0.8;
    snowAccum += snowLayer(uv, 15.0, 1.2, 0.015, 5.0) * 0.6;
    snowAccum += snowLayer(uv, 10.0, 1.5, 0.01, 10.0) * 0.4;
    snowAccum += snowLayer(uv, 5.0, 2.0, 0.005, 20.0) * 0.2;

    float snowIntensity = clamp(snowAccum.r * amount, 0.0, 1.0);

    fragColor = vec4(vec3(snowIntensity), sceneColor.a);

//    vec3 snowColor = vec3(1.0);
//    vec3 finalColor = mix(sceneColor.rgb, snowColor, snowIntensity);
//    fragColor = vec4(finalColor, sceneColor.a);
}