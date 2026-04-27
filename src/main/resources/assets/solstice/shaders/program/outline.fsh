#version 330

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;
uniform int RenderMode;
uniform float FillOpacity;
uniform int ShaderStyle;
uniform float OutlineWidth;
uniform float Feather;
uniform float Brightness;
uniform float PulseSpeed;
uniform float PulseAmount;
uniform float Time;

out vec4 fragColor;

float quad(float x) {
    return x * x;
}

float noise(vec2 uv) {
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453123);
}

float applyStyle(float alpha, vec2 uv, bool fillPass) {
    float result = alpha;

    switch (ShaderStyle) {
        case 1:
            result = smoothstep(0.05, 0.8, alpha);
            break;
        case 2:
            result = pow(alpha, 0.8) * 1.25;
            break;
        case 3:
            result = smoothstep(0.0, 1.2, alpha) * 1.4;
            break;
        case 4:
            result = alpha * (0.75 + sin(Time * PulseSpeed + uv.y * 24.0) * PulseAmount);
            break;
        case 5:
            result = alpha * (0.55 + (0.45 + 0.45 * sin(Time * PulseSpeed * 0.75 + uv.y * 42.0)) * max(0.2, PulseAmount));
            break;
        case 6:
            result = smoothstep(0.0, 0.85, alpha) * (fillPass ? max(0.25, 0.65 - PulseAmount * 0.2) : 0.9);
            break;
        case 7:
            result = alpha * (0.7 + noise(floor(uv * InSize * 0.35) + Time * 2.0) * max(0.15, PulseAmount));
            break;
        default:
            break;
    }

    return clamp(result, 0.0, 2.0);
}

vec3 applyBrightness(vec3 color) {
    float multiplier = Brightness;
    if (ShaderStyle == 2) multiplier *= 1.15;
    if (ShaderStyle == 3) multiplier *= 1.08;
    if (ShaderStyle == 6) multiplier *= 0.9;
    return color * multiplier;
}

void main() {
    float divider = max(1.0, Feather);
    float maxSample = 3;
    vec4 current = texture(DiffuseSampler, texCoord);
    float width = max(1.0, OutlineWidth);

    if (current.a != 0) {
        if (RenderMode == 1) discard;
        float fillAlpha = applyStyle(current.a * FillOpacity, texCoord, true);
        fragColor = vec4(applyBrightness(current.rgb), fillAlpha);
    } else {
        if (RenderMode == 0) discard;
        float alpha = 0;

        for (float x = -width; x < width; x++) {
            for (float y = -width; y < width; y++) {
                vec4 texture = texture(DiffuseSampler, texCoord + vec2(x, y) * oneTexel);

                if (texture.a != 0) {
                    current = texture;
                    alpha += max(0, (maxSample - distance(vec2(x, y), vec2(0))) / divider);
                }
            }
        }

        float outlineAlpha = applyStyle(quad(alpha), texCoord, false);
        fragColor = vec4(applyBrightness(current.rgb), outlineAlpha);
    }
}
