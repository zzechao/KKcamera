precision mediump float;

 varying mediump vec2 textureCoordinate;

 uniform sampler2D inputImageTexture;
 uniform sampler2D inputImageTexture2;
 uniform sampler2D inputImageTexture3;

varying vec4 aPos;
uniform int vIsHalf;

 void main()
 {
     if(aPos.x > 0.0 || vIsHalf == 0){
        vec3 texel = texture2D(inputImageTexture, textureCoordinate).rgb;

        vec3 metal = texture2D(inputImageTexture3, textureCoordinate).rgb;

        vec3 metaled = vec3(
                            texture2D(inputImageTexture2, vec2(metal.r, texel.r)).r,
                            texture2D(inputImageTexture2, vec2(metal.g, texel.g)).g,
                            texture2D(inputImageTexture2, vec2(metal.b, texel.b)).b
                            );

         vec4 texel1 = vec4(
             metaled.r,
             metaled.g,
             metaled.b,
             1.0);

         gl_FragColor = texel1;
    }else{
         gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
    }
 }
