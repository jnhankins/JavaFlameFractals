package com.jnhankins.jff.flame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * {@code RandomFlameFactory} constructs and returns a new randomly generated
 * {@code Flame}.
 * <p>
 * The generated {@code Flame} object will have the
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
 * will not be in the range {@code [minAffineTheata, maxAffineTheata]}.
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
 * @author Jeremiah N. Hankins
 */
public class RandomFlameFactory {
    /**
     * The random number generator for this factory.
     */
    private final Random random = new Random();
    /**
     * The minimum number of transforms to use per flame.
     */
    private int minNumTransforms = 1;
    /**
     * The maximum number of transforms to use per flame.
     */
    private int maxNumTransforms = 5;
    /**
     * The minimum number of variations to use per transform.
     */
    private int minNumVariations = 1;
    /**
     * The maximum number of variations to use per transform.
     */
    private int maxNumVariations = 5;
    /**
     * The minimum transform weight.
     */
    private float minTransformWeight = 1;
    /**
     * The maximum transform weight.
     */
    private float maxTransformWeight = 2;
    /**
     * The minimum color weight.
     */
    private float minColorWeight = 0.0f;
    /**
     * The maximum color weight.
     */
    private float maxColorWeight = 1.0f;
    /**
     * The minimum variation coefficient.
     */
    private float minVariationCoefficient = 0.8f;
    /**
     * The maximum variation coefficient.
     */
    private float maxVariationCoefficient = 1.2f;
    /**
     * The minimum value of {@code <affine.a, affine.d>} and
     * {@code <affine.b, affine.e>}.
     */
    private float minAffineScale;
    /**
     * The maximum value of {@code <affine.a, affine.d>} and
     * {@code <affine.b, affine.e>}.
     */
    private float maxAffineScale;
    /**
     * The minimum angle between {@code <affine.a, affine.d>} and
     * {@code <affine.b, affine.e>} in radians.
     */
    private float minAffineTheta;
    /**
     * The maximum angle between {@code <affine.a, affine.d>} and
     * {@code <affine.b, affine.e>} in radians.
     */
    private float maxAffineTheta;
    /**
     * The minimum affine translation from the origin.
     */
    private float minAffineTranslation;
    /**
     * The maximum affine translation from the origin.
     */
    private float maxAffineTranslation;
    /**
     * If {@code true} the final transform will be randomized, otherwise the
     * final transform will be an identity transform.
     */
    private boolean useFinalTransform = false;
    /**
     * If {@code true} post-variation affine's will be randomized, otherwise 
     * the post-variation affine's will be identity transforms.
     */
    private boolean usePstAffines = false;
    /**
     * If {@code true} the values for parameters of parametric variations will 
     * be selected at random from within the range [0,1], otherwise default 
     * parameter vales will be used.
     */
    private boolean useRandomParameters = false;
    /**
     * The list of potential variations.
     */
    private final List<VariationDefinition> variationList = new ArrayList();
    
    /**
     * Returns the minimum number of transforms to use per flame, excluding the
     * final transform.
     * 
     * @return the minimum number of transforms to use per flame
     */
    public int getMinNumTransforms() {
        return minNumTransforms;
    }
    
    /**
     * Returns the maximum number of transforms to use per flame, excluding the
     * final transform.
     * 
     * @return the minimum number of transforms to use per flame
     */
    public int getMaxNumTransforms() {
        return maxNumTransforms;
    }
    
    /**
     * Sets the minimum and maximum number of transforms to use per flame,
     * excluding the final transform.
     * 
     * @param min the minimum number of transforms to use per flame
     * @param max the maximum number of transforms to use per flame
     * @return this {@code RandomFlameFactory}
     * @throws IllegalArgumentException if {@code min} is not positive or {@code min} is less than {@code max}
     */
    public RandomFlameFactory setNumTransforms(int min, int max) {
        if (!(0 < min && min <= max))
            throw new IllegalArgumentException("!(0 < min && min <= max ): min="+min+" max="+max);
        minNumTransforms = min;
        maxNumTransforms = max;
        return this;
    }
    
    /**
     * Returns the minimum number of unique variations to use per transform.
     * 
     * @return the minimum number of unique variations to use per transform
     */
    public int getMinNumVariations() {
        return minNumVariations;
    }
    
    /**
     * Returns the maximum number of unique variations to use per transform.
     * 
     * @return the maximum number of unique variations to use per transform
     */
    public int getMaxNumVariations() {
        return maxNumVariations;
    }
    
    /**
     * Sets the minimum and maximum number of variations to use per transform.
     * 
     * @param min the minimum number of variations to use per transform
     * @param max the maximum number of variations to use per transform
     * @return this {@code RandomFlameFactory}
     * @throws IllegalArgumentException if {@code min} is not positive or {@code min} is less than {@code max}
     */
    public RandomFlameFactory setNumVariations(int min, int max) {
        if (!(0 < min && min <= max))
            throw new IllegalArgumentException("!(0 < min && min <= max ): min="+min+" max="+max);
        minNumVariations = min;
        maxNumVariations = max;
        return this;
    }
    
    /**
     * Returns the minimum transform weight.
     * 
     * @return the minimum transform weight
     */
    public float getMinTransformWeight() {
        return minTransformWeight;
    }
    
    /**
     * Returns the maximum transform weight.
     * 
     * @return the maximum transform weight
     */
    public float getMaxTransformWeight() {
        return maxTransformWeight;
    }
    
    /**
     * Sets the minimum and maximum transform weight.
     * 
     * @param min the minimum transform weight
     * @param max the maximum transform weight
     * @return this {@code RandomFlameFactory}
     * @throws IllegalArgumentException if {@code min} is not positive, {@code min} is less than {@code max}, or {@code max} is not finite
     */
    public RandomFlameFactory setTransformWieght(float min, float max) {
        if (!(0 < min && min <= max && max < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("!(0 < min && min <= max && max < Float.POSITIVE_INFINITY): min="+min+" max="+max);
        minTransformWeight = min;
        maxTransformWeight = max;
        return this;
    }
    
    /**
     * Returns the minimum transform color-weight.
     * 
     * @return the minimum transform color-weight
     */
    public float getMinColorWeight() {
        return minColorWeight;
    }
    
    /**
     * Returns the maximum transform color-weight.
     * 
     * @return the maximum transform color-weight
     */
    public float getMaxColorWeight() {
        return maxColorWeight;
    }
    
    /**
     * Sets the minimum and maximum transform color weight.
     * 
     * @param min the minimum transform color weight
     * @param max the maximum transform color weight
     * @return this {@code RandomFlameFactory}
     * @throws IllegalArgumentException if {@code min} is not positive, {@code min} is less than {@code max}, or {@code max} is greater than {@code 1}
     */
    public RandomFlameFactory setColorWieght(float min, float max) {
        if (!(0 <= min && min <= max && max <= 1))
            throw new IllegalArgumentException("!(0 <= min && min <= max && max <= 1): min="+min+" max="+max);
        minColorWeight = min;
        maxColorWeight = max;
        return this;
    }
    
    /**
     * Returns the minimum variation coefficient.
     * 
     * @return the minimum variation coefficient
     */
    public float getMinVariationCoefficient() {
        return minVariationCoefficient;
    }
    
    /**
     * Returns the maximum variation coefficient.
     * 
     * @return the maximum variation coefficient
     */
    public float getMaxVariationCoefficient() {
        return maxVariationCoefficient;
    }
    
    /**
     * Sets the minimum and maximum transform color weight.
     * 
     * @param min the minimum variation coefficient
     * @param max the maximum variation coefficient
     * @return this {@code RandomFlameFactory}
     * @throws IllegalArgumentException if {@code min} is not positive, {@code min} is less than {@code max}, or {@code max} is not finite
     */
    public RandomFlameFactory setVariationCoefficient(float min, float max) {
        if (!(0 < min && min <= max && max < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("!(0 < min && min <= max && max < Float.POSITIVE_INFINITY): min="+min+" max="+max);
        minVariationCoefficient = min;
        maxVariationCoefficient = max;
        return this;
    }
    
    /**
     * Returns the minimum possible value of
     * {@code sqrt(affine.a^2 + affine.d^2)} and
     * {@code {@code sqrt(affine.b^2 + affine.e^2)}}.
     *
     * @return the minimum affine scale
     */
    public float getMinAffineScale() {
        return minAffineTheta;
    }
    
    /**
     * Returns the maximum possible value of
     * {@code sqrt(affine.a^2 + affine.d^2)} and
     * {@code {@code sqrt(affine.b^2 + affine.e^2)}}.
     *
     * @return the maximum affine scale
     */
    public float getMaxAffineScale() {
        return maxAffineScale;
    }
    
    /**
     * Sets the maximum and minimum possible value of
     * {@code sqrt(affine.a^2 + affine.d^2)} and
     * {@code {@code sqrt(affine.b^2 + affine.e^2)}}.
     * 
     * @param min the minimum affine scale
     * @param max the maximum affine scale
     * @return this {@code RandomFlameFactory}
     * @throws IllegalArgumentException if {@code min} is negative, {@code min} is less than {@code max}, or {@code max} is not finite
     */
    public RandomFlameFactory setAffineScale(float min, float max) {
        if (!(0 <= min && min <= max && max < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("!(0 <= min && min <= max && max < Float.POSITIVE_INFINITY): min="+min+" max="+max);
        minAffineTheta = min;
        maxAffineTheta = max;
        return this;
    }
    
    /**
     * Returns the minimum angle between vectors {@code <affine.a, affine.d>}
     * and {@code <affine.b, affine.e>} in radians.
     *
     * @return the minimum angle in radians
     */
    public float getMinAffineTheta() {
        return minAffineTheta;
    }
    
    /**
     * Returns the maximum angle between vectors {@code <affine.a, affine.d>}
     * and {@code <affine.b, affine.e>} in radians.
     * 
     * @return the maximum angle in radians
     */
    public float getMaxAffineTheta() {
        return maxAffineTheta;
    }
    
    /**
     * Sets the maximum and minimum possible angles between vectors
     * {@code <affine.a, affine.d>} and {@code <affine.b, affine.e>} in radians.
     * 
     * @param min the minimum angle in radians
     * @param max the maximum angle in radians
     * @return this {@code RandomFlameFactory}
     * @throws IllegalArgumentException if {@code min} is negative, {@code min} is less than {@code max}, or {@code max} is greater than {@code pi/2}
     */
    public RandomFlameFactory setAffineTheta(float min, float max) {
        if (!(0 <= min && min <= max && max <= Math.PI/2))
            throw new IllegalArgumentException("!(0 <= min && min <= max && max <= Math.PI/2): min="+min+" max="+max);
        minAffineTheta = min;
        maxAffineTheta = max;
        return this;
    }
    
    /**
     * Returns the minimum possible value of
     * {@code sqrt(affine.c^2 + affine.f^2)}.
     *
     * @return the minimum affine translation
     */
    public float getMinAffineTranslation() {
        return minAffineTranslation;
    }

    /**
     * Returns the maximum possible value of
     * {@code sqrt(affine.c^2 + affine.f^2)}.
     *
     * @return the maximum affine translation
     */
    public float getMaxAffineTranslation() {
        return maxAffineTranslation;
    }

    /**
     * Sets the maximum and minimum possible values of
     * {@code sqrt(affine.c^2 + affine.f^2)}.
     * 
     * @param min the minimum affine translation
     * @param max the maximum affine translation
     * @return this {@code RandomFlameFactory}
     * @throws IllegalArgumentException if {@code min} is negative, {@code min} is less than {@code max}, or {@code max} is not finite
     */
    public RandomFlameFactory setAffineTranslation(float min, float max) {
        if (!(0 < min && min <= max && max < Float.POSITIVE_INFINITY))
            throw new IllegalArgumentException("!(0 <= min && min <= max && max < Float.POSITIVE_INFINITY): min="+min+" max="+max);
        minAffineTranslation = min;
        maxAffineTranslation = max;
        return this;
    }
    
    /**
     * Returns {@code true} if the final transform will be randomized, otherwise
     * the final transform will be an identity transform.
     * 
     * @return {@code true} the final transform will be randomized
     */
    public boolean getUseFinalTransform() {
        return useFinalTransform;
    }
    
    /**
     * If set to {@code true} final transform will be randomized, otherwise the
     * final transform will be an identity transform.
     *
     * @param useFinalTransform {@code true} if final transform should be
     * randomized
     * @return this {@code RandomFlameFactory}
     */
    public RandomFlameFactory setUseFinalTransform(boolean useFinalTransform) {
        this.useFinalTransform = useFinalTransform;
        return this;
    }
    
    /**
     * Returns {@code true} if post-variation affines will be randomized,
     * otherwise the post-variation affine's will be identity transforms.
     * 
     * @return {@code true} if post-variation affine's will be randomized
     */
    public boolean getUsePstAffines() {
        return usePstAffines;
    }
    
    /**
     * If set to {@code true} final post-variation affine's will be randomized,
     * otherwise the post-variation affine's will be identity transforms.
     * 
     * @param usePstAffines {@code true} if final post-variation affines
     * should be randomized
     * @return this {@code RandomFlameFactory}
     */
    public RandomFlameFactory setUsePstAffines(boolean usePstAffines) {
        this.usePstAffines = usePstAffines;
        return this;
    }
    
    /**
     * Returns {@code true} if the values for parameters of parametric
     * variations will be selected at random from within the range [0,1],
     * otherwise default parameter vales will be used.
     * 
     * @return {@code true} if the values for parameters of parametric
     * variations will be selected at random
     */
    public boolean getUseRandomParameters() {
        return useRandomParameters;
    }
    
    /**
     * If set to {@code true} if the values for parameters of parametric
     * variations will be selected at random from within the range [0,1],
     * otherwise default parameter vales will be used.
     * 
     * @param useRandomParameters {@code true} if parametric variation
     * parameters should be randomized 
     * @return this {@code RandomFlameFactory}
     */
    public RandomFlameFactory setUseRandomParameters(boolean useRandomParameters) {
        this.useRandomParameters = useRandomParameters;
        return this;
    }
    
    /**
     * The list of potential variations.
     * <p>
     * The returned list is <i>not</i> a copy. Changes made to the returned list
     * will effect the flames generated by this {@code RandomFlameFactory}.
     * <p>
     * If this list is empty, then one linear variations will be used per flame.
     * 
     * @return the list of potential variations
     */
    public List<VariationDefinition> getVariationList() {
        return variationList;
    }
    
    /**
     * Returns a newly constructed {@code Flame} object using this
     * {@code RandomFlameFactory}'s parameters.
     * 
     * @return a new random {@code Flame}
     */
    public Flame newRandomFlame() {
        // Make sure the min and max number of variations is reasonable
        int minVariations = Math.min(minNumVariations, variationList.size());
        int maxVariations = Math.min(maxNumVariations, variationList.size());
        
        // Construct a new flame
        Flame flame = new Flame();
        
        // Determine how many transforms will be in the flame and create them
        int numTransforms = minNumTransforms + random.nextInt(maxNumTransforms-minNumTransforms+1);
        for(int i=0; i<numTransforms+1; i++)
            flame.addTransform();
        
        /// For each transform
        for (int i=0; i<flame.numTransforms; i++) {
            // Only do the final transform if the flag has been set
            if (i == 0 && !useFinalTransform)
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
            radius = random.nextFloat()*(maxAffineScale-minAffineScale)+minAffineScale;
            xform.getPreAffine().setB((float)(radius*Math.cos(theta)));
            xform.getPreAffine().setE((float)(radius*Math.sin(theta)));
            xform.getPreAffine().setC((random.nextFloat()*(maxAffineTranslation-minAffineTranslation)+minAffineTranslation)*(random.nextInt()>0?+1:-1));
            xform.getPreAffine().setF((random.nextFloat()*(maxAffineTranslation-minAffineTranslation)+minAffineTranslation)*(random.nextInt()>0?+1:-1));
            // Randomize the pst affine
            if (usePstAffines) {
                theta += ((random.nextFloat()*(maxAffineTheta-minAffineTheta)+minAffineTheta)*(random.nextInt()>0?+1:-1));
                radius = random.nextFloat()*(maxAffineScale-minAffineScale)+minAffineScale;
                xform.getPstAffine().setA((float)(radius*Math.cos(theta)));
                xform.getPstAffine().setD((float)(radius*Math.sin(theta)));
                theta += (random.nextFloat()*(Math.PI-minAffineTheta*2.0)+minAffineTheta)*Math.signum(random.nextInt());
                radius = random.nextFloat()*(maxAffineScale-minAffineScale)+minAffineScale;
                xform.getPstAffine().setB((float)(radius*Math.cos(theta)));
                xform.getPstAffine().setE((float)(radius*Math.sin(theta)));
                xform.getPreAffine().setC((random.nextFloat()*(maxAffineTranslation-minAffineTranslation)+minAffineTranslation)*(random.nextInt()>0?+1:-1));
                xform.getPreAffine().setF((random.nextFloat()*(maxAffineTranslation-minAffineTranslation)+minAffineTranslation)*(random.nextInt()>0?+1:-1));
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
                instance.setCoefficient(random.nextFloat()*(maxVariationCoefficient-minVariationCoefficient) + minVariationCoefficient);
                // Randomize the parameters
                if (useRandomParameters) {
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
}
