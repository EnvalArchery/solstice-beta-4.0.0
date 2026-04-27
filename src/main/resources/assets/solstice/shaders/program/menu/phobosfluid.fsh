#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
in vec2 oneTexel;
uniform float alpha;
uniform float alpha2;
out vec4 fragColor;
uniform int quality;
uniform int overlay;
uniform vec2 resolution;
uniform float time;

uniform vec2 InSize;
uniform float alpha0;

vec3 hash33(vec3 p) {
    float n = sin(dot(p, vec3(7, 157, 113)));
    return fract(vec3(2097152, 262144, 32768)*n)*2. - 1.;
}

float tetraNoise(in vec3 p) {
    vec3 i = floor(p + dot(p, vec3(0.333333)));
    p -= i - dot(i, vec3(0.166666));
    vec3 st = step(p.yzx, p);
    vec3 i1 = st, i2 = max(st, 1.0-st.zxy);
    i1 = min(i1, 1.0-i1.zxy);
    vec3 p1 = p - i1 + 0.166666, p2 = p - i2 + 0.333333, p3 = p - 0.5;
    vec4 v = max(0.5 - vec4(dot(p,p), dot(p1,p1), dot(p2,p2), dot(p3,p3)), 0.0);
    vec4 d = vec4(dot(p, hash33(i)), dot(p1, hash33(i + i1)), dot(p2, hash33(i + i2)), dot(p3, hash33(i + 1.)));
    return clamp(dot(d, v*v*v*8.)*1.732 + .5, 0., 1.); // Not sure if clamping is necessary. Might be overkill.
}

float sTri(vec2 p, float radius) {
    radius /= 2.;
    vec2 a = normalize(vec2(1.6,1.));
    return max(
        dot(p, vec2(0,-1)) - radius,
        max(
            dot(p, a) - radius,
            dot(p, a * vec2(-1,1)) - radius
        )
    );
}

vec2 smoothRepeatStart(float x, float size) {
    return vec2(
        mod(x - size / 2., size),
        mod(x, size)
    );
}

float smoothRepeatEnd(float a, float b, float x, float size) {
    return mix(a, b,
               smoothstep(
                   0., 1.,
                   sin((x / size) * 3.14159265359 * 2. - 3.14159265359 * .5) * .5 + .5
               )
    );
}

vec3 createColor() {
    vec2 uv = (-resolution.xy + 2. * texCoord.xy) / resolution.y;
    uv /= 0.0005;
    uv *= 1.8;
    float repeatSize = 4.;
    float x = uv.x - mod(time, repeatSize / 2.);
    float y = uv.y;
    vec2 ab = vec2(1., 1.);
    float noise = 0.;
    float noiseA = 0.;
    float noiseB = 0.;
    ab = smoothRepeatStart(x, repeatSize);
    noiseA = tetraNoise(16. + vec3(vec2(ab.x, uv.y) * 1.2, 0)) * .5;
    noiseB = tetraNoise(16. + vec3(vec2(ab.y, uv.y) * 1.2, 0)) * .5;
    noise = smoothRepeatEnd(noiseA, noiseB, x, repeatSize);
    ab = smoothRepeatStart(y, repeatSize / 2.);
    noiseA = tetraNoise(vec3(vec2(uv.x, ab.x) * .5, 0)) * 2.;
    noiseB = tetraNoise(vec3(vec2(uv.x, ab.y) * .5, 0)) * 2.;
    noise *= smoothRepeatEnd(noiseA, noiseB, y, repeatSize / 2.);
    ab = smoothRepeatStart(x, repeatSize);
    noiseA = tetraNoise(9. + vec3(vec2(ab.x, uv.y) * .05, 0)) * 5.;
    noiseB = tetraNoise(9. + vec3(vec2(ab.y, uv.y) * .05, 0)) * 5.;
    noise *= smoothRepeatEnd(noiseA, noiseB, x, repeatSize);
    noise *= .75;
    noise = mix(noise, dot(uv, vec2(-.66, 1.) * .4), .6);
    float spacing = 1. / 20.;
    float lines = mod(noise, spacing) / spacing;
    lines = min(lines * 2., 1.) - max(lines * 2. - 1., 0.);
    lines /= fwidth(noise / spacing);
    float d = sTri(uv + vec2(0, .1), .3);
    lines = 1. - lines;
    return vec3(lines);
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);
    vec3 col0 = createColor();

    if (centerCol.a != 0.0) {
        if (overlay == 2) {
            fragColor = vec4(col0, 0.0);
            return;
        }

        fragColor = vec4(col0, alpha);
        return;
    }

    float alphaOutline = 0;
    bool hasOutline = false;

    for (int x = -quality; x <= quality; x++) {
        for (int y = -quality; y <= quality; y++) {
            vec2 offset = vec2(x, y);
            vec2 coord = texCoord + offset * oneTexel;
            vec4 t = texture(DiffuseSampler, coord);
            if (t.a != 0) {
                if (overlay < 1 && alpha0 == -1.0) {
                    float dst = length(offset);

                    if (dst <= 1.5) {
                        hasOutline = true;
                    }

                    float falloff = exp((-dst * 100.0) / 500.0);
                    alphaOutline += max(0.0, falloff) * 0.01 * alpha2;
                    continue;
                }

                fragColor = vec4(col0, alpha2);
                return;
            }
        }
    }

    if (hasOutline && (overlay == 1 || overlay == 2)) {
        fragColor = vec4(col0, 1.0); // overlay
    } else if (alphaOutline > 0.0) {
        fragColor = vec4(col0, alphaOutline); // normal glow
    } else {
        discard; // solid (its just setup up there in the loop)
    }
}