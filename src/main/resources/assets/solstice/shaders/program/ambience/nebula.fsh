#version 150
uniform sampler2D DiffuseSampler;

out vec4 fragColor;

uniform float u_time;
uniform vec2 u_resolution;
uniform vec3 u_color;
uniform float u_opacity;
uniform float u_glow;

// ======== HASH & NOISE ========

float hash21(vec2 p)
{
    p = fract(p * vec2(233.34, 851.73));
    p += dot(p, p + 23.45);
    return fract(p.x * p.y);
}

float noise(vec2 p)
{
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// ======== FBM (Fractal Brownian Motion) ========

float fbm(vec2 p, int octaves)
{
    float val = 0.0;
    float amp = 0.5;
    float freq = 1.0;
    for (int i = 0; i < octaves; i++)
    {
        val += amp * noise(p * freq);
        freq *= 2.0;
        amp *= 0.5;
    }
    return val;
}

// ======== DOMAIN-WARPED FBM (volumetric depth) ========

float warpedFbm(vec2 p, float time)
{
    // First warp pass — low-frequency drift
    vec2 q = vec2(
        fbm(p + vec2(0.0, 0.0) + time * 0.04, 5),
        fbm(p + vec2(5.2, 1.3) - time * 0.03, 5)
    );

    // Second warp pass — higher turbulence
    vec2 r = vec2(
        fbm(p + 4.0 * q + vec2(1.7, 9.2) + time * 0.06, 5),
        fbm(p + 4.0 * q + vec2(8.3, 2.8) - time * 0.05, 5)
    );

    return fbm(p + 4.0 * r, 6);
}

// ======== STAR FIELD (shared with black_hole.fsh) ========

float starField(vec2 uv, float time)
{
    float stars = 0.0;
    for (float layer = 1.0; layer < 4.0; layer += 1.0)
    {
        vec2 scaled = uv * (15.0 * layer);
        vec2 id = floor(scaled);
        vec2 gv = fract(scaled) - 0.5;
        float n = hash21(id);
        vec2 offset = vec2(n, fract(n * 34.56)) - 0.5;
        float d = length(gv - offset);
        float brightness = smoothstep(0.04 / layer, 0.0, d);
        brightness *= 0.5 + 0.5 * sin(time * (0.3 + n * 1.5) + n * 6.2831);
        stars += brightness;
    }
    return stars;
}

// ======== NEBULA COLOR PALETTE ========

vec3 nebulaPalette(float t, vec3 tint)
{
    // Base cosmic palette: deep blue/purple → warm tint from u_color
    vec3 deepSpace = vec3(0.05, 0.02, 0.12);
    vec3 purple    = vec3(0.35, 0.1, 0.45);
    vec3 warm      = tint;

    // Three-stop gradient driven by FBM density
    vec3 col = deepSpace;
    col = mix(col, purple, smoothstep(0.2, 0.45, t));
    col = mix(col, warm,   smoothstep(0.45, 0.75, t));

    return col;
}

// ======== MAIN ========

void main()
{
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float aspect = u_resolution.x / u_resolution.y;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);

    float time = u_time;

    // ---- Nebula Gas Layers (domain-warped FBM for volume) ----
    float density1 = warpedFbm(p * 2.5, time);
    float density2 = warpedFbm(p * 3.0 + vec2(10.0, 7.0), time * 0.8);
    float density3 = fbm(p * 5.0 + time * 0.1, 4);

    // Combine layers for depth
    float density = density1 * 0.5 + density2 * 0.35 + density3 * 0.15;

    // ---- Color from palette (u_color tints the warm regions) ----
    vec3 nebulaCol = nebulaPalette(density, u_color);

    // Emission intensity driven by u_glow
    float emission = smoothstep(0.3, 0.7, density) * u_glow;
    nebulaCol *= (0.4 + emission * 1.6);

    // Bright filament edges (where gas density changes rapidly)
    float edge = abs(density1 - density2);
    float filament = smoothstep(0.0, 0.15, edge) * u_glow * 0.6;
    nebulaCol += u_color * filament;

    // ---- Background Stars (visible through gas) ----
    float stars = starField(p, time);

    // Stars dim where nebula gas is dense, shine through thin regions
    float gasTransparency = 1.0 - smoothstep(0.25, 0.65, density);
    vec3 starTint = mix(vec3(0.7, 0.8, 1.0), u_color, 0.3);
    vec3 starColor = stars * starTint * gasTransparency;

    // Brightest stars punch through even dense gas
    float punchThrough = smoothstep(0.8, 1.0, stars) * 0.4;
    starColor += stars * vec3(1.0) * punchThrough;

    // ---- Compose ----
    vec3 col = nebulaCol + starColor;

    fragColor = vec4(col, u_opacity);
}
