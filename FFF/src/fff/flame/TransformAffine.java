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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import fff.util.Point2D;

/**
 * Class {@code TransformAffine} provides methods to access and modify the 
 * {@link Transform#getPreAffine() pre-affine} and
 * {@link Transform#getPstAffine() post-affine} components of a
 * {@link Transform}. Each {@code TransformAffine} is composed of six real
 * valued parameters lettered a through f and represent a linear two-dimensional
 * <a href="https://en.wikipedia.org/wiki/Affine_transformation">affine
 * transformation (wiki)</a>.
 * <br>
 * <pre>Affine transformation pseudocode:{@code
 *   x' = a*x + b*y + c;
 *   y' = d*x + e*y + f;}</pre>
 * 
 * @see Transform#getPreAffine() 
 * @see Transform#getPstAffine() 
 * 
 * @author Jeremiah N. Hankins
 */
public class TransformAffine {
    private final Transform transform;
    private final boolean preaf;
    
    TransformAffine(Transform transform, boolean preaf) {
        this.transform = transform;
        this.preaf = preaf;
    }
    
    /**
     * Sets the {@code TransformAffine} to be the identity transformation.
     * 
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public void setIdent() {
        set(1, 0, 0, 0, 1, 0);
    }
    
    /**
     * Sets the values of the {@code TransformAffine}.
     *
     * @param a the {@code a} value
     * @param b the {@code b} value
     * @param c the {@code c} value
     * @param d the {@code d} value
     * @param e the {@code e} value
     * @param f the {@code f} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code a} is not in range (-inf,inf)
     * @throws IllegalArgumentException if {@code b} is not in range (-inf,inf)
     * @throws IllegalArgumentException if {@code c} is not in range (-inf,inf)
     * @throws IllegalArgumentException if {@code d} is not in range (-inf,inf)
     * @throws IllegalArgumentException if {@code e} is not in range (-inf,inf)
     * @throws IllegalArgumentException if {@code f} is not in range (-inf,inf)
     */
    public void set(float a, float b, float c, float d, float e, float f) {
        setA(a);
        setB(b);
        setC(c);
        setD(d);
        setE(e);
        setF(f);
    }

    /**
     * Sets the {@code a} value.
     *
     * @param a the {@code a} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code a} is not in range (-inf,inf)
     */
    public void setA(float a) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if(!(Float.NEGATIVE_INFINITY < a && a < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("a is not in range (-inf,inf): "+a);
        transform.flame.xformAffine[transform.index*12+(preaf?0:6)] = a;
    }

    /**
     * Sets the {@code b} value.
     *
     * @param b the {@code b} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code b} is not in range (-inf,inf)
     */
    public void setB(float b) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if(!(Float.NEGATIVE_INFINITY < b && b < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("b is not in range (-inf,inf): "+b);
        transform.flame.xformAffine[transform.index*12+(preaf?2:8)] = b;
    }

    /**
     * Sets the {@code c} value.
     *
     * @param c the {@code c} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code c} is not in range (-inf,inf)
     */
    public void setC(float c) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if(!(Float.NEGATIVE_INFINITY < c && c < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("c is not in range (-inf,inf): "+c);
        transform.flame.xformAffine[transform.index*12+(preaf?4:10)] = c;
    }

    /**
     * Sets the {@code d} value.
     *
     * @param d the {@code d} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code d} is not in range (-inf,inf)
     */
    public void setD(float d) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if(!(Float.NEGATIVE_INFINITY < d && d < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("d is not in range (-inf,inf): "+d);
        transform.flame.xformAffine[transform.index*12+(preaf?3:9)] = d;
    }

    /**
     * Sets the {@code e} value.
     *
     * @param e the {@code e} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code e} is not in range (-inf,inf)
     */
    public void setE(float e) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if(!(Float.NEGATIVE_INFINITY < e && e < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("e is not in range (-inf,inf): "+e);
        transform.flame.xformAffine[transform.index*12+(preaf?1:7)] = e;
    }

    /**
     * Sets the {@code f} value.
     *
     * @param f the {@code f} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code f} is not in range (-inf,inf)
     */
    public void setF(float f) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if(!(Float.NEGATIVE_INFINITY < f && f < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("f is not in range (-inf,inf): "+f);
        transform.flame.xformAffine[transform.index*12+(preaf?5:11)] = f;
    }
    
    /**
     * Returns the {@code a} value.
     * 
     * @return the {@code a} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getA() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?0:6)];
    }
    
    /**
     * Returns the {@code b} value.
     * 
     * @return the {@code b} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getB() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?2:8)];
    }
    
    /**
     * Returns the {@code c} value.
     * 
     * @return the {@code c} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getC() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?4:10)];
    }
    
    /**
     * Returns the {@code d} value.
     * 
     * @return the {@code d} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getD() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?3:9)];
    }
    
    /**
     * Returns the {@code e} value.
     * 
     * @return the {@code e} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getE() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?1:7)];
    }
    
    /**
     * Returns the {@code f} value.
     * 
     * @return the {@code f} value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists

     */
    public float getF() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?5:11)];
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
        return "Affine("+getA()+", "+getB()+", "+getC()+", "+getD()+", "+getE()+", "+getF()+")";
    }
    
    /**
     * Returns {@code true} if this object is still valid and its methods can be 
     * called without throwing an {@code IllegalStateException}.
     *
     * @return {@code true} if this {@code TransformAffine} is still valid
     * 
     * @see Flame
     */
    public boolean isValid() {
        return transform.isValid();
    }
    
    /**
     * Applies this affine transformation to the given point.
     * 
     * @param point the point to transform
     */
    public void apply(Point2D point) {
        point.set(
            point.x*getA() + point.y*getB() + getC(), 
            point.x*getD() + point.y*getE() + getF());
    }
    
    /**
     * Scales {@code a}, {@code c}, and {@code d} by {@code x} and scales
     * {@code b}, {@code e}, and {@code f} by {@code y}.
     * 
     * @param x the amount to scale {@code a}, {@code c}, and {@code d}
     * @param y the amount to scale {@code d}, {@code e}, and {@code f}
     * @throws IllegalArgumentException if {@code x} or {@code y} is not finite
     */
    public void scale(float x, float y) {
        if(!Double.isFinite(x))
            throw new IllegalArgumentException("x is not finite: "+x);
        if(!Double.isFinite(y))
            throw new IllegalArgumentException("y is not finite: "+y);
        setA(getA()*x);
        setC(getC()*x);
        setD(getD()*x);
        setB(getB()*y);
        setE(getE()*y);
        setF(getF()*y);
    }
    
    /**
     * Scales {@code a} and {@code d} by {@code x} and scales {@code b} and
     * {@code e} by {@code y}.
     * 
     * @param x the amount to scale {@code a} and {@code d}
     * @param y the amount to scale {@code b} and {@code e}
     * @throws IllegalArgumentException if {@code x} or {@code y} is not finite
     */
    public void scaleScalars(float x, float y) {
        if(!Double.isFinite(x))
            throw new IllegalArgumentException("x is not finite: "+x);
        if(!Double.isFinite(y))
            throw new IllegalArgumentException("y is not finite: "+y);
        setA(getA()*x);
        setD(getD()*x);
        setB(getB()*y);
        setE(getE()*y);
    }
    
    /**
     * Scales {@code c} by {@code x} and {@code f} by {@code y}.
     * 
     * @param x the amount to scale {@code c}
     * @param y the amount to scale {@code f}
     * @throws IllegalArgumentException if {@code x} or {@code y} is not finite
     */
    public void scaleTranslation(float x, float y) {
        if(!Double.isFinite(x))
            throw new IllegalArgumentException("x is not finite: "+x);
        if(!Double.isFinite(y))
            throw new IllegalArgumentException("y is not finite: "+y);
        setC(getC()*x);
        setF(getF()*y);
    }
    
    /**
     * Adds {@code x} to {@code c} and {@code y} to {@code f}.
     * 
     * @param x the amount to add to {@code c}
     * @param y the amount to add to {@code f}
     * @throws IllegalArgumentException if {@code x} or {@code y} is not finite
     */
    public void translate(float x, float y) {
        if(!Double.isFinite(x))
            throw new IllegalArgumentException("x is not finite: "+x);
        if(!Double.isFinite(y))
            throw new IllegalArgumentException("y is not finite: "+y);
        setC(getC()+x);
        setF(getF()+y);
    }
    
    /**
     * Rotates the affine clockwise by the specified amount.
     * 
     * @param angle the amount to rotate the affine in radians
     * @throws IllegalArgumentException if {@code angle} is not finite
     */
    public void rotate(double angle) {
        if(!Double.isFinite(angle))
            throw new IllegalArgumentException("angle is not finite: "+angle);
        angle = angle%(2*Math.PI);
        float sin = (float)Math.sin(angle);
        float cos = (float)Math.cos(angle);
        float a = getA();
        float b = getB();
        float c = getC();
        float d = getD();
        float e = getE();
        float f = getF();
        setA(a*cos-d*sin);
        setB(b*cos-e*sin);
        setC(c*cos-f*sin);
        setD(d*cos+a*sin);
        setE(e*cos+b*sin);
        setF(f*cos+c*sin);
    }
    
    /**
     * Rotates {@code c} and {@code f} clockwise by the specified amount.
     * 
     * @param angle the amount to rotate {@code c} and {@code f} in radians
     * @throws IllegalArgumentException if {@code angle} is not finite
     */
    public void rotateTranslation(double angle) {
        if(!Double.isFinite(angle))
            throw new IllegalArgumentException("angle is not finite: "+angle);
        angle = angle%(2*Math.PI);
        float sin = (float)Math.sin(angle);
        float cos = (float)Math.cos(angle);
        float c = getC();
        float f = getF();
        setC(c*cos-f*sin);
        setF(f*cos+c*sin);
    }
    
    /**
     * Rotates {@code a}, {@code b}, {@code d}, and {@code e} clockwise by the
     * specified amount.
     * 
     * @param angle the amount to rotate in radians
     * @throws IllegalArgumentException if {@code angle} is not finite
     */
    public void rotateScalars(double angle) {
        if(!Double.isFinite(angle))
            throw new IllegalArgumentException("angle is not finite: "+angle);
        angle = angle%(2*Math.PI);
        float sin = (float)Math.sin(angle);
        float cos = (float)Math.cos(angle);
        float a = getA();
        float b = getB();
        float d = getD();
        float e = getE();
        setA(a*cos-d*sin);
        setB(b*cos-e*sin);
        setD(d*cos+a*sin);
        setE(e*cos+b*sin);
    }
}
