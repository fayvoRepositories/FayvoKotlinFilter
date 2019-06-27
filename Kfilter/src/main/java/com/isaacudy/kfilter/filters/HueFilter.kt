package com.isaacudy.kfilter.filters

import com.isaacudy.kfilter.Kfilter

class HueFilter (var hueValue : Float = 4.0f) : Kfilter() {

    override fun getShader(): String {
        hueValue = ((10 - 45) / 45f + 0.5f) * -1
        return """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;
            varying vec2 textureCoord;
            uniform samplerExternalOES externalTexture;
//            uniform samplerExternalOES sTexture;
            float hue = $hueValue;
            void main() {
                vec4 kRGBToYPrime = vec4 (0.299, 0.587, 0.114, 0.0);
                vec4 kRGBToI = vec4 (0.595716, -0.274453, -0.321263, 0.0);
                vec4 kRGBToQ = vec4 (0.211456, -0.522591, 0.31135, 0.0);

                vec4 kYIQToR = vec4 (1.0, 0.9563, 0.6210, 0.0);
                vec4 kYIQToG = vec4 (1.0, -0.2721, -0.6474, 0.0);
                vec4 kYIQToB = vec4 (1.0, -1.1070, 1.7046, 0.0);

                vec4 color = texture2D(externalTexture, textureCoord);

                float YPrime = dot(color, kRGBToYPrime);
                float I = dot(color, kRGBToI);
                float Q = dot(color, kRGBToQ);

                float chroma = sqrt (I * I + Q * Q);
                Q = chroma * sin (hue);
                I = chroma * cos (hue);

                vec4 yIQ = vec4 (YPrime, I, Q, 0.0);
                color.r = dot (yIQ, kYIQToR);
                color.g = dot (yIQ, kYIQToG);
                color.b = dot (yIQ, kYIQToB);
                gl_FragColor = color;
            }
        """
    }

    override fun copy(): Kfilter {
        return HueFilter(hueValue)
    }
}