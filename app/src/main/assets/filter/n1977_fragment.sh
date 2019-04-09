precision mediump float;

 uniform vec3 vChangeColor;

 varying mediump vec2 aCoordinate;

 uniform sampler2D vTexture;
 uniform sampler2D inputImageTexture2;

 void main()
 {

     vec4 texel = texture2D(vTexture, aCoordinate);

     texel = vec4(
                  texture2D(inputImageTexture2, vec2(texel.r, vChangeColor.r)).r,
                  texture2D(inputImageTexture2, vec2(texel.g, vChangeColor.g)).g,
                  texture2D(inputImageTexture2, vec2(texel.b, vChangeColor.b)).b,
                  texel.a);

     gl_FragColor = texel;
 }
