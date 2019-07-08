package com.isaacudy.kfilter.filters

import com.isaacudy.kfilter.Kfilter

class MonochromeFilter (var intensity : Float = 1.0f ): Kfilter() {

    override fun getShader(): String {
        return """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 textureCoord;
        uniform samplerExternalOES externalTexture;
        precision lowp float;
        const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);
        void main() {
            float intensity = $intensity;
            lowp vec4 textureColor = texture2D(externalTexture, textureCoord);
            float luminance = dot(textureColor.rgb, luminanceWeighting);

            lowp vec4 desat = vec4(vec3(luminance), 1.0);

            lowp vec4 outputColor = vec4(
            (desat.r < 0.5 ? (2.0 * desat.r * 0.6) : (1.0 - 2.0 * (1.0 - desat.r) * (1.0 - 0.6))),
            (desat.g < 0.5 ? (2.0 * desat.g * 0.45) : (1.0 - 2.0 * (1.0 - desat.g) * (1.0 - 0.45))),
            (desat.b < 0.5 ? (2.0 * desat.b * 0.3) : (1.0 - 2.0 * (1.0 - desat.b) * (1.0 - 0.3))),
            1.0
            );

            gl_FragColor = vec4(mix(textureColor.rgb, outputColor.rgb, intensity), textureColor.a);
        }


        """
    }

    override fun copy(): Kfilter {
        return MonochromeFilter(intensity)
    }
}