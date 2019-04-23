precision mediump float;
varying vec2 textureCoordinate;
uniform sampler2D inputImageTexture;
uniform sampler2D inputImageTextureAlpha;

void main() {
    vec4 color=texture2D( inputImageTexture, textureCoordinate);
    color.a=texture2D(inputImageTextureAlpha,textureCoordinate).r;
    gl_FragColor = color;
}