precision mediump float;

uniform sampler2D inputImageTexture;
uniform int vIsHalf;

varying vec2 aCoordinate;

void main(){
    gl_FragColor=texture2D(inputImageTexture,aCoordinate);
}