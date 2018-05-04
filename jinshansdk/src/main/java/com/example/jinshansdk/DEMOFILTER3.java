package com.example.jinshansdk;


import com.ksy.recordlib.service.hardware.ksyfilter.KSYImageFilter;

/**
 * Created by hansentian on 4/8/28.
 */
public class DEMOFILTER3 extends KSYImageFilter {
    // Fragment shader that attempts to produce a high contrast image
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform  float greenplus;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    float color = ((tc.r  + tc.g + tc.b ) / 3.0) ;\n" +
                    "    gl_FragColor = vec4(color+ greenplus, color , color, 1.0);\n" +
                    "}\n";

    public DEMOFILTER3() {
        super(NO_TRANSFORMER_VERTEX_SHADER, FRAGMENT_SHADER);
    }

    private int greenplusLocation = -1;

    public void onInitialized() {
        greenplusLocation = getUniformLocation("greenplus");
        setFloat(greenplusLocation, -0.3f);
    }

}
