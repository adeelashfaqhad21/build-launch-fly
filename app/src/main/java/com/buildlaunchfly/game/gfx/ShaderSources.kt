package com.buildlaunchfly.game.gfx

/**
 * All GLSL ES 3.00 sources. Kept in one place so the shader/uniform contract between the
 * renderer and the programs is easy to audit.
 */
object ShaderSources {

    private const val FRAG_HEADER = """#version 300 es
precision highp float;
"""

    /** Lighting, shadow lookup, fog and tone mapping shared by every lit program. */
    private const val ENV_COMMON = """
uniform vec3 uCameraPos;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uSkyColor;
uniform vec3 uGroundColor;
uniform vec3 uFogColor;
uniform float uFogDensity;
uniform float uShadowStrength;
uniform float uShadowTexel;
uniform mat4 uLightViewProj;
uniform sampler2D uShadowMap;

float sampleShadow(vec3 worldPos, vec3 n) {
    vec4 lightClip = uLightViewProj * vec4(worldPos + n * 0.05, 1.0);
    if (lightClip.w <= 0.0001) {
        return 1.0;
    }
    vec3 proj = lightClip.xyz / lightClip.w;
    proj = proj * 0.5 + 0.5;
    if (proj.z > 1.0 || proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0) {
        return 1.0;
    }
    float bias = 0.0016;
    float sum = 0.0;
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            float depth = texture(uShadowMap, proj.xy + vec2(float(x), float(y)) * uShadowTexel).r;
            sum += ((proj.z - bias) <= depth) ? 1.0 : 0.0;
        }
    }
    return sum / 9.0;
}

vec3 shadeSurface(vec3 albedo, vec3 n, vec3 v, vec3 worldPos, float roughness, float metallic, float emissive) {
    vec3 l = normalize(uSunDir);
    float ndl = max(dot(n, l), 0.0);
    float ndv = max(dot(n, v), 0.001);
    vec3 h = normalize(v + l);
    float ndh = max(dot(n, h), 0.0);
    float vdh = max(dot(v, h), 0.0);

    float rough = clamp(roughness, 0.05, 1.0);
    float a = rough * rough;
    float a2 = a * a;
    float denom = ndh * ndh * (a2 - 1.0) + 1.0;
    float distribution = a2 / max(3.14159265 * denom * denom, 0.0001);

    float k = (rough + 1.0) * (rough + 1.0) / 8.0;
    float gv = ndv / (ndv * (1.0 - k) + k);
    float gl = ndl / (ndl * (1.0 - k) + k);
    float geometry = gv * gl;

    float metal = clamp(metallic, 0.0, 1.0);
    vec3 f0 = mix(vec3(0.04), albedo, metal);
    vec3 fresnel = f0 + (1.0 - f0) * pow(1.0 - vdh, 5.0);
    vec3 specular = (distribution * geometry * fresnel) / max(4.0 * ndv * ndl, 0.001);

    float shadow = mix(1.0, sampleShadow(worldPos, n), clamp(uShadowStrength, 0.0, 1.0));
    vec3 diffuse = albedo * (1.0 - metal) / 3.14159265;
    vec3 direct = (diffuse + specular) * uSunColor * ndl * shadow;

    vec3 ambientColor = mix(uGroundColor, uSkyColor, n.y * 0.5 + 0.5);
    vec3 ambient = ambientColor * albedo * (1.0 - rough * 0.5) * (1.0 - metal * 0.6);

    return direct + ambient + albedo * emissive;
}

vec3 applyFog(vec3 color, vec3 worldPos) {
    float dist = length(uCameraPos - worldPos);
    float fog = 1.0 - exp(-max(uFogDensity, 0.0) * dist);
    return mix(color, uFogColor, clamp(fog, 0.0, 1.0));
}

vec3 applyTonemap(vec3 c) {
    vec3 mapped = c / (c + vec3(1.0));
    return pow(max(mapped, vec3(0.0)), vec3(1.0 / 2.2));
}
"""

    val PBR_VERT = """#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;

uniform mat4 uModel;
uniform mat4 uNormalMat;
uniform mat4 uViewProj;
uniform float uUvScale;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUv;

void main() {
    vec4 world = uModel * vec4(aPosition, 1.0);
    vWorldPos = world.xyz;
    vNormal = normalize(mat3(uNormalMat) * aNormal);
    vUv = aUv * uUvScale;
    gl_Position = uViewProj * world;
}
"""

    val PBR_INSTANCED_VERT = """#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;

layout(location = 4) in vec4 aInstance0;
layout(location = 5) in vec4 aInstance1;
layout(location = 6) in vec4 aInstance2;
layout(location = 7) in vec4 aInstance3;

uniform mat4 uViewProj;
uniform float uUvScale;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vUv;

void main() {
    mat4 model = mat4(aInstance0, aInstance1, aInstance2, aInstance3);
    vec4 world = model * vec4(aPosition, 1.0);
    vWorldPos = world.xyz;
    vNormal = normalize(mat3(model) * aNormal);
    vUv = aUv * uUvScale;
    gl_Position = uViewProj * world;
}
"""

    val PBR_FRAG = FRAG_HEADER + ENV_COMMON + """
in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUv;

uniform sampler2D uAlbedo;
uniform vec3 uTint;
uniform float uRoughness;
uniform float uMetallic;
uniform float uEmissive;

out vec4 fragColor;

void main() {
    vec3 albedo = texture(uAlbedo, vUv).rgb * uTint;
    vec3 n = normalize(vNormal);
    vec3 v = normalize(uCameraPos - vWorldPos);
    vec3 color = shadeSurface(albedo, n, v, vWorldPos, uRoughness, uMetallic, uEmissive);
    color = applyFog(color, vWorldPos);
    fragColor = vec4(applyTonemap(color), 1.0);
}
"""

    val TERRAIN_FRAG = FRAG_HEADER + ENV_COMMON + """
in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUv;

uniform sampler2D uTexA;
uniform sampler2D uTexB;
uniform sampler2D uTexC;
uniform vec3 uTintA;
uniform vec3 uTintB;
uniform vec3 uTintC;
uniform float uRockThreshold;
uniform float uSnowLine;

out vec4 fragColor;

void main() {
    vec3 n = normalize(vNormal);
    float upness = clamp(n.y, 0.0, 1.0);

    vec3 a = texture(uTexA, vUv).rgb * uTintA;
    vec3 b = texture(uTexB, vUv * 0.37).rgb * uTintB;
    vec3 c = texture(uTexC, vUv * 2.13).rgb * uTintC;

    float rockMask = 1.0 - smoothstep(uRockThreshold - 0.24, uRockThreshold, upness);
    vec3 albedo = mix(a, b, rockMask);

    float highMask = smoothstep(uSnowLine, uSnowLine + 45.0, vWorldPos.y);
    albedo = mix(albedo, c, highMask);

    float detail = texture(uTexC, vUv * 0.07).r;
    albedo *= 0.92 + detail * 0.16;

    vec3 v = normalize(uCameraPos - vWorldPos);
    vec3 color = shadeSurface(albedo, n, v, vWorldPos, 0.9, 0.0, 0.0);
    color = applyFog(color, vWorldPos);
    fragColor = vec4(applyTonemap(color), 1.0);
}
"""

    val WATER_FRAG = FRAG_HEADER + ENV_COMMON + """
in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vUv;

uniform sampler2D uNormals;
uniform vec3 uDeepColor;
uniform vec3 uShallowColor;
uniform float uTime;
uniform float uReflections;

out vec4 fragColor;

void main() {
    vec2 uv = vWorldPos.xz * 0.035;
    vec3 n1 = texture(uNormals, uv + vec2(uTime * 0.012, uTime * 0.019)).xyz * 2.0 - 1.0;
    vec3 n2 = texture(uNormals, uv * 1.73 - vec2(uTime * 0.008, uTime * 0.015)).xyz * 2.0 - 1.0;
    // Two normal-map frequencies plus directional geometric wavelets keep the
    // surface readable at both close river scale and distant ocean scale.
    float waveA = sin(vWorldPos.x * 0.032 + uTime * 0.72) *
                  cos(vWorldPos.z * 0.021 - uTime * 0.48);
    float waveB = sin(vWorldPos.x * 0.011 - vWorldPos.z * 0.017 + uTime * 0.31);
    vec3 n = normalize(vec3(n1.x + n2.x + waveA * 0.16 + waveB * 0.12,
                            3.15, n1.z + n2.z + waveA * 0.12 - waveB * 0.14));

    vec3 v = normalize(uCameraPos - vWorldPos);
    float viewFacing = clamp(dot(n, v), 0.0, 1.0);
    float fresnel = pow(1.0 - viewFacing, 4.0);
    float shoreMix = clamp(0.5 + waveA * 0.22 + waveB * 0.12, 0.0, 1.0);
    vec3 base = mix(uShallowColor, uDeepColor, shoreMix * 0.72);
    base = mix(base, uShallowColor, fresnel * 0.55);

    vec3 h = normalize(normalize(uSunDir) + v);
    float spec = pow(max(dot(n, h), 0.0), 220.0);
    float crest = smoothstep(0.62, 0.92, waveA * 0.65 + waveB * 0.35);
    vec3 foam = vec3(0.72, 0.84, 0.86) * crest * (1.0 - viewFacing) * 0.18;

    vec3 reflection = uSkyColor * fresnel * 0.45 * uReflections;
    vec3 color = base + uSunColor * spec * 1.4 + reflection + foam;
    color = applyFog(color, vWorldPos);
    fragColor = vec4(applyTonemap(color), 0.93);
}
"""

    val SKY_VERT = """#version 300 es
layout(location = 0) in vec2 aPos;

uniform mat4 uInvViewProj;

out vec3 vDir;

void main() {
    vec4 farPoint = uInvViewProj * vec4(aPos, 1.0, 1.0);
    vDir = farPoint.xyz / farPoint.w;
    gl_Position = vec4(aPos, 1.0, 1.0);
}
"""

    val SKY_FRAG = FRAG_HEADER + """
in vec3 vDir;

uniform vec3 uCameraPos;
uniform vec3 uSunDir;
uniform vec3 uSunColor;
uniform vec3 uZenith;
uniform vec3 uHorizon;
uniform float uTime;

out vec4 fragColor;

void main() {
    vec3 d = normalize(vDir - uCameraPos);
    float h = clamp(d.y, -1.0, 1.0);
    float t = pow(clamp(h, 0.0, 1.0), 0.55);
    vec3 sky = mix(uHorizon, uZenith, t);
    sky = mix(sky, uHorizon * 0.55, smoothstep(0.0, -0.3, h));

    float sunDot = max(dot(d, normalize(uSunDir)), 0.0);
    sky += uSunColor * pow(sunDot, 220.0) * 1.6;
    sky += uSunColor * pow(sunDot, 6.0) * 0.10;

    // Multi-scale procedural cloud field. Project the view ray onto a high cloud layer,
    // then combine broad masses with finer breakup so clouds do not read as a single sine band.
    vec2 cloudUv = d.xz / max(h + 0.16, 0.16) * 0.42;
    cloudUv += vec2(uTime * 0.0018, -uTime * 0.0011);
    float cloudLarge = sin(cloudUv.x * 1.7 + sin(cloudUv.y * 1.2)) *
                       sin(cloudUv.y * 1.5 - cos(cloudUv.x * 0.8));
    float cloudFine = sin(cloudUv.x * 5.2 + cloudUv.y * 2.7) *
                      cos(cloudUv.y * 4.6 - cloudUv.x * 1.9);
    float cloudShape = cloudLarge * 0.72 + cloudFine * 0.18;
    float cloudMask = smoothstep(0.08, 0.48, cloudShape) *
                      smoothstep(0.015, 0.22, h) *
                      (1.0 - smoothstep(0.72, 0.98, h));
    float cloudLight = 0.78 + 0.22 * clamp(sunDot, 0.0, 1.0);
    vec3 cloudColor = mix(vec3(0.62, 0.70, 0.78), vec3(1.0, 0.98, 0.94), cloudLight);
    sky = mix(sky, cloudColor, cloudMask * 0.58);

    vec3 mapped = sky / (sky + vec3(1.0));
    fragColor = vec4(pow(max(mapped, vec3(0.0)), vec3(1.0 / 2.2)), 1.0);
}
"""

    val DEPTH_VERT = """#version 300 es
layout(location = 0) in vec3 aPosition;

uniform mat4 uModel;
uniform mat4 uLightViewProj;

void main() {
    gl_Position = uLightViewProj * uModel * vec4(aPosition, 1.0);
}
"""

    val DEPTH_INSTANCED_VERT = """#version 300 es
layout(location = 0) in vec3 aPosition;

layout(location = 4) in vec4 aInstance0;
layout(location = 5) in vec4 aInstance1;
layout(location = 6) in vec4 aInstance2;
layout(location = 7) in vec4 aInstance3;

uniform mat4 uLightViewProj;

void main() {
    mat4 model = mat4(aInstance0, aInstance1, aInstance2, aInstance3);
    gl_Position = uLightViewProj * model * vec4(aPosition, 1.0);
}
"""

    val DEPTH_FRAG = FRAG_HEADER + """
out vec4 fragColor;

void main() {
    fragColor = vec4(1.0);
}
"""

    val PARTICLE_VERT = """#version 300 es
layout(location = 0) in vec4 aParticle;

uniform mat4 uViewProj;
uniform float uPointScale;

out float vDepth;

void main() {
    vec4 clip = uViewProj * vec4(aParticle.xyz, 1.0);
    gl_Position = clip;
    gl_PointSize = clamp(aParticle.w * uPointScale / max(clip.w, 0.1), 1.0, 96.0);
    vDepth = clip.w;
}
"""

    val PARTICLE_FRAG = FRAG_HEADER + """
in float vDepth;

uniform vec3 uColor;
uniform vec3 uFogColor;
uniform float uFogDensity;

out vec4 fragColor;

void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    float r = dot(p, p);
    if (r > 1.0) {
        discard;
    }
    float alpha = smoothstep(1.0, 0.15, r);
    float fog = 1.0 - exp(-vDepth * max(uFogDensity, 0.0));
    vec3 c = mix(uColor, uFogColor, clamp(fog, 0.0, 1.0));
    vec3 mapped = c / (c + vec3(1.0));
    fragColor = vec4(pow(max(mapped, vec3(0.0)), vec3(1.0 / 2.2)), alpha * 0.85);
}
"""
}
