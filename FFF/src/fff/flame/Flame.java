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

import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.nio.FloatBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@code Flame} maintains protected vectorized arrays of data that describe a
 * flame fractal and provides methods for acessing and manipulating that data
 * from an object oriented perspective.
 * <p>
 * For basic information about flame fractals see 
 * <a href=https://en.wikipedia.org/wiki/Fractal_flame>https://en.wikipedia.org/wiki/Fractal_flame</a>.
 * For detailed information about the structure of flame fractals and the 
 * algorithm used to generate their images see <a href="http://flam3.com/flame.pdf">
 * "The Fractal Flame Algorithm" by Scott Draves (pdf)</a>
 * <p>
 * In brief, Flames are composed of a set of Transforms. Each Transform has a
 * weight, a weighted color, two linear affine transforms, and non-linear 
 * "variation" transforms. Each flame also has a special Transform known as a 
 * "final transform". Furthermore the Flame specifies the background color,
 * some parameters that affect the way that colors appear the output image 
 * ("colorization"), and also provides parameters that specify which portion
 * of the flame image should be rendered ("view").
 * <p>
 * A Transform is fundamentally a function takes a color and a point as input
 * and returns a color and a point as output. The output color is determined by
 * mixing the Transform's color with the input color by an amount proportional
 * to the Transform's color's weight. The output point is arrived at through the
 * following sequence of steps: 1) Apply the the first linear affine function
 * ("pre-affine") to the point. 2) Apply each of the Transform's non-linear
 * functions ("variations") to the point. 3) Apply the the second linear affine
 * function ("pst-affine") to the point. 4) Return the result. 
 * <p>
 * The nested structure of Fractal flames naturally lends itself to object
 * oriented programming. It would be reasonable to create a Flame object that
 * contains a set of Transform objects which each contain their own private data
 * and public methods for transforming point-color pairs. This is <b>not</b>
 * how these classes are implemented.
 * <p>
 * <i>All of the data needed to render a Flame is contained within vectorized 
 * arrays in the Flame class.</i> 
 * <p>
 * This library was written with the the intent to utilize Java and OpenCL as a
 * platform to generate long sequences of images to produce video. Data is most
 * efficiently passed into OpenCL kernels in a vectorized format. Some programs
 * that use OpenCL chose to keep their data conveniently stored in an object
 * oriented data structure program-side and vectorize the data just before
 * passing it into OpenCL. For a single high-resolution image where the time
 * spent in the render kernel is much greater than the time spent vectorize the
 * kernel's argument, the just in time vectorization approach would be fine.
 * However, for a large volume of lower resolution images, the time spent on the
 * vectorization process becomes non-negligible.
 * <p>
 * Furthermore, the primary method for generating flame fractal
 * movies is to create two fractals, then interpolate between the two to create 
 * a sequence of images depicting the first fractal smoothly transforming into
 * the second. Using an OOP approach for interpolation would require traversing
 * the tree-like data structure for both Flame objects to generate a the
 * interpolated Flame's data tree. Pre-vectorization allows this step to be
 * optimized into simple for-loops.
 * <p>
 * One potential downside of pre-vectorization is that it shifts the workload
 * from once just before data is loaded into the kernel to every single time the
 * flame is modified. However, modifications to the flames are generally caused
 * by end users who are unlikely to notice the sub millisecond increase in time 
 * spent in methods that modify the flame. On the other hand, they are likely to
 * notice the extra percentage increase in time the library spends interpolating
 * and vectorizing when rendering a sequence of many flames.
 * <p>
 * Another obvious potential downside of pre-vectorization is that all of the
 * data is compressed into horrendously difficult to work with vectors. However,
 * the vector arrays contained within Flame are made private. To access the data
 * one can use Flame's accessor methods to aquire instance of objects that
 * provide views of the Flame's data. For instance, one can aquire a 
 * {@link Transform} object from {@link #getTransform(int)}, then aquire a
 * {@link TransformAffine} object from {@link Transform#getPreAffine()}, etc...
 * These objects do not contain data themselves; they merely provide views of
 * the data contained within their parent {@code Flame}. As such, they cannot
 * be instantiated independently. <i>The hope is that this overall strategy
 * combines transparent pre-vectorization with the convenience of the appearance
 * of an object oriented data structure.</i>
 * <p>
 * Because these objects are merely view's of data contained within Flames some
 * of these objects have the potential to become invalid if the underlying data
 * within the Flame is deleted. If needed, use the isValid() methods to ensure
 * the objects which provide views of the data remain valid.
 * <p>
 * Note 1: 
 * The arrays backing the vectors within the flame only grow and never
 * shrink, and, when they grow, they only grow to the minimum length required.
 * <p>
 * Note 2: 
 * The object that provide views of a flame's data (e.g. Transform or
 * FlameColoration) use lazy initialization. This is done so that flames whose
 * object accessor methods are never called (e.g. interpolated flames) can be 
 * initialized more quickly
 * <p>
 * Note 3: 
 * The array that contains the {@link FlameView} data stores it 
 * internally as: translationX, translationY, rotation (in radians), scale.
 * This format is maintained internally because it is easier to conceptualize 
 * and to interpolate in an intuitive manner. However when it retrieved via
 * {@link #fillBuffers(int, int, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer) fillBuffers()}
 * it is converted into an equivalent affine transformation for speed of
 * computation. Similarly, internally transform weights are stored 
 * independently, but is converted into a series of partial sums for speed of
 * computation using roulette-wheel selection when it is retrieved via
 * {@link #fillBuffers(int, int, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer) fillBuffers()}.
 * <p>
 * Note 4:
 * If you want direct access to the vectorized arrays to avoid invoking
 * {@link #fillBuffers(int, int, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer, java.nio.FloatBuffer) fillBuffers()},
 * then you only need to extend the {@code Flame} class to access its protected
 * members. However, beware of side effects. For example, if a variation that
 * takes parameters is present in only one of the flame's transform, and that
 * transform is removed. The program needs to update numTransforms,
 * numVariations, numParameters, xformWeights, xformCmixes, xformColors,
 * xformAffine, xformHasVariations, and xformParameters. To make matters worse,
 * the pertinent data in the xformVariations and xformParameters is interleaved,
 * so N non-consecutive subsequences within both arrays must be removed where N
 * is the number of transforms in the flame, then the arrays must be compacted.
 * Off-by-one oh my...
 * <p>
 * Note 5: 
 * Flame implements Serializable, though this has not been tested
 * robustly. FlameRendererOpenCL uses {@link Object#equals(Object obj)} to test
 * VariationDefinition instances for equality. This is supposed to decrease the
 * time spent recompiling the kernel in the event that the two flames use the
 * same set of variations. I <i>think</i> that if Flames are deserialized
 * independently that the VariationDefinition instances they contain will fail
 * the {@code equals()} test, though the data they contain will be identical. I
 * could write a Comparator, but it would involves string compares and speed
 * preferred over serialization support. Also of note, references to objects
 * that provide views of the data (e.g. Transform) are marked as transient.
 * <pre>Example Usage:
 * {@code
 * // Create a Sierpinski triangle flame object
 * Flame flame = Flame.newSierpinski();
 * // Print a string representation of the flame
 * System.out.println(flame);
 * // Get the final transform 
 * Transform xform = flame.getTransform(0);
 * // Set the final transform's color to red
 * xform.getColor().set(1.0f, 0.0f, 0.0f); // red
 * // Set the final transform's color weight to 50%
 * xform.setColorWeight(0.5f);
 * // Add a new transform (default's to an idenity transform)
 * xform = flame.addTransform();
 * // Set the new transform's weight 
 * xform.setWeight(0.5f);
 * // Print a string representation of the modified flame
 * System.out.println(flame);
 * // Create a second flame
 * Flame flame2 = Flame.newSierpinski();
 * // Create a flame half way between the first flame and the second
 * Flame flame3 = Flame.lerp(flame, flame2, 0.5f, null);
 * // Print a string representation of the interpolated flame
 * System.out.println(flame3);
 * }</pre>
 * 
 * @see FlameBackground
 * @see FlameColoration
 * @see FlameView
 * @see Transform
 * @see TransformAffine
 * @see TransformColor
 * @see VariationDefinition
 * @see VariationInstance
 * 
 * @author Jeremiah N. Hankins
 */
public class Flame implements Serializable {
    protected int numTransforms;
    protected int numVariations;
    protected int numParameters;
    
    protected float[] xformWeight;
    protected float[] xformCmixes;
    protected float[] xformColors;
    protected float[] xformAffine;
    
    protected boolean[] xformHasVariation;
    protected float[] xformVariations;
    protected float[] xformParameters;
    
    protected float[] flameViewAffine;
    protected float[] flameColoration;
    protected float[] flameBackground;
    
    private transient FlameView view;
    private transient FlameColoration coloration;
    private transient FlameBackground background;
    private transient Transform[] transforms;
    
    protected final TreeSet<VariationDefinition> variationSet;
    
    public Flame() {
        numTransforms = 0;
        numVariations = 0;
        numParameters = 0;
        
        xformWeight = new float[0];
        xformCmixes = new float[0];
        xformColors = new float[0];
        xformAffine = new float[0];

        xformHasVariation = new boolean[0];
        xformVariations = new float[0];
        xformParameters = new float[0];

        flameViewAffine = new float[4];
        flameColoration = new float[3];
        flameBackground = new float[4];
        
        view = null;
        coloration = null;
        background = null;
        transforms = new Transform[0];
        
        variationSet = new TreeSet();
    }
    
    /**
     * Constructs a copy of the given {@code Flame}.
     * 
     * @param flame the {@code Flame} to copy
     * @throws IllegalArgumentException if {@code Flame} is null
     */
    public Flame(Flame flame) {
        if (flame == null)
            throw new IllegalArgumentException("flame is null");
        
        numTransforms = flame.numTransforms;
        numVariations = flame.numVariations;
        numParameters = flame.numParameters;
        
        xformWeight = Arrays.copyOf(flame.xformWeight, numTransforms);
        xformCmixes = Arrays.copyOf(flame.xformCmixes, numTransforms);
        xformColors = Arrays.copyOf(flame.xformColors, numTransforms*4);
        xformAffine = Arrays.copyOf(flame.xformAffine, numTransforms*12);
        
        xformHasVariation = Arrays.copyOf(flame.xformHasVariation, numTransforms*numVariations);
        xformVariations = Arrays.copyOf(flame.xformVariations, numTransforms*numVariations);
        xformParameters = Arrays.copyOf(flame.xformParameters, numTransforms*numParameters);

        flameViewAffine = Arrays.copyOf(flame.flameViewAffine, flame.flameViewAffine.length);
        flameColoration = Arrays.copyOf(flame.flameColoration, flame.flameColoration.length);
        flameBackground = Arrays.copyOf(flame.flameBackground, flame.flameBackground.length);
        
        view = null;
        coloration = null;
        background = null;
        transforms =  new Transform[numTransforms];
        
        variationSet = new TreeSet(flame.variationSet);
    }
    
    /**
     * Copies the data from the specified {@code Falme} into this {@code Flame}
     * object.
     * 
     * @param flame the {@code Flame} to copy
     * @throws IllegalArgumentException if {@code Flame} is null
     */
    public void set(Flame flame) {
        if (flame == null)
            throw new IllegalArgumentException("flame is null");
        
        numTransforms = flame.numTransforms;
        numVariations = flame.numVariations;
        numParameters = flame.numParameters;
        
        if (xformWeight.length < numTransforms) {
            xformWeight = Arrays.copyOf(flame.xformWeight, numTransforms);
            xformCmixes = Arrays.copyOf(flame.xformCmixes, numTransforms);
            xformColors = Arrays.copyOf(flame.xformColors, numTransforms*4);
            xformAffine = Arrays.copyOf(flame.xformAffine, numTransforms*12);
            transforms = Arrays.copyOf(transforms, numTransforms);
        } else {
            System.arraycopy(flame.xformWeight, 0, xformWeight, 0, numTransforms);
            System.arraycopy(flame.xformCmixes, 0, xformCmixes, 0, numTransforms);
            System.arraycopy(flame.xformColors, 0, xformColors, 0, numTransforms*4);
            System.arraycopy(flame.xformAffine, 0, xformAffine, 0, numTransforms*12);
        }
        if (xformVariations.length < numTransforms*numVariations) {
            xformHasVariation = Arrays.copyOf(flame.xformHasVariation, numTransforms*numVariations);
            xformVariations = Arrays.copyOf(flame.xformVariations, numTransforms*numVariations);
        } else {
            System.arraycopy(flame.xformHasVariation, 0, xformHasVariation, 0, numTransforms*numVariations);
            System.arraycopy(flame.xformVariations, 0, xformVariations, 0, numTransforms*numVariations);
        }
        if (xformParameters.length < numTransforms*numParameters) {
            xformParameters = Arrays.copyOf(flame.xformParameters, numTransforms*numParameters);
        } else {
            System.arraycopy(flame.xformParameters, 0, xformParameters, 0, numTransforms*numParameters);
        }
        
        System.arraycopy(flame.flameViewAffine, 0, flameViewAffine, 0, flameViewAffine.length);
        System.arraycopy(flame.flameColoration, 0, flameColoration, 0, flameColoration.length);
        System.arraycopy(flame.flameBackground, 0, flameBackground, 0, flameBackground.length);
        
        variationSet.clear();
        variationSet.addAll(flame.variationSet);
    }
    
    /**
     * Returns the number of {@link Transform}s contained in this {@code Flame}.
     * 
     * @return the number of transforms
     */
    public int getNumTransforms() {
        return numTransforms;
    }
    
    /**
     * Returns the number of unique {@link VariationDefinition}s referenced in
     * all of the {@link Transform}s contained in this {@code Flame}.
     * 
     * @return the number of variations
     */
    public int getNumVariations() {
        return numVariations;
    }
    
    /**
     * Returns the total number of parameters in all of the unique
     * {@link VariationDefinition}s referenced in all of the {@link Transform}s
     * contained in this {@code Flame}.
     *
     * @return the number of parameters
     */
    public int getNumParameters() {
        return numParameters;
    }
    
    /**
     * Returns the {@link FlameView}.
     * 
     * @return the view
     */
    public FlameView getView() {
        if (view == null)
            view = new FlameView(this);
        return view;
    }
    
    /**
     * Returns the {@link FlameColoration}.
     * 
     * @return the coloration
     */
    public FlameColoration getColoration() {
        if (coloration == null)
            coloration = new FlameColoration(this);
        return coloration;
    }
    
    /**
     * Returns the {@link FlameBackground} color.
     * 
     * @return the background color
     */
    public FlameBackground getBackground() {
        if (background == null)
            background = new FlameBackground(this);
        return background;
    }
    
    /**
     * Returns an unmodifiable {@code Set} of all of the unique 
     * {@link VariationDefinition}s referenced in all of the {@link Transform}s
     * contained in this {@code Flame}.
     * 
     * @return the variation set
     */
    public Set<VariationDefinition> getVariationSet() {
        return Collections.unmodifiableSet(variationSet);
    }
    
    /**
     * Returns the {@link Transform} with the given index.
     * 
     * @param index the index of the {@code Transform} to retrieve
     * @return the {@code Transform} at the given index
     * @throws IllegalArgumentException if {@code index} is out of range
     */
    public Transform getTransform(int index) {
        if (!(0 <= index && index < numTransforms))
            throw new IllegalArgumentException("index is out of range [0,"+(numTransforms-1)+"]: "+index);
        if (transforms == null)
            transforms = new Transform[numTransforms];
        if (transforms[index] == null)
            transforms[index] = new Transform(this, index);
        return transforms[index];
    }
    
    /**
     * Returns the 'final' {@link Transform}. The final transform is applied at
     * the end of each iteration of the Flame Fractal Algorithm. This function
     * is equivalent to {@code getTransform(0)}.
     * 
     * @return the final {@code Transform}
     */
    public Transform getTransformFinal() {
        return getTransform(0);
    }
    
    /**
     * Adds a new {@link Transform} to this {@code Flame} and returns it.
     * 
     * @return the new {@code Transform}
     */
    public Transform addTransform() {
        // Keep track of the index of the new transform
        int index = numTransforms;
        // Increment the number of transforms
        numTransforms++;
        // Allocate space for the xform object, weight, cmixes, color, & affine
        if (transforms.length < numTransforms) {
            xformWeight = Arrays.copyOf(xformWeight, numTransforms);
            xformCmixes = Arrays.copyOf(xformCmixes, numTransforms);
            xformColors = Arrays.copyOf(xformColors, numTransforms*4);
            xformAffine = Arrays.copyOf(xformAffine, numTransforms*12);
            transforms = Arrays.copyOf(transforms, numTransforms);
            transforms[index] = new Transform(this, index);
        }
        // Allocate space for the variations
        if (xformVariations.length < numTransforms*numVariations) {
            xformHasVariation = Arrays.copyOf(xformHasVariation, numTransforms*numVariations);
            xformVariations = Arrays.copyOf(xformVariations, numTransforms*numVariations);
        }
        // Allocate space for the parameters
        if (xformParameters.length < numTransforms*numParameters)
            xformParameters = Arrays.copyOf(xformParameters, numTransforms*numParameters);
        // Get a refrence to the xform object
        Transform xform = transforms[index];
        // Fill the transforms with default values
        xformWeight[index] = (index == 0)? 0.0f : 1.0f;
        xformCmixes[index] = 0.5f;
        xformColors[index*4+0] = 1.0f;
        xformColors[index*4+1] = 1.0f;
        xformColors[index*4+2] = 1.0f;
        xformColors[index*4+3] = 1.0f;
        xformAffine[index*12+0] = 1.0f;
        xformAffine[index*12+1] = 1.0f;
        xformAffine[index*12+2] = 0.0f;
        xformAffine[index*12+3] = 0.0f;
        xformAffine[index*12+4] = 0.0f;
        xformAffine[index*12+5] = 0.0f;
        xformAffine[index*12+6] = 1.0f;
        xformAffine[index*12+7] = 1.0f;
        xformAffine[index*12+8] = 0.0f;
        xformAffine[index*12+9] = 0.0f;
        xformAffine[index*12+10] = 0.0f;
        xformAffine[index*12+11] = 0.0f;
        Arrays.fill(xformHasVariation, index*numVariations, numTransforms*numVariations, false);
        xform.addVariation(VariationDefinition.LINEAR);
        // Return a refrence to the transform
        return xform;
    }

    /**
     * Removes the {@link Transform} at the specified index. Returns 
     * {@code true} if a {@code Transform} that was removed. The 
     * {@code Transform} at index 0, i.e. the 'final' transform, cannot be 
     * removed. 
     * 
     * @param index the index of the {@code Transform} to remove
     * @return {@code true} if a {@code Transform} was removed
     */
    public boolean removeTransform(int index) {
        // If the index is invalid, return false
        if (!(1 <= index && index < numTransforms))
            return false;
        // Get the variations in the transform being removed
        ArrayList<VariationDefinition> variations = new ArrayList();
        int varIndex = numVariations*index;
        for (VariationDefinition definition : variationSet)
            if (xformHasVariation[varIndex++])
                variations.add(definition);
        // If the transform that's being removed is not at the last index,
        // then we'll have to do some array copies to ensure the data is stored
        // contiguously
        if (index < numTransforms-1) {
            int src = index+1;
            int dst = index;
            int num = numTransforms-index-1;
            System.arraycopy(xformWeight, src, xformWeight, dst, num);
            System.arraycopy(xformCmixes, src, xformCmixes, dst, num);
            System.arraycopy(xformColors, src*4, xformColors, dst*4, num*4);
            System.arraycopy(xformAffine, src*12, xformAffine, dst*12, num*12);
            System.arraycopy(xformHasVariation, src*numVariations, xformHasVariation, dst*numVariations, num*numVariations);
            System.arraycopy(xformVariations, src*numVariations, xformVariations, dst*numVariations, num*numVariations);
            System.arraycopy(xformParameters, src*numParameters, xformParameters, dst*numParameters, num*numParameters);
        }
        // Decrement the transform counter
        numTransforms--;
        // Remove the variations that were in the transform being removed
        for (VariationDefinition variation: variations)
            removeVariation(variation);
        // A transform was removed, return true
        return true;
    }
    
    /**
     * Removes the specified {@link Transform} if this {@code Flame} contains
     * the {@link Transform} and the {@code Transform} is not the 'final'
     * transform, i.e. the {@code Transform} at index 0.
     * 
     * @param transform the {@code Transform} to remove
     * @return {@code true} if a {@code Transform} was removed 
     */
    public boolean removeTransform(Transform transform) {
        if (transform != null)
            for (int i=1; i<numTransforms; i++)
                if (transforms[i] == transform)
                    return removeTransform(i);
        return false;
    }
    
    protected void addVariation(VariationDefinition definition) {
        // If this flame does not already contain the specified variation
        if (!variationSet.contains(definition)) {
            // Add the variation
            variationSet.add(definition);
            // Get the number of parameters in the new variation
            int varParameters = definition.getParameters().size();
            // Update the variation and parameter counters
            numParameters += varParameters;
            numVariations += 1;
            // Find the index of the new variation
            int varIndex = getVariationOffset(definition);
            // Find the index of the first parameter
            int parIndex = getParameterOffset(definition);
            // Ensure the variations and parameters array will be large enough
            boolean[] hasVariations = xformHasVariation.length < numTransforms*numVariations?
                new boolean[numTransforms*numVariations] : xformHasVariation;
            float[] variations = xformVariations.length < numTransforms*numVariations?
                new float[numTransforms*numVariations] : xformVariations;
            float[] parameters = xformParameters.length < numTransforms*numParameters?
                new float[numTransforms*numParameters] : xformParameters;
            // Variations
            for (int i=numTransforms-1; i>=0; i--) {
                int srcA = i*(numVariations-1);
                int dstA = i*numVariations;
                int numA = varIndex;
                int dstB = dstA+varIndex;
                int srcC = srcA+varIndex;
                int dstC = dstA+varIndex+1;
                int numC = numVariations-varIndex-1;
                System.arraycopy(xformHasVariation, srcC, hasVariations, dstC, numC);
                System.arraycopy(xformHasVariation, srcA, hasVariations, dstA, numA);
                hasVariations[dstB] = false;
                System.arraycopy(xformVariations, srcC, variations, dstC, numC);
                System.arraycopy(xformVariations, srcA, variations, dstA, numA);
                variations[dstB] = 0.0f;
            }
            // Parameters
            if (varParameters > 0) {
                for (int i=numTransforms-1; i>=0; i--) {
                    int srcA = i*(numParameters-varParameters);
                    int dstA = i*numParameters;
                    int numA = parIndex;
                    int srcC = srcA+parIndex;
                    int dstC = dstA+parIndex+varParameters;
                    int numC = numParameters-parIndex-varParameters;
                    System.arraycopy(xformParameters, srcC, parameters, dstC, numC);
                    System.arraycopy(xformParameters, srcA, parameters, dstA, numA);
                }
            }
            // Store the results
            xformHasVariation = hasVariations;
            xformVariations = variations;
            xformParameters = parameters;
        }
    }
    
    protected void removeVariation(VariationDefinition definition) {
        if (!variationSet.contains(definition))
            throw new IllegalArgumentException("the specified variation definition is not registered to this flame: "+definition.getName());
        // Find the index of the variation
        int varIndex = getVariationOffset(definition);
        // Find the index of the first parameter
        int parIndex = getParameterOffset(definition);
        // If the specified variation appears in atleast one of the transforms, 
        // then do nothing and return
        for (int i=0; i<numTransforms; i++)
            if (xformHasVariation[i*numVariations+varIndex])
                return;
        // Remove the varation from the map
        variationSet.remove(definition);
        // Variations
        int srcA = 0;
        int dstA = 0;
        int numA = varIndex;
        int srcB = varIndex+1;
        int dstB = varIndex;
        int numB = numVariations-varIndex-1;
        for (int i=0; i<numTransforms; i++) {
            System.arraycopy(xformHasVariation, srcA, xformHasVariation, dstA, numA);
            System.arraycopy(xformHasVariation, srcB, xformHasVariation, dstB, numB);
            System.arraycopy(xformVariations, srcA, xformVariations, dstA, numA);
            System.arraycopy(xformVariations, srcB, xformVariations, dstB, numB);
            srcA += numVariations;
            srcB += numVariations;
            dstA += numVariations-1;
            dstB += numVariations-1;
        }
        // Parameters
        int varParameters = definition.getParameters().size();
        srcA = 0;
        dstA = 0;
        numA = parIndex;
        srcB = parIndex+varParameters;
        dstB = parIndex;
        numB = numParameters-parIndex-varParameters;
        for (int i=0; i<numTransforms; i++) {
            System.arraycopy(xformParameters, srcA, xformParameters, dstA, numA);
            System.arraycopy(xformParameters, srcB, xformParameters, dstB, numB);
            srcA += numParameters;
            srcB += numParameters;
            dstA += numParameters-varParameters;
            dstB += numParameters-varParameters;
        }
        // Update the number of variations and parameters
        numVariations -= 1;
        numParameters -= varParameters;
    }
    
    protected int getVariationOffset(VariationDefinition definition) {
        int variationIndex = 0;
        for (VariationDefinition def: variationSet) {
            if (def == definition)
                return variationIndex;
            variationIndex++;
        }
        throw new IllegalArgumentException("The specified variation definition is not registered to this flame: "+definition.getName());
    }
    
    protected int getParameterOffset(VariationDefinition definition) {
        int parameterIndex = 0;
        for (VariationDefinition def: variationSet) {
            if (def == definition)
                return parameterIndex;
            parameterIndex += def.getParameters().size();
        }
        throw new IllegalArgumentException("The specified variation definition is not registered to this flame: "+definition.getName());
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Flame {\n");
        builder.append("   ").append(getView()).append("\n");
        builder.append("   ").append(getColoration()).append("\n");
        builder.append("   ").append(getBackground()).append("\n");
        for (int i=0; i<numTransforms; i++)
            builder.append("   ").append(getTransform(i)).append("\n");
        builder.append("}");
        return builder.toString();
    }
    
    /**
     * Fills the given {@link FloatBuffer}s with this {@code Flame}'s data.
     * 
     * @param width the width of the image
     * @param height the height of the image
     * @param xformWeightBuffer the transform weight buffer
     * @param xformCmixesBuffer the transform color mix buffer
     * @param xformColorsBuffer the transform color buffer
     * @param xformAffineBuffer the transform affine buffer
     * @param xformVariationsBuffer the transform variation coefficient buffer
     * @param xformParametersBuffer the transform variation parameter buffer
     * @param flameViewAffineBuffer the flame view affine buffer
     * @param flameColorationBuffer the flame coloration buffer
     * @param flameBackgroundBuffer the flame background color buffer
     * @throws NullPointerException if any of the {@code FloatBuffer}s are null
     * @throws BufferOverflowException if any of the {@code FloatBuffer}s have insufficient space
     * @throws ReadOnlyBufferException if any of the {@code FloatBuffer}s are read-only
     */
    public void fillBuffers(int width, int height,
            FloatBuffer xformWeightBuffer,
            FloatBuffer xformCmixesBuffer,
            FloatBuffer xformColorsBuffer,
            FloatBuffer xformAffineBuffer,
            FloatBuffer xformVariationsBuffer,
            FloatBuffer xformParametersBuffer,
            FloatBuffer flameViewAffineBuffer,
            FloatBuffer flameColorationBuffer,
            FloatBuffer flameBackgroundBuffer) {
        // Weights
        float totalWeight = 0;
        for (int i=1; i<numTransforms; i++)
            totalWeight += xformWeight[i];
        float weight = 0;
        for (int i=0; i<numTransforms; i++)
            xformWeightBuffer.put((weight += xformWeight[i])/totalWeight);
        // Xform Color Weights
        xformCmixesBuffer.put(xformCmixes, 0, numTransforms);
        // Xform Colors Indices
        xformColorsBuffer.put(xformColors, 0, numTransforms*4);
        // Xform Affines
        xformAffineBuffer.put(xformAffine, 0, numTransforms*12);
        // Xform Variation Coefficients
        xformVariationsBuffer.put(xformVariations, 0, numTransforms*numVariations);
        // Xform Parameter Values
        xformParametersBuffer.put(xformParameters, 0, numTransforms*numParameters);
        // Flame View Affine
        float cos = (float)(Math.cos(Math.toRadians(flameViewAffine[2]))*flameViewAffine[3]*Math.min(width,height)*0.5);
        float sin = (float)(Math.sin(Math.toRadians(flameViewAffine[2]))*flameViewAffine[3]*Math.min(width,height)*0.5);
        flameViewAffineBuffer.put(+cos); // A
        flameViewAffineBuffer.put(-cos); // E
        flameViewAffineBuffer.put(+sin); // B
        flameViewAffineBuffer.put(+sin); // D
        flameViewAffineBuffer.put(-cos*flameViewAffine[0]-sin*flameViewAffine[1]+width*0.5f);  // C
        flameViewAffineBuffer.put(-sin*flameViewAffine[0]+cos*flameViewAffine[1]+height*0.5f); // F
        // Flame Coloration
        flameColorationBuffer.put(flameColoration);
        // Flame Bacground Color
        flameBackgroundBuffer.put(flameBackground);
    }
    
    /**
     * Constructs a new {@code Flame} resembling a Sierpinksi Gasket.
     * 
     * @return the new Sierpinksi {@code Flame}
     */
    public static Flame newSierpinski() {
        Flame flame = new Flame();
        Transform transform;
        // Final Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // First Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1.0f, 0.0f, 0.0f);
        transform.getPreAffine().set(0.5f, 0, 0.433f, 0.0f, 0.5f, -0.25f);
        // Second Trasnform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(0.0f, 0.0f, 1.0f);
        transform.getPreAffine().set(0.5f, 0,-0.433f, 0.0f, 0.5f, -0.25f);
        // Third Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(0.0f, 1.0f, 0.0f);
        transform.getPreAffine().set(0.5f, 0, 0.0f, 0.0f, 0.5f, 0.5f);
        // View
        flame.getView().set(0, 0, 0, 1);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(1, 0, 0, 0);
        // Return the flame
        return flame;
    }
    
    /** 
     * Constructs and returns a new {@code Flame} object using the given
     * parameters. This method is equivalent to the following code:
     * <pre>{@code
     * newRandomFlame(
     *    minTransforms, maxTransforms, 
     *    minVariations, maxVariations,
     *    0.50f, 1.0f,
     *    0.25f, 0.75f,
     *    0.25f, 1.5f,
     *    0.25f, 1.0f, (float)(Math.PI/4), 
     *    1.0f,
     *    finalTransform, pstAffines, randomParameters, 
     *    variationList)}</pre>
     * 
     * @param minTransforms
     * @param maxTransforms
     * @param minVariations
     * @param maxVariations
     * @param finalTransform
     * @param pstAffines
     * @param randomParameters
     * @param variationList
     * @return a newly constructed [@code Flame} object
     */
    public static Flame newRandomFlame(
            int minTransforms, int maxTransforms, 
            int minVariations, int maxVariations,
            boolean finalTransform, boolean pstAffines, boolean randomParameters,
            List<VariationDefinition> variationList) {
        return newRandomFlame(
                minTransforms, maxTransforms, 
                minVariations, maxVariations,
                0.50f, 1.0f,
                0.25f, 0.75f,
                0.25f, 1.5f,
                0.25f, 1.0f, (float)(Math.PI/4), 
                1.0f,
                finalTransform, pstAffines, randomParameters, 
                variationList);
    }
    
    /**
     * Constructs and returns a new randomly generated {@code Flame} using 
     * the given parameters. The generated {@code Flame} object will have the
     * following properties:<br>
     * <ul>
     * <li>
     * The number of transforms will be in the range 
     * {@code [minTransforms, maxTransforms]}.
     * </li>
     * <li>
     * The number of variations contained in each transform will be in the range 
     * {@code [minVariations, maxVariations]}. The variations used will be taken
     * from the provided list of variation definitions, (@code variationList}.
     * </li>
     * <li>
     * The weight of each transform will be in the range 
     * {@code [minTransformWeight, maxTransformWeight]}.
     * </li>
     * <li>
     * The color weight of each transform will be in the range 
     * {@code [minColorWeight, maxColorWeight]}.
     * </li>
     * <li>
     * The coefficient for each variation will be in the range 
     * {@code [minCoefficient, maxCoefficient]}.
     * </li>
     * <li>
     * If {@code finalTransform} is {@code false}, then the flame will have a 
     * final-transform equivalent to an identity transform, otherwise a
     * final-transform will be randomly generated following the same rules as
     * the other transforms.
     * </li>
     * <li>
     * If {@code pstAffines} is {@code false}, then the flame's 
     * post-variation affine transformations will be identity transformations, 
     * otherwise they will be randomly generated following the same rules as the
     * pre-variation affine transformations.
     * </li>
     * <li>
     * If {@code randomParameters} is {@code false}, then any variations within
     * the flame that are parameterized will used the default parameters, 
     * otherwise random parameter values in the range (-1, 1) will be generated 
     * for all parameterized variations.
     * </li>
     * </ul>
     * Furthermore, affine transformations will be generated using the following
     * steps:
     * <ol>
     * <li>
     * Two 2D-vectors are generated whose lengths are in the range 
     * {@code [minAffineScale, maxAffineScale]} and whose orientations are
     * randomized except in that the minimum angle between the two vectors
     * will not be less than {@code minAffineTheata}.
     * </li>
     * <li>
     * The x and y components of the first vector are used as the affine's a and
     * d components which control the scale and skew of the affine's x-output.
     * The x and y components of the second vector are used as the affine's b 
     * and e components which control the scale and skew of the affine's 
     * y-output.
     * </li>
     * <li>
     * The affine's translation components, c and f, are then generated to be in
     * the range {@code [-maxAffineTranslation, maxAffineTranslation]}.
     * </li>
     * </ol>
     * Any combination of parameters which results in an invalid range will 
     * generate an {@code IllegalArgumentException}.
     * 
     * @param minTransforms
     * @param maxTransforms
     * @param minVariations
     * @param maxVariations
     * @param minTransformWeight
     * @param maxTransformWeight
     * @param minColorWeight
     * @param maxColorWeight
     * @param minCoefficient
     * @param maxCoefficient
     * @param minAffineScale
     * @param maxAffineScale
     * @param minAffineTheata
     * @param maxAffineTranslation
     * @param finalTransform
     * @param pstAffines
     * @param randomParameters
     * @param variationList
     * @return a newly constructed [@code Flame} object
     */
    public static Flame newRandomFlame(
            int minTransforms, int maxTransforms, 
            int minVariations, int maxVariations, 
            float minTransformWeight, float maxTransformWeight,
            float minColorWeight, float maxColorWeight,
            float minCoefficient, float maxCoefficient,
            float minAffineScale, float maxAffineScale, float minAffineTheata, 
            float maxAffineTranslation, 
            boolean finalTransform, boolean pstAffines, boolean randomParameters,
            List<VariationDefinition> variationList) {
        if (!(minTransforms >= 0 && minTransforms <= maxTransforms))
            throw new IllegalArgumentException("!(minTransforms >= 0 && minTransforms <= maxTransforms): minTransforms="+minTransforms+" minTransforms="+maxTransforms);
        if (!(minVariations > 0 && minVariations <= maxVariations))
            throw new IllegalArgumentException("!(minVariations > 0 && minVariations <= maxVariations): minVariations="+minVariations+" maxVariations="+maxVariations);
        if (!(minTransformWeight > 0 && minTransformWeight <= maxTransformWeight))
            throw new IllegalArgumentException("!(minTransformWeight > 0 && minTransformWeight <= maxTransformWeight): minTransformWeight="+minTransformWeight+" maxTransformWeight="+maxTransformWeight);
        if (!(minColorWeight >= 0 && minColorWeight <= maxColorWeight))
            throw new IllegalArgumentException("!(minColorWeight >= 0 && minColorWeight <= maxColorWeight): minColorWeight="+minColorWeight+" maxColorWeight="+maxColorWeight);
        if (!(minCoefficient > 0 && minCoefficient <= maxCoefficient))
            throw new IllegalArgumentException("!(minCoefficient > 0 && minCoefficient <= maxCoefficient): minCoefficient="+minCoefficient+" maxCoefficient="+maxCoefficient);
        if (!(minAffineScale >= 0 && minAffineScale <= maxAffineScale))
            throw new IllegalArgumentException("!(minAffineScale >= 0 && minAffineScale <= maxAffineScale): minAffineScale="+minAffineScale+" maxAffineScale="+maxAffineScale);
        if (!(Float.isFinite(minAffineTheata)))
            throw new IllegalArgumentException("!(Float.isFinite(minAffineTheata)): minAffineTheata="+minAffineTheata);
        if (!(Float.isFinite(maxAffineTranslation)))
            throw new IllegalArgumentException("!(Float.isFinite(maxAffineTranslation)): maxAffineTranslation="+maxAffineTranslation);
        if (maxVariations > 0 && (variationList == null || variationList.size() < maxVariations))
            throw new IllegalArgumentException("maxVariations > 0 && (variationList == null || variationList.size() < maxVariations): variationList="+variationList+" maxVariations="+maxVariations);
        
        // Create a random number generator
        Random random = new Random();
        
        // Make sure the min and max number of variations is reasonable
        minVariations = Math.min(minVariations, variationList.size());
        maxVariations = Math.min(maxVariations, variationList.size());
        
        // Construct a new flame
        Flame flame = new Flame();
        
        // Determine how many transforms will be in the flame and create them
        int numTransforms = minTransforms + random.nextInt(maxTransforms-minTransforms+1);
        for(int i=0; i<numTransforms+1; i++)
            flame.addTransform();
        
        /// For each transform
        for (int i=0; i<flame.numTransforms; i++) {
            // Only do the final transform if the flag has been set
            if (i == 0 && !finalTransform)
                continue;
            // Get the transform
            Transform xform = flame.getTransform(i);
            // Determine the weight for this transform
            xform.setWeight(random.nextFloat()*(maxTransformWeight-minTransformWeight) + minTransformWeight);
            // Determine the color weight for this transform
            xform.setColorWeight(random.nextFloat()*(maxColorWeight-minColorWeight) + minColorWeight);
            // Determine the color for this transfrom
            xform.getColor().setR(random.nextFloat());
            xform.getColor().setG(random.nextFloat());
            xform.getColor().setB(random.nextFloat());
            // Randomize the pre affine
            double theta = random.nextFloat()*Math.PI*2.0;
            double radius = random.nextFloat()*(maxAffineScale-minAffineScale)+minAffineScale;
            xform.getPreAffine().setA((float)(radius*Math.cos(theta)));
            xform.getPreAffine().setD((float)(radius*Math.sin(theta)));
            theta += (random.nextDouble()*(Math.PI-minAffineTheata*2.0)+minAffineTheata)*Math.signum(random.nextFloat());
            radius = random.nextFloat()*(maxAffineScale-minAffineScale)+minAffineScale;
            xform.getPreAffine().setB((float)(radius*Math.cos(theta)));
            xform.getPreAffine().setE((float)(radius*Math.sin(theta)));
            xform.getPreAffine().setC(random.nextFloat()*maxAffineTranslation*2.0f- maxAffineTranslation);
            xform.getPreAffine().setF(random.nextFloat()*maxAffineTranslation*2.0f- maxAffineTranslation);
            // Randomize the pst affine
            if (pstAffines) {
                theta = random.nextFloat()*Math.PI*2.0;
                radius = random.nextFloat()*(maxAffineScale-minAffineScale)+minAffineScale;
                xform.getPstAffine().setA((float)(radius*Math.cos(theta)));
                xform.getPstAffine().setD((float)(radius*Math.sin(theta)));
                theta += (random.nextDouble()*(Math.PI-minAffineTheata*2.0)+minAffineTheata)*Math.signum(random.nextFloat());
                radius = random.nextFloat()*(maxAffineScale-minAffineScale)+minAffineScale;
                xform.getPstAffine().setB((float)(radius*Math.cos(theta)));
                xform.getPstAffine().setE((float)(radius*Math.sin(theta)));
                xform.getPstAffine().setC(random.nextFloat()*maxAffineTranslation*2.0f- maxAffineTranslation);
                xform.getPstAffine().setF(random.nextFloat()*maxAffineTranslation*2.0f- maxAffineTranslation);
            }
            // Determine how many variations will be in this transform
            int numVariations = minVariations+random.nextInt(maxVariations-minVariations+1);
            // Construct a set of variations that will be added
            Set<VariationDefinition> variations = new HashSet();
            List<VariationDefinition> variationChoices = new ArrayList(variationList);
            for (int j=0; j<numVariations; j++) {
                int varIndex = random.nextInt(variationChoices.size());
                VariationDefinition definition = variationChoices.remove(varIndex);
                variations.add(definition);
            }
            // Add the variations
            for (VariationDefinition definition: variations) {
                VariationInstance instance = xform.addVariation(definition);
                // Determine the variation's coefficient
                instance.setCoefficient(random.nextFloat()*(maxCoefficient-minCoefficient) + minCoefficient);
                // Randomize the parameters
                if (randomParameters) {
                    for (Map.Entry<String,Float> param : instance.getParameterMap().entrySet()) {
                        float value = random.nextFloat()*1.8f - 0.9f;
                        value += Math.signum(value)*0.1f;
                        instance.setParameter(param.getKey(), value);
                    }
                }
            }
            // Remove the lienar variation
            if (!variations.isEmpty() && !variations.contains(VariationDefinition.LINEAR)) {
                xform.removeVariation(VariationDefinition.LINEAR);
            }
        }
        // View
        flame.getView().set(0, 0, 0, 0.5f);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(1, 0, 0, 0);
        
        // Return the flame
        return flame;
    }
    
    /**
     * Interpolates between the first flame, and the second flame, b, by
     * the amount specified by mix. The results will be stored in the third
     * given flame, unless the given reference is null, in which case a new
     * flame object will be constructed to store the results. The returned 
     * flame object contains the result of the linear-interpolation.
     * 
     * @param a the first flame
     * @param b the second flame
     * @param mix the mix amount
     * @param out the flame in which the results are stored, or null
     * @return the interpolated flame
     * 
     * @throws NullPointerException if a or b are null
     */
    public static Flame lerp(Flame a, Flame b, float mix, Flame out) {
        // If the output Flame is null, construct a new Flame
        if (out == null)
            out = new Flame();
        
        // If flame A has fewer transforms thatn flame B, swap A and B, to
        // ensure than B always fewer transforms
        if (a.numTransforms < b.numTransforms) {
            Flame f = a;
            a = b;
            b = f;
            mix = 1.0f-mix;
        }
        
        // The output flame's variation set is the union of A and B's variation
        // sets
        out.variationSet.clear();
        out.variationSet.addAll(a.variationSet);
        out.variationSet.addAll(b.variationSet);
        
        // Deterimine the number of transforms, variations, and parameters in
        // the output flame
        int numTransforms = Math.max(a.numTransforms, b.numTransforms);
        int numVariations = out.variationSet.size();
        int numParameters = 0;
        for (VariationDefinition definition: out.variationSet)
            numParameters += definition.parameters.size();
        out.numTransforms = numTransforms;
        out.numVariations = numVariations;
        out.numParameters = numParameters;
        
        // Ensure the output flame's arrays are large enough
        if (out.transforms.length < numTransforms) {
            out.xformWeight = new float[numTransforms];
            out.xformCmixes = new float[numTransforms];
            out.xformColors = new float[numTransforms*4];
            out.xformAffine = new float[numTransforms*12];
            out.transforms = Arrays.copyOf(out.transforms, numTransforms);
        }
        if (out.xformVariations.length < numTransforms*numVariations) {
            out.xformHasVariation = new boolean[numTransforms*numVariations];
            out.xformVariations = new float[numTransforms*numVariations];
        }
        if (out.xformParameters.length < numTransforms*numParameters) {
            out.xformParameters = new float[numTransforms*numParameters];
        }
        
        // Lerp the weights
        float invMix = 1.0f-mix;
        for (int i=0; i<b.numTransforms; i++)
            out.xformWeight[i] = Flame.lerp(a.xformWeight[i], b.xformWeight[i], mix);
        for (int i=b.numTransforms; i<a.numTransforms; i++)
            out.xformWeight[i] = a.xformWeight[i]*invMix;
        
        // Lerp the color mixes
        for (int i=0; i<b.numTransforms; i++)
            out.xformCmixes[i] = Flame.lerp(a.xformCmixes[i], b.xformCmixes[i], mix);
        System.arraycopy(a.xformCmixes, b.numTransforms, out.xformCmixes, b.numTransforms, a.numTransforms-b.numTransforms);
        
        // Lerp the colors
        for (int i=0; i<b.numTransforms*4; i++) 
            out.xformColors[i] = Flame.lerp(a.xformColors[i], b.xformColors[i], mix);
        System.arraycopy(a.xformColors, b.numTransforms*4, out.xformColors, b.numTransforms*4, (a.numTransforms-b.numTransforms)*4);
        
        // Lerp the affines
        for (int i=0; i<b.numTransforms*12; i++)
            out.xformAffine[i] = Flame.lerp(a.xformAffine[i], b.xformAffine[i], mix);
        System.arraycopy(a.xformAffine, b.numTransforms*12, out.xformAffine, b.numTransforms*12, (a.numTransforms-b.numTransforms)*12);
        
        // Lerp Views
        for (int i=0; i<out.flameViewAffine.length; i++)
            out.flameViewAffine[i] = Flame.lerp(a.flameViewAffine[i], b.flameViewAffine[i], mix);
        
        // Lerp Colorations
        for (int i=0; i<out.flameColoration.length; i++)
            out.flameColoration[i] = Flame.lerp(a.flameColoration[i], b.flameColoration[i], mix);
        
        // Lerp FlameBackground Colors
        for (int i=0; i<out.flameBackground.length; i++)
            out.flameBackground[i] = Flame.lerp(a.flameBackground[i], b.flameBackground[i], mix);
        
        /// Construct arrays to aid with interpolating varations and parameters.
        // Create arrays to map the indexes of the variations and parameters in 
        // the output flame to indexs in the input flames A and B.
        int aVarIndex[] = new int[numVariations];
        int bVarIndex[] = new int[numVariations];
        int aParIndex[] = new int[numParameters];
        int bParIndex[] = new int[numParameters];
        // Build an array of default parameters.
        float defaultPar[] =  new float[numParameters];
        // Keep track of index locations as we fill the arrays
        int aVarI = 0, bVarI = 0, oVarI = 0;
        int aParI = 0, bParI = 0, oParI = 0;
        // Aquire iterators for the variation sets
        Iterator<VariationDefinition> aVarIt = a.variationSet.iterator();
        Iterator<VariationDefinition> bVarIt = b.variationSet.iterator();
        Iterator<VariationDefinition> oVarIt = out.variationSet.iterator();
        // Keep track of which variation is being examined in the input flame's
        // variation sets.
        VariationDefinition aVar = null;
        VariationDefinition bVar = null;
        // For every variation in the output set...
        while (oVarIt.hasNext()) {
            // Get the variation
            VariationDefinition oVar = oVarIt.next();
            // Get the number of parameters in the variation
            int pSize = oVar.parameters.size();
            int p = 0;
            // Store the variation's default values
            for (float value: oVar.parameters.values()) {
                defaultPar[oParI+p] = value;
                p++;
            }
            // Get the next variation from A
            if (aVar == null && aVarIt.hasNext())
                aVar = aVarIt.next();
            // If A's variation is equal to the current variation...
            if (oVar.compareTo(aVar) == 0) {
                // Store the variation's index in A's variation index map
                aVarIndex[oVarI] = aVarI++;
                // Set A's variation to null to signal that we need to get
                // annother variation from a in the next iteraton
                aVar = null;
                // Store the variation's parameter's indexes in A's parameter 
                // index map
                for (int q=0; q<pSize; q++) {
                    aParIndex[oParI+q] = aParI++;
                }
            } 
            // If A's variation is nto equal to the current variation...
            else {
                // Then A does not have the current variation, so set the 
                // indexes to -1
                aVarIndex[oVarI] = -1;
                for (int q=0; q<pSize; q++) {
                    aParIndex[oParI+q] = -1;
                }
            }
            // Get the next variation from B
            if (bVar == null && bVarIt.hasNext())
                bVar = bVarIt.next();
            // If B's variation is equal to the current variation...
            if (oVar.compareTo(bVar) == 0) {
                // Store the variation's index in B's variation index map
                bVarIndex[oVarI] = bVarI++;
                // Set B's variation to null to signal that we need to get
                // annother variation from a in the next iteraton
                bVar = null;
                // Store the variation's parameter's indexes in B's parameter 
                // index map
                for (int q=0; q<pSize; q++) {
                    bParIndex[oParI+q] = bParI++;
                }
            } 
            // If B's variation is not equal to the current variation...
            else {
                // Then B does not have the current variation, so set the 
                // indexes to -1
                bVarIndex[oVarI] = -1;
                for (int q=0; q<pSize; q++) {
                    bParIndex[oParI+q] = -1;
                }
            }
            // Incremept the output variation and parameter indexes
            oVarI += 1;
            oParI += pSize;
        }
        
        // Lerp Transform Variations
        for (int t=0; t<numTransforms; t++) {
            for (int v=0; v<numVariations; v++) {
                out.xformHasVariation[t*numVariations+v] = 
                    (aVarIndex[v]>=0 && a.xformHasVariation[t*a.numVariations + aVarIndex[v]]) ||
                    (bVarIndex[v]>=0 && t<b.numTransforms && b.xformHasVariation[t*b.numVariations + bVarIndex[v]]);
                float aVal = aVarIndex[v]<0? 0 : a.xformVariations[t*a.numVariations + aVarIndex[v]];
                float bVal = b.numTransforms<=t ? aVal : bVarIndex[v]<0? 0 : b.xformVariations[t*b.numVariations + bVarIndex[v]];
                out.xformVariations[t*numVariations+v] = Flame.lerp(aVal, bVal, mix);
            }
        }
        // Lerp Transform Parameters
        for (int t=0; t<numTransforms; t++) {
            for (int v=0; v<numParameters; v++) {
                float aVal = aParIndex[v]<0? defaultPar[v] : a.xformParameters[t*a.numParameters + aParIndex[v]];
                float bVal = b.numTransforms<=t ? aVal : bParIndex[v]<0? defaultPar[v] : b.xformParameters[t*b.numParameters + bParIndex[v]];
                out.xformParameters[t*numParameters+v] = Flame.lerp(aVal, bVal, mix);
            }
        }
        
        // Return the Flame
        return out;
    }
    
    /** 
     * Returns the result of linear interpolation between the first two
     * parameter, {@code a} and {@code b}, by an amount specified by the third 
     * parameter, {@code mix}, using the following equation: 
     * {@code a+(b-a)*mix}.
     * 
     * @param a the first number
     * @param b the second number
     * @param mix the amount to mix the first and second number
     * 
     * @return the interpolated result
     */
    private static float lerp(float a, float b, float mix) {
        return a+(b-a)*mix;
    }
    
    /**
     * Prints the contents of the arrays containing the information backing this
     * {@code Flame}.  Used for debugging.
     */
    public void printState() {
        System.out.println("numTransforms:     "+numTransforms);
        System.out.println("numVariations:     "+numVariations);
        System.out.println("numParameters:     "+numParameters);
        System.out.println("xformWeight:       "+Arrays.toString(xformWeight));
        System.out.println("xformCmixes:       "+Arrays.toString(xformCmixes));
        System.out.println("xformColors:       "+Arrays.toString(xformColors));
        System.out.println("xformAffine:       "+Arrays.toString(xformAffine));
        System.out.println("xformHasVariation: "+Arrays.toString(xformHasVariation));
        System.out.println("xformVariations:   "+Arrays.toString(xformVariations));
        System.out.println("xformParameters:   "+Arrays.toString(xformParameters));
        System.out.println("flameViewAffine:   "+Arrays.toString(flameViewAffine));
        System.out.println("flameColoration:   "+Arrays.toString(flameColoration));
        System.out.println("flameBackground:   "+Arrays.toString(flameBackground));
        System.out.println("variationSet:      "+Arrays.toString(variationSet.toArray()));
    }
}
