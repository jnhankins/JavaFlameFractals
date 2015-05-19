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
 * Class {@code FlameBackground} provides accessors methods to a {@link Flame}'s 
 * background color parameters: a, r, g, and b.
 * 
 * @author Jeremiah N. Hankins
 */
public class FlameBackground {
    private final Flame flame;

    FlameBackground(Flame flame) {
        this.flame = flame;
    }

    /**
     * Sets the alpha, red, green, and blue-component values.
     * 
     * @param a the alpha-component value
     * @param r the red-component value
     * @param g the green-component value
     * @param b the blue-component value
     * @throws IllegalArgumentException if {@code a} is not in range [0,1]
     * @throws IllegalArgumentException if {@code r} is not in range [0,1]
     * @throws IllegalArgumentException if {@code g} is not in range [0,1]
     * @throws IllegalArgumentException if {@code b} is not in range [0,1]
     */
    public void set(float a, float r, float g, float b) {
        setA(a);
        setR(r);
        setG(g);
        setB(b);
    }
    
    /**
     * Sets the alpha-component value.
     * 
     * @param a the alpha-component value
     * @throws IllegalArgumentException if {@code a} is not in range [0,1]
     */
    public void setA(float a) {
        if (!(0<=a && a<=1))
            throw new IllegalArgumentException("a is not in range [0,1]: "+a);
        flame.flameBackground[0] = a;
    }
    
    /**
     * Sets the red-component value.
     * 
     * @param r the red-component value
     * @throws IllegalArgumentException if {@code g} is not in range [0,1]
     */
    public void setR(float r) {
        if (!(0<=r && r<=1))
            throw new IllegalArgumentException("r is not in range [0,1]: "+r);
        flame.flameBackground[1] = r;
    }
    
    /**
     * Sets the green-component value.
     * 
     * @param g the green-component value
     * @throws IllegalArgumentException if {@code g} is not in range [0,1]
     */
    public void setG(float g) {
        if (!(0<=g && g<=1))
            throw new IllegalArgumentException("g is not in range [0,1]: "+g);
        flame.flameBackground[2] = g;
    }
    
    /**
     * Sets the blue-component value.
     * 
     * @param b the blue-component value
     * @throws IllegalArgumentException if {@code b} is not in range [0,1]
     */
    public void setB(float b) {
        if (!(0<=b && b<=1))
            throw new IllegalArgumentException("b is not in range [0,1]: "+b);
        flame.flameBackground[3] = b;
    }

    /**
     * Returns the alpha-component value.
     * 
     * @return the alpha-component value
     */
    public float getA() {
        return flame.flameBackground[0];
    }

    /**
     * Returns the red-component value.
     * 
     * @return the red-component value
     */
    public float getR() {
        return flame.flameBackground[1];
    }

    /**
     * Returns the green-component value.
     * 
     * @return the green-component value
     */
    public float getG() {
        return flame.flameBackground[2];
    }

    /**
     * Returns the blue-component value.
     * 
     * @return the blue-component value
     */
    public float getB() {
        return flame.flameBackground[3];
    }

    @Override
    public String toString() {
        return "Background("+getA()+", "+getR()+", "+getG()+", "+getB()+")";
    }
}
