const float zoom_quickness = 0.8;

varying vec2 vTextureCoord;
uniform sampler2D sTexture1;
uniform sampler2D sTexture2;
uniform float progress;
uniform float isZoomIn;//0.0:表示拉近；1.0:表示推远

float nQuick = clamp(zoom_quickness, 0.2, 1.0);

vec2 zoom(vec2 uv, float amount) {
    return 0.5 + ((uv - 0.5) * (1.0-amount));
}

vec4 transition (vec2 uv) {
    float realProgress = 0.0;
    vec4 texture1;
    vec4 texture2;
    if (isZoomIn == 0.0){
        realProgress = progress;
        vec2 zoom = zoom(uv, smoothstep(0.0, nQuick, realProgress));
        texture1 = texture2D(sTexture1, zoom);
        texture2 = texture2D(sTexture2, uv);
    } else{
        realProgress = 1.0 - progress;
        vec2 zoom = zoom(uv, smoothstep(0.0, nQuick, realProgress));
        texture1 = texture2D(sTexture2, zoom);
        texture2 = texture2D(sTexture1, uv);
    }


    return mix(texture1, texture2, smoothstep(nQuick-0.2, 1.0, realProgress));
}

void main() {
    gl_FragColor = vec4(transition(vTextureCoord));
}
