package com.isaacudy.kfilter.filters

import com.isaacudy.kfilter.Kfilter

class BrightnessFilter(var brightness: Float = 1.2f) : Kfilter() {

    override fun getShader(): String {
        if (brightness < 0.1f)
            brightness = 0.1f
        if (brightness > 2.0f)
            brightness = 2.0f
        return """
            #extension GL_OES_EGL_image_external : require

            precision mediump float;
            varying vec2 textureCoord;
            uniform samplerExternalOES externalTexture;
            float brightness = $brightness;
            void main() {
                vec4 color = texture2D(externalTexture, textureCoord);
                gl_FragColor = brightness * color;
            }
        """
    }

    override fun copy(): Kfilter {
        return BrightnessFilter(brightness)
    }
}