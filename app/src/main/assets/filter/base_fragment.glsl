#extension GL_OES_EGL_image_external : require
precision mediump float;

varying vec2 aCoordinate;

uniform samplerExternalOES inputImageTexture;

void main() {
    gl_FragColor = texture2D( inputImageTexture, aCoordinate );
}
