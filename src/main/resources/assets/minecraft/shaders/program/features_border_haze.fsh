#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;
uniform vec3 HazeColor;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    vec2 uv = texCoord - vec2(0.5);
    float dist = length(uv) * 1.55;
    float vig = smoothstep(0.15, 1.05, dist);
    float wash = clamp(Intensity * (0.28 + 0.55 * vig), 0.0, 0.85);
    vec3 washed = mix(color.rgb, HazeColor, wash);
    washed = mix(washed, washed * 0.88 + HazeColor * 0.12, Intensity * vig * 0.65);
    fragColor = vec4(washed, 1.0);
}
