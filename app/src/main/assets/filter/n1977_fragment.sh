precision mediump float;

 varying mediump vec2 aCoordinate;

 uniform sampler2D vTexture;
 uniform sampler2D inputImageTexture2;

 void main()
 {

     vec3 texel = texture2D(vTexture, aCoordinate).rgb;

     texel = vec3(
                  texture2D(inputImageTexture2, vec2(texel.r, .16666)).r,
                  texture2D(inputImageTexture2, vec2(texel.g, .5)).g,
                  texture2D(inputImageTexture2, vec2(texel.b, .83333)).b);

     gl_FragColor = vec4(texel, 1.0);
 }
