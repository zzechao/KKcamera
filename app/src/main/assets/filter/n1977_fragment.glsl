precision mediump float;

 uniform vec3 vChangeColor;

 varying mediump vec2 textureCoordinate;

 uniform sampler2D inputImageTexture;
 uniform sampler2D inputImageTexture2;

 void main()
 {

     vec4 texel = texture2D(inputImageTexture, textureCoordinate);

     texel = vec4(
                  texture2D(inputImageTexture2, vec2(texel.r, vChangeColor.r)).r,
                  texture2D(inputImageTexture2, vec2(texel.g, vChangeColor.g)).g,
                  texture2D(inputImageTexture2, vec2(texel.b, vChangeColor.b)).b,
                  texel.a);

     gl_FragColor = texel;
 }
