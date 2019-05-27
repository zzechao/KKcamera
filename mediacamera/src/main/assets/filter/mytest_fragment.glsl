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
            vec3 metal = texture2D(inputImageTexture2, textureCoordinate).rgb;
            vec3 metaled = vec3(
                                texture2D(inputImageTexture3, vec2(metal.r, metal.r)).r,
                                texture2D(inputImageTexture3, vec2(metal.g, metal.g)).g,
                                texture2D(inputImageTexture3, vec2(metal.b, metal.b)).b
                                );

            gl_FragColor = vec4(metaled, 1.0);
    }else{
         gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
    }
 }
