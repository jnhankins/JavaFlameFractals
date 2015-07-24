/**
 * JavaFlameFractals (JFF)
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

package com.jnhankins.jff.flame;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code Transform} provides methods to access and modify parameters
 * contained in {@link Flame} which define transformation function. In the
 * context of flame fractals, a transformation function is function that uses a
 * combination of linear and nonlinear functions to map an input point-color
 * pair to an output point-color pair.
 * <p>
 * The output color of a transformation is determined by mixing the input color
 * and the {@link Transform#getColor() transform's color} by the amount 
 * specified by the transform's {@link Transform#setColorWeight(float) colors weight}:
 * <pre>{@code color_out = color_in + (xform_color - color_in) * xform_color_weight}</pre>
 * The process for transforming a points is as follows:
 * <ol>
 * <li>
 * Apply the first affine transformation, which is referred to as the
 * {@link Transform#getPreAffine() pre-affine} because it is applied before the
 * non-linear variations are applied.
 * </li>
 * <li>
 * Separately apply each of the non-linear functions (referred to as
 * {@link Transform#getVariations() variations}) to the point and sum the
 * results.
 * </li>
 * <li>
 * Apply the second affine transformation, which is referred to as the
 * {@link Transform#getPstAffine() ppost-affine} because it is applied after the
 * non-linear variations are applied.
 * </li>
 * <li>
 * Return the results.
 * </li>
 * </ol>
 * <p>
 * Variation functions are split into two classes: {@code VariationDefinition}
 * and {@code VariationInstance}. A {@code VariationDefinition} defines the
 * algorithms that implement the variation's function, while a
 * {@code VariationInstance} contains a reference to a
 * {@code VariationDefinition} and can contains parameters used by the variation
 * function. Internally {@code Transform} objects maintain a map from
 * {@code VariationDefinition} keys to {@code VariationInstance} values. This
 * ensures that each {@code VariationDefinition} contained within the
 * {@code Transform} is unique. {@code VariationInstnace} objects are 
 * {@link #addVariation(VariationDefinition) added},
 * {@link #getVariation(VariationDefinition) retrieved}, and
 * {@link #removeVariation(VariationDefinition) removed} using their
 * {@code VariationDefinition} key. See {@link VariationDefinition} and
 * {@link VariationInstance} for more information.
 * <p>
 * When {@code Transform} objects are instantiated they are initially identity
 * transformations, meaning that if their function as applied to a point-color
 * pair then that pair would remain unchanged. Specifically, the transform's 
 * color weight is set to 0, the pre and post-affines are both identity affine
 * mappings, and the variation set contains a the Linear (identity) variation.
 * <p>
 * Since a {@code Transform} cannot function properly if it contains no 
 * variations, when the last variation is removed from a {@code Transform} a 
 * Linear (identity) variation is automatically inserted into the 
 * {@code Transform} so that it will not enter an illegal state. It is helpful
 * to mindful of to avoid confusion while modifying a transform's variations.
 * For instance, if you attempt to remove the Linear variation then add a Swirl
 * variation, the transform will have both a Linear and s Swirl variation. To
 * rectify the problem, first add the Swirl variation then remove the Linear
 * variation.
 * <p>
 * To add a new {@code Transform} to a {@code Flame} use
 * {@link Flame#addTransform()}. To remove a {@code Transform} from a
 * {@code Flame} use {@link Flame#removeTransform(Transform)} or
 * {@link Flame#removeTransform(int)}. {@link Flame#getTransform(int) Accessing}
 * a specific {@code Transform} is done so by index. To find out how many
 * {@code Transform} objects a {@code Flame} contains, use
 * {@link Flame#getNumTransforms()}.
 * <p>
 * Each {@code Flame} contains a special {@code Transform} called the "final
 * transform". The final transform is applied to every point-color pair just
 * before it is passed through the {@link FlameView view affine} and plotted on
 * the image. The final transform cannot be removed, and attempting to do so
 * will result in an error. The final transform is stored at index 0 and can be
 * accessed via {@link Flame#getTransformFinal()}.
 * <p>
 * Each iteration of the flame fractal algorithm (see
 * <a href="http://flam3.com/flame.pdf">
 * "The Fractal Flame Algorithm" by Scott Draves (pdf)</a>) requires that one of
 * the flame's transforms is selected using random weighted selection, aka
 * <a href="https://en.wikipedia.org/wiki/Fitness_proportionate_selection">
 * roulette-wheel selection (wiki)</a>. The more often a transform is picked
 * during this process the more influence it will have on the overall fractal
 * flame image. The value that determines this is the transform's
 * {@link #setWeight(float) weight}. The greater the transform's weight relative
 * to the weight of the other transforms, the more likely that the transform
 * will be picked, and the more influence it will have.
 * 
 * @see Flame
 * @see TransformAffine
 * @see TransformColor
 * @see VariationDefinition
 * @see VariationInstance
 * 
 * @author Jeremiah N. Hankins
 */
public class Transform {
    protected final Flame flame;
    protected final int index;
    
    private TransformColor color;
    private TransformAffine preAffine;
    private TransformAffine pstAffine;
    private Map<VariationDefinition, VariationInstance> variations; 
    
    Transform(Flame flame, int index) {
        this.flame = flame;
        this.index = index;
    }
    
    /**
     * Sets the selection probability weight.
     * 
     * @param weight the weight
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if weight is not in the range [0,inf)
     */
    public void setWeight(float weight) {
        if (index == 0)
            throw new UnsupportedOperationException("cannot set weight on final transform");
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        if (!(0<=weight && weight<Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("weight is not in the range [0,inf): "+weight);
        flame.xformWeight[index] = weight;
    }
    
    /**
     * Sets the color weight. The color weight is a value in the range [0,1] 
     * that determines how much this transform's color's influences the color
     * of the next dot plotted to the histogram.
     * 
     * @param colorWeight the colorWeight
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     * @throws IllegalArgumentException if colorWeight is not in the range [0,1]
     */
    public void setColorWeight(float colorWeight) {
        if (!isValid())
            throw new IllegalStateException("this transform is no longer valid.");
        if (!(0<=colorWeight && colorWeight<=1))
            throw new IllegalArgumentException("colorWeight is not in the range [0,1]: "+colorWeight);
        flame.xformCmixes[index] = colorWeight;
    }
    
    /**
     * Returns the selection probability weight.
     * 
     * @return the selection probability weight
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getWeight() {
        if (!isValid())
            throw new IllegalStateException("this transform is no longer valid.");
        return flame.xformWeight[index];
    }
    
    /**
     * Returns the color weight. The color weight is a value in the range [0,1] 
     * that determines how much this transform's color's influences the color
     * of the next dot plotted to the histogram.
     * 
     * @return the color weight
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public float getColorWeight() {
        if (!isValid())
            throw new IllegalStateException("this transform is no longer valid.");
        return flame.xformCmixes[index];
    }
    
    /**
     * Returns the {@link TransformColor}.
     * 
     * @return the {@link TransformColor}
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public TransformColor getColor() {
        if (!isValid())
            throw new IllegalStateException("this transform is no longer valid.");
        if (color == null)
            color = new TransformColor(this);
        return color;
    }
    
    /**
     * Returns the pre-variations {@link TransformAffine}.
     * 
     * @return the pre-variations {@link TransformAffine}
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public TransformAffine getPreAffine() {
        if (!isValid())
            throw new IllegalStateException("this transform is no longer valid.");
        if (preAffine == null)
            preAffine = new TransformAffine(this, true);
        return preAffine;
    }
    
    /**
     * Returns the post-variations {@link TransformAffine}.
     * 
     * @return the post-variations {@link TransformAffine}
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public TransformAffine getPstAffine() {
        if (!isValid())
            throw new IllegalStateException("this transform is no longer valid.");
        if (pstAffine == null)
            pstAffine = new TransformAffine(this, false);
        return pstAffine;
    }
    
    /**
     * Adds the specified {@link VariationDefinition} to this {@code Transfrom},
     * if it is not already present, and returns the {@link VariationInstance} 
     * associated with it.
     * 
     * @param definition the variation to add
     * @return the {@code VariationInstance} associated with the variation
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public VariationInstance addVariation(VariationDefinition definition) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        // If this transform already contrains the definition, then return it
        VariationInstance instance = getVariation(definition);
        if (instance != null)
            return instance;
        // Add the variation to the flame
        flame.addVariation(definition);
        // Get the index for the variation
        int varIndex = flame.numVariations*index+flame.getVariationOffset(definition);
        // Set the hasVariation flag
        flame.xformHasVariation[varIndex] = true;
        // Create a VariationInstance object for the variation
        instance = new VariationInstance(this, definition);
        // Set the variation coeffieicnt
        instance.setCoefficient(1.0f);
        // Set the default parameters
        instance.setDefaultParameters();
        // Allocate the variations map if needed
        if (variations == null)
            variations = new TreeMap();
        // Add the instance to the map
        variations.put(definition, instance);
        // Return the instance
        return instance;
    }
    
    /**
     * Removes the specified {@link VariationDefinition} from this 
     * {@code Transform} if this {@code Transform} contains the specified
     * {@link VariationDefinition}. Returns {@code true} if a variation was 
     * removed.
     * 
     * @param definition the variation to remove
     * @return {@code true} if a variation was removed
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public boolean removeVariation(VariationDefinition definition) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        // Return false if this flame doesn't have the variation
        if (definition == null || !flame.variationSet.contains(definition))
            return false;
        // Get the index for the variation
        int varIndex = flame.numVariations*index+flame.getVariationOffset(definition);
        // Return false if this transform doesn't have the variation
        if (!flame.xformHasVariation[varIndex])
            return false;
        // Unset the hasVariation flag
        flame.xformHasVariation[varIndex] = false;
        // Set the variation coefficient to 0
        flame.xformVariations[varIndex] = 0.0f;
        // Ensure the variation is not in the variations map
        if (variations != null)
            variations.remove(definition);
        // Signal to the flame that a variaton has been removed
        flame.removeVariation(definition);
        // We removed the variation, return true 
       return true;
    }
        
    /**
     * Returns the {@link VariationInstance} associated with the specified 
     * {@link VariationDefinition} in this {@code Transform} or {@code null} 
     * if there is no such {@code VariationInstance}.
     * 
     * @param definition the variation
     * @return the {@code VariationInstance} associated with the variation
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public VariationInstance getVariation(VariationDefinition definition) {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        // Return null if this flame doen't have the variation
        if (definition == null || !flame.variationSet.contains(definition))
            return null;
        // Get the index for the variation
        int varIndex = flame.numVariations*index+flame.getVariationOffset(definition);
        // Return null if this transform doesn't have the variation
        if (!flame.xformHasVariation[varIndex])
            return null;
        // If a VariationInstance object exists for the variation in the
        // variation map, then return it
        if (variations != null) {
            VariationInstance instance = variations.get(definition);
            if (instance != null) {
                return instance;
            }
        }
        // Create a VariationInstance object for the variation
        VariationInstance instance = new VariationInstance(this, definition);
        // Allocate the variations map if needed
        if (variations == null)
            variations = new TreeMap();
        // Add the instance to the map
        variations.put(definition, instance);
        // Return the instance
        return instance;
    }
    
    /**
     * Returns an unmodifiable {@code Collection} of the 
     * {@link VariationInstance}s in this {@code Transform}.
     * 
     * @return the variations
     * @throws IllegalStateException if the underlying data structure backing this object no longer exists
     */
    public Collection<VariationInstance> getVariations() {
        if (!isValid())
            throw new IllegalStateException("the underlying data structure backing this object no longer exists");
        // Allocate the variations map if needed
        if (variations == null) {
            variations = new TreeMap();
        } else {
            removeInvalidVariations();
        }
        // Keep track of the variation index
        int varIndex = flame.numVariations*index;
        // For every variation in the flame
        for (VariationDefinition definition: flame.variationSet) {
            // If this transform has the variation but the variation map does
            // not contain an instance for the variation, create the variation
            // instance and add it to the map
            if (flame.xformHasVariation[varIndex] && !variations.containsKey(definition))
                variations.put(definition, new VariationInstance(this, definition));
            // Increment the variation index
            varIndex++;
        }
        // Return an unmodifiable collection of the variation instances
        return Collections.unmodifiableCollection(variations.values());
    }
    
    /**
     * Returns {@code true} this object is still valid and its methods can be 
     * called without throwing an {@code IllegalStateException}.
     *
     * @return {@code true} if this {@code TransformAffine} is still valid
     */
    public boolean isValid() {
        return index < flame.numTransforms;
    }
    
    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object
     * @throws IllegalStateException if this {@code Transform} is no longer valid
     */
    @Override
    public String toString() {
        if (!isValid())
            throw new IllegalStateException("This transform is no longer valid.");
        StringBuilder builder = new StringBuilder();
        builder.append("Transform(");
        builder.append("Weight(").append(getWeight()).append("), ");
        builder.append(getColor()).append(", ");
        builder.append("Pre").append(getPreAffine()).append(", ");
        builder.append("Pst").append(getPstAffine());
        for (VariationInstance variation: getVariations())
            builder.append(", ").append(variation);
        builder.append(")");
        return builder.toString();
    }
    
    protected void removeInvalidVariations() {
        Iterator<VariationInstance> it = variations.values().iterator();
        while (it.hasNext())
            if (!it.next().isValid())
                it.remove();
    }
    
    /**
     * Applies this transformation to the given point.
     * 
     * @param point the point to transform
     * @param useVariations if {@code true} the variations functions will be applied
     * @param usePostAffine if {@code true} the post-variations affine will be applied
     */
    public void apply(Point2D point, boolean useVariations, boolean usePostAffine) {
        // PreAffine
        preAffine.apply(point);
        // Variations
        if (useVariations) {
            Point2D sum = new Point2D();
            Point2D pt = new Point2D();
            for (VariationInstance variation : getVariations()) {
                pt.set(point);
                variation.apply(pt);
                sum.x += pt.x;
                sum.y += pt.y;
            }
            point.set(sum);
        }
        // PstAffine
        if (usePostAffine) {
            pstAffine.apply(point);
        }
    }
}
