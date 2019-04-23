attribute vec4 vPosition;
attribute vec2 vCoordinate;
varying vec2 textureCoordinate;
uniform mat4 vMatrix;

void main(){
    textureCoordinate = vCoordinate;
    gl_Position = vMatrix*vPosition;
}