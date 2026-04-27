#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;
uniform int quality;
uniform vec2 InSize;
uniform float alpha0;
uniform float alpha1;

uniform vec2 resolution;
uniform float time;

uniform vec4 first;
uniform vec3 second;
uniform vec3 third;

uniform vec4 ffirst;
uniform vec3 fsecond;
uniform vec3 fthird;

uniform int oct;
uniform int overlay;

float heightMap(vec2 p) {
    p *= 3.;
    vec2 h = vec2(p.x + p.y * .57735, p.y * 1.1547);
    vec2 fh = floor(h);
    vec2 f = h - fh;
    h = fh;
    float c = fract((h.x + h.y) / 3.);
    h =  c < .666 ? (c < .333 ?  h : h + 1.) : h + step(f.yx, f);
    p -= vec2(h.x - h.y * .5, h.y * .8660254);
    c = fract(cos(dot(h, vec2(41, 289))) * 43758.5453);
    p -= p * step(c, .5) * 2.;
    p -= vec2(-1, 0);
    c = dot(p, p);
    p -= vec2(1.5, .8660254);
    c = min(c, dot(p, p));
    p -= vec2(0, -1.73205);
    c = min(c, dot(p, p));
    return sqrt(c);
}

float map(vec3 p) {
    float c = heightMap(p.xy);
    c = cos(c * 6.2831589) + cos(c * 6.2831589*2.);
    c = (clamp(c * .6 +.5, 0., 1.));
    return 1. - p.z - c * .025;
}

float[5] getNormal(vec3 p) {
    float[5] points;
    vec2 e = vec2(.01, 0);
    float d1 = map(p + e.xyy), d2 = map(p - e.xyy);
    float d3 = map(p + e.yxy), d4 = map(p - e.yxy);
    float d5 = map(p + e.yyx), d6 = map(p - e.yyx);
    float d = map(p) * 2.;
    float edge = abs(d1 + d2 - d) + abs(d3 + d4 - d) + abs(d5 + d6 - d);
    edge = smoothstep(0., 1., sqrt(edge / e.x * 2.));
    float crv = clamp((d1 + d2 + d3 + d4 + d5 + d6 - d * 3.) * 32. + .6, 0., 1.);
    e = vec2(.0025, 0);
    d1 = map(p + e.xyy);
    d2 = map(p - e.xyy);
    d3 = map(p + e.yxy);
    d4 = map(p - e.yxy);
    d5 = map(p + e.yyx);
    d6 = map(p - e.yyx);
    vec3 newVec = normalize(vec3(d1 - d2, d3 - d4, d5 - d6));

    points[0] = newVec.r;
    points[1] = newVec.g;
    points[2] = newVec.b;
    points[3] = edge;
    points[4] = crv;

    return points;
}

float calculateAO(vec3 p, vec3 n) {
    float sca = 2., occ = 0.;
    for(float i = 0.; i < 5.; i++){

        float hr = .01 + i * .5 / 4.;
        float dd = map(n * hr + p);
        occ += (hr - dd) * sca;
        sca *= 0.7;
    }
    return clamp(1.0 - occ, 0., 1.);
}

float n3D(vec3 p) {
    const vec3 s = vec3(7, 157, 113);
    vec3 ip = floor(p);
    p -= ip;
    vec4 h = vec4(0., s.yz, s.y + s.z) + dot(ip, s);
    p = p * p * (3. - 2. * p);
    h = mix(fract(sin(mod(h, 6.2831589)) * 43758.5453), fract(sin(mod(h + s.x, 6.2831589)) * 43758.5453), p.x);
    h.xy = mix(h.xz, h.yw, p.y);
    return mix(h.x, h.y, p.z);
}

vec3 envMap(vec3 rd, vec3 sn) {
    vec3 sRd = rd;
    rd.xy -= time * .25;
    rd *= 3.;
    float c = n3D(rd) * .57 + n3D(rd * 2.) * .28 + n3D(rd * 4.) * .15;
    c = smoothstep(.4, 1., c);
    vec3 col = vec3(c, c * c, c * c * c * c);
    return mix(col, col.yzx, sRd * .25 + .25);
}

vec2 hash22(vec2 p) {
    float n = sin(mod(dot(p, vec2(41, 289)), 6.2831589));
    return fract(vec2(262144, 32768)*n)*.75 + .25;
}

float Voronoi(vec2 p) {
    vec2 g = floor(p), o; p -= g;
    vec3 d = vec3(1);
    for(int y = -1; y <= 1; y++){
        for(int x = -1; x <= 1; x++){
            o = vec2(x, y);
            o += hash22(g + o) - p;
            d.z = dot(o, o);
            d.y = max(d.x, min(d.y, d.z));
            d.x = min(d.x, d.z);

        }
    }
    return max(d.y / 1.2 - d.x * 1., 0.) / 1.2;
}

vec3 getColor(vec4 centerCol) {
    vec3 rd = normalize(vec3(2. * gl_FragCoord.xy - resolution.xy, resolution.y));
    float tm = time / 2.;
    vec2 a = sin(vec2(1.570796, 0) + sin(tm / 4.) * .3);
    rd.xy = mat2(a, -a.y, a.x) * rd.xy;
    vec3 ro = vec3(tm, cos(tm / 4.), 0.);
    vec3 lp = ro + vec3(cos(tm / 2.) * .5, sin(tm / 2.) * .5, -.5);
    float d, t = 0.;
    for (int j = 0; j < 32; j++){
        d = map(ro + rd * t);
        t += d * .7;
        if(d < 0.001) break;
    }
    vec3 sp = ro + rd * t;
    float[5] nn4 = getNormal(sp);
    vec3 sn = vec3(nn4[0], nn4[1], nn4[2]);
    float edge = nn4[3];
    float crv = nn4[4];
    vec3 ld = lp - sp;
    float c = heightMap(sp.xy);
    vec3 fold = cos(vec3(1, 2, 4) * c * 6.2831589);
    float c2 = heightMap((sp.xy + sp.z * .025) * 6.);
    c2 = cos(c2 * 6.2831589 * 3.);
    c2 = (clamp(c2 +.5, 0., 1.));
    vec3 oC = vec3(1);
    if(fold.x > 0.) {
        oC = vec3(1, .05, .1) * c2;
    }
    if(fold.x < 0.05 && (fold.y) < 0.) {
        oC = vec3(1, .7, .45) * (c2 * .25 + .75);
    } else if(fold.x < 0.) {
        oC = vec3(1, .8, .4) * c2;
    }
    float p1 = 1.0 - smoothstep(0., .1, fold.x * .5 + .5);
    float p2 = 1.0 - smoothstep(0., .1, Voronoi(sp.xy * 4. + vec2(tm, cos(tm / 4.))));
    p1 = (p2 + .25) * p1;
    oC += oC.yxz * p1 * p1;
    float lDist = max(length(ld), 0.001);
    float atten = 1./(1. + lDist*.125);
    ld /= lDist;
    float diff = max(dot(ld, sn), 0.);
    float spec = pow(max( dot( reflect(-ld, sn), -rd ), 0.0 ), 16.);
    float fre = pow(clamp(dot(sn, rd) + 1., .0, 1.), 3.);
    crv = crv*.9 + .1;
    float ao = calculateAO(sp, sn);
    vec3 col = oC * (diff + .5) + vec3(1., .7, .4) * spec * 2. + vec3(.4, .7, 1) * fre;
    col += (oC * .5+.5) * envMap(reflect(rd, sn), sn) * 6.;
    col *= 1. - edge * .85;
    col *= (atten * crv * ao);
    return vec3(sqrt(clamp(col, 0., 1.)));
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);
    vec3 col0 = getColor(centerCol);

    if (centerCol.a != 0.0) {
        fragColor = vec4(col0, alpha1);
        return;
    }

    float alphaOutline = 0.;
    bool hasOutline = false;

    for (int x = -quality; x < quality + 1; x++) {
        for (int y = -quality; y < quality + 1; y++) {
            vec2 offset = vec2(x, y);
            vec2 coord = texCoord + offset * oneTexel;
            vec4 t = texture(DiffuseSampler, coord);
            if (t.a != 0) {
                if (alpha0 == -1.0) {
                    float dst = length(offset);

                    if (dst <= 1.5) {
                        hasOutline = true;
                    }

                    float falloff = exp((-dst * 100.0) / 500.0);
                    alphaOutline += (max(0.0, falloff) * 0.01) * ffirst.a;
                } else {
                    fragColor = vec4(col0, alpha0);
                    return;
                }
            }
        }
    }

    if (hasOutline && overlay == 1) {
        fragColor = vec4(col0, 1.0);
    } else if (alphaOutline > 0.0) {
        fragColor = vec4(col0, alphaOutline); // normal glow
    } else {
        discard; // solid (its just setup up there in the loop)
    }
}