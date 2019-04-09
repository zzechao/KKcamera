precision mediump float;
uniform sampler2D vTexture;
varying highp vec2 aCoordinate;

const mat3 yuv2rgb = mat3(
	1, 0, 1.2802,
	1, -0.214821, -0.380589,
	1, 2.127982, 0
);

void main() {
	vec3 yuv = vec3(
		1.1643 * (texture2D(vTexture, aCoordinate).r - 0.0625),
		texture2D(vTexture, aCoordinate).a - 0.5,
		texture2D(vTexture, aCoordinate).r - 0.5
	);\
	vec3 rgb = yuv * yuv2rgb;
	gl_FragColor = vec4(rgb, 1);
}