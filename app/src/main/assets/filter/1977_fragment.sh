precision lowp float;
varying vec2 aCoordinate;

uniform sampler2D vTexture;
uniform sampler2D vTexture2;

void main()
    {
       vec4 texel = texture2D(vTexture, aCoordinate);
       gl_FragColor = vec4(texture2D(vTexture2, vec2(texel.r, .16666)).r,
                                              texture2D(vTexture, vec2(texel.g, .5)).g,
                                              texture2D(vTexture, vec2(texel.b, .83333)).b,
                                              texel.a);
     }