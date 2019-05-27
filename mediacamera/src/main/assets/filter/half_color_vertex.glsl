attribute vec4 vPosition;
attribute vec2 vCoordinate;
uniform mat4 vMatrix;

varying vec2 textureCoordinate;
varying vec4 aPos;
varying vec4 gPosition;

void main(){
    gl_Position=vMatrix*vPosition;
    aPos=vPosition;
    textureCoordinate=vCoordinate.xy;
    gPosition=vMatrix*vPosition;
}