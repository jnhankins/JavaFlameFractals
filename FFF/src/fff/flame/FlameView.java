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

package fff.flame;

/** 
 * Class {@code FlameView} provides accessors methods to a {@link Flame}'s view
 * parameters: {@code translationX}, {@code translationY}, {@code rotation}, and
 * {@code scale}. These parameters generate an affine transformation which maps
 * coordinates from the Flame Fractal Algorithm render-plane to the output image
 * plane. This allows the fractal to be translated, rotated, and scaled (in that
 * order).
 *
 * @author Jeremiah N. Hankins
 */
public class FlameView {
    private final Flame flame;
    
    FlameView(Flame flame) {
        this.flame = flame;
    }
    
    /**
     * Sets the amount the x and y-coordinates will be translated, rotated, and
     * scaled.
     *
     * @param translationX the x-coordinate translation
     * @param translationY the y-coordinate translation
     * @param rotation the rotation
     * @param scale the scale
     * @throws IllegalArgumentException if {@code translationX} is not in range (inf,inf)
     * @throws IllegalArgumentException if {@code translationY} is not in range (inf,inf)
     * @throws IllegalArgumentException if {@code rotation} is not in range (inf,inf)
     * @throws IllegalArgumentException if {@code scale} is not in range (0,inf)
     */
    public void set(float translationX, float translationY, float rotation, float scale) {
        setTranslationX(translationX);
        setTranslationY(translationY);
        setRotation(rotation);
        setScale(scale);
    }
    
    /**
     * Sets the amount the x-coordinates will be translated.
     * 
     * @param translationX the x-coordinate translation
     * @throws IllegalArgumentException if {@code translationX} is not in range (inf,inf)
     */
    public void setTranslationX(float translationX) {
        if (!(Float.NEGATIVE_INFINITY<translationX && translationX<Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("translationX is not in range (inf,inf): "+translationX);
        flame.flameViewAffine[0] = translationX;
    }
    
    /**
     * Sets the amount the y-coordinates will be translated.
     * 
     * @param translationY the y-coordinate translation
     * @throws IllegalArgumentException if {@code translationY} is not in range (inf,inf)
     */
    public void setTranslationY(float translationY) {
        if (!(Float.NEGATIVE_INFINITY<translationY && translationY<Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("translationY is not in range (inf,inf): "+translationY);
        flame.flameViewAffine[1] = translationY;
    }
    
    /**
     * Sets the amount the coordinates will be rotated in degrees clockwise.
     * The given value will be modulated into range [0,360).
     *
     * @param rotation the rotation
     * @throws IllegalArgumentException if {@code rotation} is not in range (inf,inf)
     */
    public void setRotation(float rotation) {
        if (!(Float.NEGATIVE_INFINITY<rotation && rotation<Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("rotation is not in range (inf,inf): "+rotation);
        flame.flameViewAffine[2] = (rotation % 360 + 360) % 360;
    }
    
    /**
     * Sets the amount the coordinates will be scaled.
     * 
     * @param scale the scale
     * @throws IllegalArgumentException if {@code scale} is not in range [0,inf)
     */
    public void setScale(float scale) {
        if (!(0<=scale && scale<Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("scale is not in range [0,inf): "+scale);
        flame.flameViewAffine[3] = scale;
    }

    /**
     * Returns the amount the x-coordinates will be translated.
     * 
     * @return the x-translation
     */
    public float getTranslationX() {
        return flame.flameViewAffine[0];
    }

    /**
     * Returns the amount the y-coordinates will be translated.
     * 
     * @return the y-translation
     */
    public float getTranslationY() {
        return flame.flameViewAffine[1];
    }
    
    /**
     * Returns the amount that the coordinates will be rotated in degrees 
     * clockwise.
     * 
     * @return the rotation
     */
    public float getRotation() {
        return flame.flameViewAffine[2];
    }
    
    /**
     * Returns the amount the coordinates will be scaled.
     *
     * @return the scale
     */
    public float getScale() {
        return flame.flameViewAffine[3];
    }

    @Override
    public String toString() {
        return "View("+getTranslationX()+", "+getTranslationY()+", "+getRotation()+", "+getScale()+")";
    }
}
