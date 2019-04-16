#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES inputImageTexture;
uniform int vChangeType;
uniform vec3 vChangeColor;
uniform int vIsHalf;
uniform float uXY;

varying vec4 gPosition;

varying vec2 textureCoordinate;
varying vec4 aPos;

void modifyColor(vec4 color){
    color.r=max(min(color.r,1.0),0.0);
    color.g=max(min(color.g,1.0),0.0);
    color.b=max(min(color.b,1.0),0.0);
    color.a=max(min(color.a,1.0),0.0);
}

void main(){
    vec4 nColor=texture2D(inputImageTexture,textureCoordinate);
    if(aPos.y>0.0||vIsHalf==0){
        if(vChangeType==1){
            float c=nColor.r*vChangeColor.r+nColor.g*vChangeColor.g+nColor.b*vChangeColor.b;
            gl_FragColor=vec4(c,c,c,nColor.a);
        }else if(vChangeType==2){
            vec4 deltaColor=nColor+vec4(vChangeColor,0.0);
            modifyColor(deltaColor);
            gl_FragColor=deltaColor;
        }else if(vChangeType==3){
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x-vChangeColor.r,textureCoordinate.y-vChangeColor.r));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x-vChangeColor.r,textureCoordinate.y+vChangeColor.r));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x+vChangeColor.r,textureCoordinate.y-vChangeColor.r));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x+vChangeColor.r,textureCoordinate.y+vChangeColor.r));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x-vChangeColor.g,textureCoordinate.y-vChangeColor.g));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x-vChangeColor.g,textureCoordinate.y+vChangeColor.g));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x+vChangeColor.g,textureCoordinate.y-vChangeColor.g));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x+vChangeColor.g,textureCoordinate.y+vChangeColor.g));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x-vChangeColor.b,textureCoordinate.y-vChangeColor.b));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x-vChangeColor.b,textureCoordinate.y+vChangeColor.b));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x+vChangeColor.b,textureCoordinate.y-vChangeColor.b));
            nColor+=texture2D(inputImageTexture,vec2(textureCoordinate.x+vChangeColor.b,textureCoordinate.y+vChangeColor.b));
            nColor/=13.0;
            gl_FragColor=nColor;
        }else if(vChangeType==4){
            float dis=distance(vec2(gPosition.x,gPosition.y/uXY),vec2(vChangeColor.r,vChangeColor.g));
            if(dis<vChangeColor.b){
                nColor=texture2D(inputImageTexture,vec2(textureCoordinate.x/2.0+0.25,textureCoordinate.y/2.0+0.25));
            }
            gl_FragColor=nColor;
        }else{
            gl_FragColor=nColor;
        }
    }else{
        gl_FragColor=nColor;
    }
}