package fff.flame;

import fff.util.Point2D;

/**
 * Class {@code TransformAffine} provides accessors methods to the affine 
 * parameters of a {@link Transform}. These parameters are six real values 
 * lettered a through f and represent a linear two-dimensional affine 
 * transformation.
 * <br>
 * TransformAffine transformation pseudocode: 
 * <code>
 *   x' = a*x + b*y + c;
 *   y' = d*x + e*y + f;
 * </code>
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
     * @param a the a value
     * @param b the b value
     * @param c the c value
     * @param d the d value
     * @param e the e value
     * @param f the f value
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
     * Sets the a value.
     *
     * @param a the a value
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
     * Sets the b value.
     *
     * @param b the b value
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
     * Sets the c value.
     *
     * @param c the c value
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
     * Sets the d value.
     *
     * @param d the d value
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
     * Sets the e value.
     *
     * @param e the e value
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
     * Sets the f value.
     *
     * @param f the f value
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
     * Returns the a value.
     * 
     * @return the a value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getA() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?0:6)];
    }
    
    /**
     * Returns the b value.
     * 
     * @return the b value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getB() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?2:8)];
    }
    
    /**
     * Returns the c value.
     * 
     * @return the c value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getC() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?4:10)];
    }
    
    /**
     * Returns the d value.
     * 
     * @return the d value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getD() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?3:9)];
    }
    
    /**
     * Returns the e value.
     * 
     * @return the e value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getE() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return transform.flame.xformAffine[transform.index*12+(preaf?1:7)];
    }
    
    /**
     * Returns the f value.
     * 
     * @return the f value
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
     * Returns {@code true} this object is still valid and its methods can be 
     * called without throwing an {@code IllegalStateException}.
     *
     * @return {@code true} if this {@code TransformAffine} is still valid
     */
    public boolean isValid() {
        return transform.isValid();
    }
    
    /**
     * Applies this affine transformation to the given point.
     * @param point the point to transform
     */
    public void apply(Point2D point) {
        point.set(
            point.x*getA() + point.y*getB() + getC(), 
            point.x*getD() + point.y*getE() + getF());
    }
}
