attribute vec4 vPosition;
attribute vec2 vCoordinate;
uniform mat4 vMatrix;

varying vec2 textureCoordinate;

void main(){
    gl_Position= vMatrix*vPosition;
    textureCoordinate = vCoordinate;
}