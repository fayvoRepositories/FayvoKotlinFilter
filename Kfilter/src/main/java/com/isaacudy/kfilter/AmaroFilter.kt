package com.isaacudy.kfilter

class AmaroFilter() : Kfilter() {

    override fun getShader(): String {

        return """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;
            varying vec2 textureCoord;
            uniform samplerExternalOES externalTexture;
            precision mediump float;

//            uniform sampler2D u_Texture0;
//            uniform sampler2D u_Texture1;
            const vec3 W = vec3(0.2125, 0.7154, 0.0721);

            vec3 BrightnessContrastSaturation(vec3 color, float brt, float con, float sat)
            {
                vec3 black = vec3(0., 0., 0.);
                vec3 middle = vec3(0.5, 0.5, 0.5);
                float luminance = dot(color, W);
                vec3 gray = vec3(luminance, luminance, luminance);

                vec3 brtColor = mix(black, color, brt);
                vec3 conColor = mix(middle, brtColor, con);
                vec3 satColor = mix(gray, conColor, sat);
                return satColor;
            }

            vec3 ovelayBlender(vec3 Color, vec3 filter){
                vec3 filter_result;
                float luminance = dot(filter, W);

                if(luminance < 0.5)
                    filter_result = 2. * filter * Color;
                else
                    filter_result = 1. - (1. - (2. *(filter - 0.5)))*(1. - Color);

                return filter_result;
            }

            void main()
            {
                 //get the pixel
                 vec2 st = textureCoord.st;
                 vec3 filter = texture2D(externalTexture, st).rgb;

                 //adjust the brightness/contrast/saturation
                 float T_bright = 1.04;
                 float T_contrast = 1.15;
                 float T_saturation = 1.05;
                 vec3 bcs_result = BrightnessContrastSaturation(filter, T_bright, T_contrast, T_saturation);

                 gl_FragColor = vec4(bcs_result, 1.);
            }
        """
    }

    override fun copy(): Kfilter {
        return AmaroFilter()
    }
}