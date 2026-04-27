// NOTE THIS IS UNUSED!

#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
in vec2 oneTexel;
uniform vec4 color;
uniform vec4 outlinecolor;
out vec4 fragColor;
uniform int quality;

uniform vec2 InSize;
uniform float alpha0;

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);

    if (centerCol.a != 0) {
        fragColor = color;  // Solid fill for the inside
        return;
    }

    float alphaOutline = 0.0;
    vec3 colorFinal = outlinecolor.rgb;
    bool hasOutline = false;

    for (int x = -quality; x <= quality; x++) {
        for (int y = -quality; y <= quality; y++) {
            vec2 offset = vec2(x, y);
            vec2 coord = texCoord + offset * oneTexel;
            vec4 t = texture(DiffuseSampler, coord);

            if (t.a != 0) {
                float dst = length(offset);

                // Hard 1px outline
                if (dst <= 1.5) {
                    hasOutline = true;
                }

                // Glow effect
                float falloff = exp((-dst * 100.0) / 500.0);
                alphaOutline += (max(0.0, falloff) * 0.01 * outlinecolor.a);
            }
        }
    }

    if (hasOutline) {
        fragColor = vec4(outlinecolor.rgb, 1.0);  // Hard outline
    } else if (alphaOutline > 0.0) {
        fragColor = vec4(colorFinal, alphaOutline);  // Glow effect
    } else {
        discard;
    }
}