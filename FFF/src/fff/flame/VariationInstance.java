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

import fff.util.Point2D;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code VariationInstance} provides accessors methods to one of a
 * {@link Transform}'s variation parameters: a scalar coefficient and parameter
 * values. Each {@code VariationInstance} is associated with a 
 * {@link VariationDefinition} and represents an instance of that variation
 * definition.
 *
 * @author Jeremiah N. Hankins
 */
public class VariationInstance {
    private final Transform transform;
    private final VariationDefinition definition;
    
    protected VariationInstance(Transform transform, VariationDefinition definition) {
        this.transform = transform;
        this.definition = definition;
    }
    
    /**
     * Sets the variation's scalar coefficient.
     * 
     * @param coefficient the coefficient
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if {@code coefficient} is not in the range [0,inf)
     */
    public void setCoefficient(float coefficient) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if (!(0 <= coefficient && coefficient < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("Coefficient is not in the range [0,inf): "+coefficient);
        int variationIndex = getVariationIndex();
        transform.flame.xformVariations[variationIndex] = coefficient;
    }
    
    /**
     * Sets the value of a parameter.
     * 
     * @param parameter the parameter's name
     * @param value the value
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if the {@link #getParameterMap() parameter map} does not contain {@code parameter}
     * @throws IllegalArgumentException if {@code value} is not in the range (-inf,inf)
     */
    public void setParameter(String parameter, float value) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if (!definition.parameters.containsKey(parameter))
            throw new IllegalArgumentException("the variation does not contain the specified parameter: "+parameter);
        if (!(Float.NEGATIVE_INFINITY < value && value < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("Value is not in the range (-inf,inf): "+value);
        int parameterIndex = getParameterIndex(parameter);
        transform.flame.xformParameters[parameterIndex] = value;
    }
    
    /**
     * Sets all of the parameters to their default values.
     * 
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public void setDefaultParameters() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        for (String parameter: definition.parameters.keySet()) {
            setParameter(parameter, definition.parameters.get(parameter));
        }
    }
    
    /**
     * Returns the variation's scalar coefficient.
     * 
     * @return the coefficient
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getCoefficient() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        int varIdx = getVariationIndex();
        return transform.flame.xformVariations[varIdx];
    }
    
    /**
     * Returns the value of the parameter with the given name.
     * 
     * @param parameter the parameter's name
     * @return the value of the parameter
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if the {@link #getParameterMap() parameter map} does not contain {@code parameter}
     */
    public float getParameter(String parameter) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if (!definition.parameters.containsKey(parameter))
            throw new IllegalArgumentException("the variation does not contain the specified parameter: "+parameter);
        int parameterIndex = getParameterIndex(parameter);
        return transform.flame.xformParameters[parameterIndex];
    }
    
    /**
     * Returns the parameters as an unmodifiable map.
     * 
     * @return the parameters
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public Map<String,Float> getParameterMap() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        Map<String,Float> parameters = new TreeMap();
        for (String parameter: definition.parameters.keySet())
            parameters.put(parameter, getParameter(parameter));
        return Collections.unmodifiableMap(parameters);
    }
    
    /**
     * Returns this {@code VariationInstance}'s {@link VariationDefinition}.
     * 
     * @return the variation definition
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public VariationDefinition getDefinition() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        return definition;
    }
    
    /**
     * Returns {@code true} if this {@code VariationInstance} is still valid and 
     * its methods can be called without throwing an  
     * {@code IllegalStateException}.
     *
     * @return {@code true} if this {@code VariationInstance} is still valid
     */
    public boolean isValid() {
        return transform.isValid() &&
                transform.flame.variationSet.contains(definition) &&
                transform.flame.xformHasVariation[getVariationIndex()];
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
        StringBuilder builder = new StringBuilder();
        builder.append("Variation(");
        builder.append(definition.getName()).append(" ");
        builder.append(getCoefficient());
        for (Map.Entry<String,Float> parameter: getParameterMap().entrySet()) 
            builder.append(" ").append(parameter.getKey()).append("=").append(parameter.getValue());
        builder.append(")");
        return builder.toString();
    }
    
    private int getVariationIndex() {
        int variationOffset = transform.flame.getVariationOffset(definition);
        return transform.index*transform.flame.numVariations+variationOffset;
    }
    
    private int getParameterIndex(String parameter) {
        int parameterOffset = transform.flame.getParameterOffset(definition);
        Iterator<String> it = definition.parameters.keySet().iterator();
        while (it.hasNext() && !it.next().equals(parameter))
            parameterOffset++;
        return transform.index*transform.flame.numParameters+parameterOffset;
    }
    
    public void apply(Point2D point) {
        double[] affine = new double[] {
            transform.getPreAffine().getA(),
            transform.getPreAffine().getE(),
            transform.getPreAffine().getB(),
            transform.getPreAffine().getD(),
            transform.getPreAffine().getC(),
            transform.getPreAffine().getF(),
        };
        definition.apply(point, getCoefficient(), getParameterMap(), affine);
    }
}
