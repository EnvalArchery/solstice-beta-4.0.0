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

float fbm(vec2 p)
{
    float val = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++)
    {
        val += amp * noise(p);
        p *= 2.0;
        amp *= 0.5;
    }
    return val;
}

// ======== STAR FIELD ========

float starField(vec2 uv)
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
        brightness *= 0.5 + 0.5 * sin(u_time * (0.3 + n * 1.5) + n * 6.2831);
        stars += brightness;
    }
    return stars;
}

// ======== GRAVITATIONAL LENSING ========

vec2 gravitationalLens(vec2 p, float mass)
{
    float dist = length(p);
    if (dist < 0.001) return p;
    float deflection = mass / (dist * dist + mass * 0.1);
    return p + normalize(p) * deflection;
}

// ======== ACCRETION DISK ========

float accretionDisk(vec2 p, float time)
{
    float dist = length(p);
    float angle = atan(p.y, p.x);

    float innerRadius = 0.12;
    float outerRadius = 0.50;

    // Radial mask
    float radial = smoothstep(innerRadius, innerRadius + 0.04, dist)
                 * (1.0 - smoothstep(outerRadius - 0.08, outerRadius, dist));

    // Thin disk viewed at an angle (vertical squish)
    float thinness = exp(-abs(p.y) * 12.0 / (dist + 0.01));

    // Swirling "space fire" noise
    float spiral = angle + log(max(dist, 0.01)) * 3.0 - time * 1.5;
    float n1 = noise(vec2(spiral * 2.0, dist * 12.0 - time * 0.8));
    float n2 = noise(vec2(spiral * 5.0 + 1.7, dist * 25.0 - time * 0.5));
    float n3 = fbm(vec2(spiral * 1.5 + time * 0.3, dist * 8.0));
    float turbulence = n1 * 0.45 + n2 * 0.3 + n3 * 0.25;

    // Doppler beaming: approaching side is brighter
    float doppler = 0.6 + 0.4 * sin(angle + 0.8);

    return radial * thinness * turbulence * doppler;
}

// ======== MAIN ========

void main()
{
    vec2 uv = gl_FragCoord.xy / u_resolution;
    float aspect = u_resolution.x / u_resolution.y;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);

    float dist = length(p);

    // Black hole parameters
    float eventHorizon = 0.08;
    float photonSphere = eventHorizon * 1.5;
    float lensMass = eventHorizon * eventHorizon * 0.7;

    // ---- Background Stars (gravitationally lensed) ----
    vec2 lensedP = gravitationalLens(p, lensMass);
    float stars = starField(lensedP);
    vec3 starTint = mix(vec3(0.7, 0.8, 1.0), u_color, 0.4);
    vec3 starColor = stars * starTint;

    // ---- Accretion Disk ----
    float disk = accretionDisk(p, u_time) * u_glow;

    // Temperature gradient: white-hot core -> orange -> user color at edges
    float tempGrad = smoothstep(0.12, 0.50, dist);
    vec3 hotCore  = vec3(1.0, 0.95, 0.85);
    vec3 midTemp  = vec3(1.0, 0.5, 0.15);
    vec3 coolEdge = u_color;
    vec3 diskCol  = mix(hotCore, mix(midTemp, coolEdge, tempGrad), tempGrad);
    vec3 diskFinal = diskCol * disk * 3.0;

    // ---- Photon Ring (bright edge at photon sphere) ----
    float ringDist = abs(dist - photonSphere);
    float ring = exp(-ringDist * ringDist * 2500.0) * u_glow * 1.5;
    vec3 ringColor = mix(vec3(1.0, 0.8, 0.35), u_color, 0.3) * ring;

    // ---- Inner glow (faint light just outside event horizon) ----
    float innerGlow = exp(-(dist - eventHorizon) * 20.0)
                    * step(eventHorizon, dist) * u_glow * 0.4;
    vec3 innerGlowColor = mix(vec3(1.0, 0.6, 0.2), u_color, 0.5) * innerGlow;

    // ---- Event Horizon (absolute black) ----
    float holeMask = 1.0 - smoothstep(eventHorizon - 0.004, eventHorizon, dist);

    // ---- Compose ----
    vec3 col = vec3(0.0);
    col += starColor;
    col += diskFinal;
    col += ringColor;
    col += innerGlowColor;
    col *= (1.0 - holeMask);

    fragColor = vec4(col, u_opacity);
}
