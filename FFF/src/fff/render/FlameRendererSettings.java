/**
 * FastFlameFractals (FFF)
 * A library for rendering flame fractals asynchronously using Java and OpenCL.
 * 
 * Copyright (c) 2015 Jeremiah N. Hankins
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fff.render;

/**
 * Class {@code FlameRendererSettings} contains settings used by a 
 * {@link FlameRenderer}
 * 
 * @author Jeremiah N. Hankins
 */
public class FlameRendererSettings {
    // Image Resolution
    private int width;
    private int height;
    // Maximum Image Time and Quality
    private double maxTime;
    private double maxQuality;
    // Optimization Flags
    private boolean useVariations;
    private boolean usePostAffines;
    private boolean useFinalTransform;
    private boolean useJitter;
    // Blur Kernel
    private boolean useBlur;
    private float blurAlpha;
    private float blurMaxRadius;
    private float blurMinRadius;
    
    /**
     * Constructs a new {@code FlameRendererSettings} with default values.
     */
    public FlameRendererSettings() {
        width = 1280;
        height = 720;
        maxTime = 60.0; // 60 secibds
        maxQuality = 256;
        useVariations = true;
        usePostAffines = true;
        useFinalTransform = true;
        useJitter = true;
        useBlur = false;
        blurAlpha = 0.4f;
        blurMinRadius = 0.0f;
        blurMaxRadius = 9.0f;
    }
    
    /**
     * Constructs a new {@code FlameRendererSettings} by copying the given 
     * {@code FlameRendererSettings}.
     * 
     * @param settings the {@code FlameRendererSettings} to copy
     * @throws IllegalArgumentException if {@code settings} is {@code null}
     */
    public FlameRendererSettings(FlameRendererSettings settings) {
        set(settings);
    }
    
    /**
     * Copies the given {@code FlameRendererSettings}.
     * 
     * @param settings the {@code FlameRendererSettings} to copy
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code settings} is {@code null}
     */
    public FlameRendererSettings set(FlameRendererSettings settings) {
        if (settings == null)
            throw new IllegalArgumentException("settings is null");
        width = settings.width;
        height = settings.height;
        maxTime = settings.maxTime;
        maxQuality = settings.maxQuality;
        useVariations = settings.useVariations;
        usePostAffines = settings.usePostAffines;
        useFinalTransform = settings.useFinalTransform;
        useJitter = settings.useJitter;
        useBlur = settings.useBlur;
        blurAlpha = settings.blurAlpha;
        blurMinRadius = settings.blurMinRadius;
        blurMaxRadius = settings.blurMaxRadius;
        return this;
    }
    
    /**
     * Sets the width of the output image in pixels.
     * 
     * @param width the image width
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code width} is not in the range [1,inf)
     */
    public FlameRendererSettings setWidth(int width) {
        if (!(1 <= width))
            throw new IllegalArgumentException("width is not in the range [1,inf): "+width);
        this.width = width;
        return this;
    }
    
    /**
     * Sets the height of the output image in pixels.
     * 
     * @param height the image height
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code height} is not in the range [1,inf)
     */
    public FlameRendererSettings setHeight(int height) {
        if (!(1 <= height))
            throw new IllegalArgumentException("height is not in the range [1,inf): "+height);
        this.height = height;
        return this;
    }
    
    /**
     * Sets the width and height of the output image in pixels.
     * 
     * @param width the image width
     * @param height the image height
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code width} is not in the range [1,inf)
     * @throws IllegalArgumentException if {@code height} is not in the range [1,inf)
     */
    public FlameRendererSettings setSize(int width, int height) {
        if (!(1 <= width))
            throw new IllegalArgumentException("width is not in the range [1,inf): "+width);
        if (!(1 <= height))
            throw new IllegalArgumentException("height is not in the range [1,inf): "+height);
        this.width = width;
        this.height = height;
        return this;
    }
    
    /**
     * Sets the maximum time in seconds spent rendering a flame.
     * 
     * @param maxTime the maximum rendering time in seconds
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code maxTime} is not in the range [1,inf)
     */
    public FlameRendererSettings setMaxTime(double maxTime) {
        if (!(0 < maxTime && maxTime < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("maxTime is not in the range (0,inf): "+maxTime);
        this.maxTime = maxTime;
        return this;
    }
    
    /**
     * Sets the maximum quality of the image. The quality of the image is
     * calculated by dividing the total number of dots that hive been plotted by
     * the number of unique pixels to which dots have been plotted.
     *
     * @param maxQuality the maximum image quality
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code maxQuality} is not in the range (0,inf)
     */
    public FlameRendererSettings setMaxQuality(double maxQuality) {
        if (!(0 < maxQuality && maxQuality < Double.POSITIVE_INFINITY))
            throw new IllegalArgumentException("maxQuality is not in the range (0,inf): "+maxQuality);
        this.maxQuality = maxQuality;
        return this;
    }
    
    /**
     * Sets the optimization flag that indicates whether or not non-linear
     * variations should be used.
     * 
     * @param useVariations {@code true} if non-linear variations will be used
     * @return this {@code FlameRendererSettings} object
     */
    public FlameRendererSettings setUseVariations(boolean useVariations) {
        this.useVariations = useVariations;
        return this;
    }
    
    /**
     * Sets the optimization flag that indicates whether or not post non-linear
     * variation affines should be used.
     * 
     * @param usePostAffines {@code true} if post non-linear variation affines will be used
     * @return this {@code FlameRendererSettings} object
     */
    public FlameRendererSettings setUsePostAffines(boolean usePostAffines) {
        this.usePostAffines = usePostAffines;
        return this;
    }
    
    /**
     * Sets the optimization flag that indicates whether or not final transforms
     * should be used.
     * 
     * @param useFinalTransform {@code true} if final transforms should be used
     * @return this {@code FlameRendererSettings} object
     */
    public FlameRendererSettings setUseFinalTransform(boolean useFinalTransform) {
        this.useFinalTransform = useFinalTransform;
        return this;
    }
    
    /**
     * Sets the optimization flag that indicates whether or not jitter should be
     * used.
     *
     * @param useJitter {@code true} if final transforms should be used
     * @return this {@code FlameRendererSettings} object
     */
    public FlameRendererSettings setUseJitter(boolean useJitter) {
        this.useJitter = useJitter;
        return this;
    }
    
    /**
     * Sets the flag that indicates whether or not the final image will use
     * density-estimation blurring.
     * 
     * @param useBlur {@code true} if density-estimation blurring will be used
     * @return this {@code FlameRendererSettings} object
     */
    public FlameRendererSettings setUseBlur(boolean useBlur) {
        this.useBlur = useBlur;
        return this;
    }
    
    /**
     * Sets a coefficient that determines how aggressive the blur-kernel will
     * be applied to to low-density portions of the image.
     * 
     * @param blurAlpha the blur-kernel aggressiveness coefficient 
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code blurAlpha} is not in the range [0, inf)
     */
    public FlameRendererSettings setBlurAlpha(float blurAlpha) {
        if (!(0 <= blurAlpha && blurAlpha < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("blurAlpha is not in the range [0,inf): "+blurAlpha);
        this.blurAlpha = blurAlpha;
        return this;
    }
    
    /**
     * Sets the minimum blur-kernel width.
     * 
     * @param blurMinRadius the minimum blur-kernel width
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code blurMinRadius} is not in the range [0, {@link #getBlurMaxRadius()}]
     */
    public FlameRendererSettings setBlurMinRadius(float blurMinRadius) {
        if (!(0 <= blurMinRadius && blurMinRadius <= blurMaxRadius))
            throw new IllegalArgumentException("blurMinRadius is not in the range [0,blurMaxRadius]: "+blurMinRadius);
        this.blurMinRadius = blurMinRadius;
        return this;
    }
    
    /**
     * Sets the maximum blur-kernel width.
     * 
     * @param blurMaxRadius the maximum blur-kernel width
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code blurMaxRadius} is not in the range [{@link #getBlurMinRadius()},inf)
     */
    public FlameRendererSettings setBlurMaxRadius(float blurMaxRadius) {
        if (!(blurMinRadius <= blurMaxRadius && blurMaxRadius < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("blurMaxRadius is not in the range [blurMinRadius,inf): "+blurMaxRadius);
        this.blurMaxRadius = blurMaxRadius;
        return this;
    }
    
    /**
     * Sets the minimum and maximum blur-kernel width.
     * 
     * @param blurMinRadius the minimum blur-kernel width
     * @param blurMaxRadius the maximum blur-kernel width
     * @return this {@code FlameRendererSettings} object
     * @throws IllegalArgumentException if {@code blurMinRadius} or {@code blurMaxRadius} is not in the range [0,inf)
     * @throws IllegalArgumentException if {@code blurMinRadius} is not less than or equal to {@code blurMaxRadius}
     */
    public FlameRendererSettings setBlurMinAndMaxRadius(float blurMinRadius, float blurMaxRadius) {
        if (!(0 <= blurMinRadius && blurMinRadius < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("blurMinRadius is not in the range [0,inf): "+blurMinRadius);
        if (!(0 <= blurMaxRadius && blurMaxRadius < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("blurMaxRadius is not in the range [0,inf): "+blurMaxRadius);
        if (!(blurMinRadius <= blurMaxRadius))
            throw new IllegalArgumentException("blurMinRadius is not less than or equal to blurMaxRadius: min:"+blurMinRadius+" max:"+blurMaxRadius);
        this.blurMinRadius = blurMinRadius;
        this.blurMaxRadius = blurMaxRadius;
        return this;
    }
    
    /**
     * Returns the width of the output image in pixels.
     * 
     * @return the image width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Returns the height of the output image in pixels.
     * 
     * @return the image height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Returns the maximum time in seconds spent rendering a flame.
     * 
     * @return the maximum rendering time in seconds
     */
    public double getMaxTime() {
        return maxTime;
    }
    
    /**
     * Returns the maximum quality of the image. The quality of the image is
     * calculated by dividing the total number of dots that hive been plotted by
     * the number of unique pixels to which dots have been plotted.
     *
     * @return the maximum image quality
     */
    public double getMaxQuality() {
        return maxQuality;
    }
    
    /**
     * Returns the optimization flag that indicates whether or not non-linear
     * variations should be used.
     * 
     * @return {@code true} if non-linear variations will be used
     */
    public boolean getUseVariations() {
        return useVariations;
    }
    
    /**
     * Returns the optimization flag that indicates whether or not post non-linear
     * variation affines should be used.
     * 
     * @return {@code true} if post non-linear variation affines will be used
     */
    public boolean getUsePostAffines() {
        return usePostAffines;
    }
    
    /**
     * Returns the optimization flag that indicates whether or not final transforms
     * should be used.
     * 
     * @return {@code true} if final transforms will be used
     */
    public boolean getUseFinalTransform() {
        return useFinalTransform;
    }
    
    /**
     * Returns the optimization flag that indicates whether or not jitter should be
     * used.
     *
     * @return {@code true} if jitter will be used
     */
    public boolean getUseJitter() {
        return useJitter;
    }
    
    /**
     * Returns the flag that indicates whether or not the final image will use
     * density-estimation blurring.
     * 
     * @return {@code true} if density-estimation blurring will be used
     */
    public boolean getUseBlur() {
        return useBlur;
    }
    
    /**
     * Returns a coefficient that determines how aggressive the blur-kernel will
     * be applied to to low-density portions of the image.
     * 
     * @return the blur-kernel aggressiveness coefficient 
     */
    public float getBlurAlpha() {
        return blurAlpha;
    }
    
    /**
     * Returns the minimum blur-kernel width.
     * 
     * @return the minimum blur-kernel width
     */
    public float getBlurMinRadius() {
        return blurMinRadius;
    }
    
    /**
     * Returns the maximum blur-kernel width.
     * 
     * @return the maximum blur-kernel width
     */
    public float getBlurMaxRadius() {
        return blurMaxRadius;
    }
    
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Settings(");
        builder.append("width=").append(width).append(", ");
        builder.append("height=").append(height).append(", ");
        builder.append("maxTime=").append(maxTime).append(", ");
        builder.append("maxQuality=").append(maxQuality).append(", ");
        builder.append("useVariations=").append(useVariations).append(", ");
        builder.append("usePostAffines=").append(usePostAffines).append(", ");
        builder.append("useFinalTransform=").append(useFinalTransform).append(", ");
        builder.append("useJitter=").append(useJitter).append(", ");;
        builder.append("useBlur=").append(useBlur).append(", ");
        builder.append("blurAlpha=").append(blurAlpha).append(", ");
        builder.append("blurMinRadius=").append(blurMinRadius).append(", ");
        builder.append("blurMaxRadius=").append(blurMaxRadius).append(")");
        return builder.toString();
    }
}
