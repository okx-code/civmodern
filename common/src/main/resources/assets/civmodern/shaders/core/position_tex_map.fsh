#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 texCoord0;

out vec4 fragColor;

// TODO uniform?
const int waterColour = 4210943;
const vec4 waterColourRGBA = vec4(float((waterColour >> 16) & 0xFF) / 255.0, float((waterColour >> 8) & 0xFF) / 255.0, float(waterColour & 0xFF) / 255.0, 1.0);

void main() {
    vec4 blockColour = texture(Sampler0, texCoord0);
    vec4 realBlockColour = vec4(blockColour.r, blockColour.g, blockColour.b, 1.0);
    fragColor = mix(realBlockColour, waterColourRGBA, blockColour.a) * ColorModulator;
}
