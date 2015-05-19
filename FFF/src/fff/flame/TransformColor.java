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
 * Class {@code TransformColor} provides methods to access and modify the color 
 * parameters of a {@link Transform}. The color {@code Transform} is composed
 * of three components: {@link #setR(float) red}, {@link #setG(float) green}, 
 * and {@link #setB(float) blue}.
 * <p>
 * The weight of the transforms color is stored separately. Use
 * {@link Transform#getColorWeight()} and
 * {@link Transform#setColorWeight(float)} to access and modify the color's
 * weight.
 *
 * @see Transform#getColor()
 * 
 * @author Jeremiah N. Hankins
 */
public class TransformColor {
    private final Transform transform;

    TransformColor(Transform transform) {
        this.transform = transform;
    }

    /**
     * Sets the alpha, red, green, and blue-component values.
     * 
     * @param r the red-component value
     * @param g the green-component value
     * @param b the blue-component value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code a} is not in range [0,1]
     * @throws IllegalArgumentException if {@code r} is not in range [0,1]
     * @throws IllegalArgumentException if {@code g} is not in range [0,1]
     * @throws IllegalArgumentException if {@code b} is not in range [0,1]
     */
    public void set(float r, float g, float b) {
        setR(r);
        setG(g);
        setB(b);
    }
    
    /**
     * Sets the red-component value.
     * 
     * @param r the red-component value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code g} is not in range [0,1]
     */
    public void setR(float r) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if (!(0<=r && r<=1))
            throw new IllegalArgumentException("r is not in range [0,1]: "+r);
        transform.flame.xformColors[transform.index*4+1] = r;
    }
    
    /**
     * Sets the green-component value.
     * 
     * @param g the green-component value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code g} is not in range [0,1]
     */
    public void setG(float g) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if (!(0<=g && g<=1))
            throw new IllegalArgumentException("g is not in range [0,1]: "+g);
        transform.flame.xformColors[transform.index*4+2] = g;
    }
    
    /**
     * Sets the blue-component value.
     * 
     * @param b the blue-component value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code b} is not in range [0,1]
     */
    public void setB(float b) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if (!(0<=b && b<=1))
            throw new IllegalArgumentException("b is not in range [0,1]: "+b);
        transform.flame.xformColors[transform.index*4+3] = b;
    }

    /**
     * Returns the red-component value.
     * 
     * @return the red-component value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getR() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformColors[transform.index*4+1];
    }

    /**
     * Returns the green-component value.
     * 
     * @return the green-component value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getG() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformColors[transform.index*4+2];
    }

    /**
     * Returns the blue-component value.
     * 
     * @return the blue-component value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getB() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformColors[transform.index*4+3];
    }
    
    /**
     * Returns {@code true} this object is still valid and its methods can be 
     * called without throwing an {@code IllegalStateException}.
     *
     * @return {@code true} if this {@code TransformAffine} is still valid
     */
    public boolean isValid() {
        return transform.isValid();
    }

    /**
     * Returns a {@code String} representation of this {@code TransformColor}.
     * 
     * @return a {@code String} representation of this {@code TransformColor}
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    @Override
    public String toString() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return "Color("+getR()+", "+getG()+", "+getB()+")";
    }
}
