precision mediump float;

 varying mediump vec2 textureCoordinate;

 uniform sampler2D inputImageTexture;
 uniform sampler2D inputImageTexture2;

varying vec4 aPos;
uniform int vIsHalf;


 void main()
 {
     if(aPos.x > 0.0 || vIsHalf == 0){
         vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;

         vec3 edge = texture2D(inputImageTexture2, textureCoordinate).rgb;

         texel = texel * edge;

         gl_FragColor = vec4(texel.r,texel.g,texel.b,1.0);
    }else{
        gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
    }
 }
