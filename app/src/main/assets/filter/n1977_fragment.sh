precision mediump float;

 varying vec2 aCoordinate;

 uniform sampler2D vTexture;
 uniform sampler2D inputImageTexture2;

 void main()
 {

     vec4 nColor = texture2D(vTexture, aCoordinate);

     gl_FragColor = nColor;
 }
