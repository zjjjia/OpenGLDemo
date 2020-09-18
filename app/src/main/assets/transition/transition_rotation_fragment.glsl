//#version 120
#define PI 3.14159265359
precision mediump float;

const vec2 center = vec2(0.5, 0.5);
const float scale = 8.0;
const vec4 backColor = vec4(0.15, 0.15, 0.15, 1.0);

varying vec2 vTextureCoord;//接收从顶点着色器过来的参数
uniform float rotations;//正数为顺时针，负数为逆时针
uniform sampler2D sTexture1;//纹理内容数据
uniform sampler2D sTexture2;
uniform float progress;

vec4 clockwiseRotation (vec2 uv) {

    vec2 difference = uv - center;
    vec2 dir = normalize(difference);
    float dist = length(difference);

    float angle = 2.0 * PI * rotations * progress;

    float c = cos(angle);
    float s = sin(angle);

    float currentScale = mix(scale, 1.0, 2.0 * abs(progress - 0.5));

    vec2 rotatedDir = vec2(dir.x  * c - dir.y * s, dir.x * s + dir.y * c);
    vec2 rotatedUv = center + rotatedDir * dist / currentScale;

    if (rotatedUv.x < 0.0 || rotatedUv.x > 1.0 ||rotatedUv.y < 0.0 || rotatedUv.y > 1.0){
        return backColor;
    }

    vec4 texture1 = texture2D(sTexture1, rotatedUv);
    vec4 texture2 = texture2D(sTexture2, rotatedUv);
    return mix(texture1, texture2, progress);
}

void main()
{
    gl_FragColor = vec4(clockwiseRotation(vTextureCoord));
}