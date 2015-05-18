package fff.flame;

/**
 * Class {@code TransformColor} provides accessor methods to the color parameters of a
 * {@link FlameTransform}: a, r, g, and b.
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
     * @return {@code true} if this {@code FlameTransformAffine} is still valid
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
