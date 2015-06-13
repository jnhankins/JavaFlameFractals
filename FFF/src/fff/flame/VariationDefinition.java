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

import fff.util.DynamicJavaCompiler;
import fff.util.Point2D;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

/**
 * Class {@code VariationDefinition} represents a a non-linear transformation
 * function called a variation. 
 * <p>
 * Variation functions are split into two classes: {@code VariationDefinition}
 * and {@code VariationInstance}. A {@code VariationDefinition} defines the
 * algorithms that implement the variation's function, while a
 * {@code VariationInstance} contains a reference to a
 * {@code VariationDefinition} and can contains parameters used by the variation
 * function.
 * <p>
 * To non-statically store the algorithm that implements the a non-linear 
 * variation function, {@code VariationDefinition} store the raw OpenCL/Java 
 * source code for the function as a {@code String}. The code can then be 
 * incorporated into a dynamically generated Java class which can be compiled, 
 * loaded, and instantiated at runtime (see {@link DynamicJavaCompiler}), or the
 * incorporated directly into an OpenCL kernel and compiled just in time. The
 * benefits are two-fold: Users can create and edit new variations at runtime.
 * Code can be optimized so that the minimum number of variations are 
 * incorporated into the rendering kernel (this reduces function selection 
 * time which is traditionally performed by either an if-else cascade or a 
 * switch).
 * <p>
 * Variations are intended to be non-linear, since if one wanted to include a
 * linear transform as a variation it is more computationally efficient to
 * incorporate the desired linear transformation into either the pre-affine or 
 * post-affine transformation (affine transformations are a superset of linear  
 * transformations and can be efficiently composited). However, the identity 
 * transformation is a linear transformation which is traditionally used as a 
 * variation in flame fractals and given the name "Linear".
 * <p>
 * All {@code VariationDefinition} instances should be given unique
 * {@code String} names. Though not a strict requirement, allowing
 * {@code VarationDefinition} instances to share a common name may cause errors.
 * For instance, if a {@code Flame} contains two distinct
 * {@code VariationDefinition} objects that have the same name, errors will
 * occur.
 * 
 * @see Transform
 * @see VariationInstance
 * 
 * @author Jeremiah N. Hankins
 */
public class VariationDefinition implements Comparable<VariationDefinition>, Serializable {
    protected final String name;
    protected final String code;
    protected final TreeMap<String,Float> parameters;
    protected transient VariationFunction function;
    
    /**
     * Constructs a new {@code VariationDefinition} with the given name, source
     * code, and parameters. If {@code null} is provided instead of parameter 
     * map, then the {@code VariationDefinition}'s parameter map will be empty.
     * 
     * @param name the name
     * @param code the source code
     * @param parameters the parameters, optionally null
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty
     * @throws IllegalArgumentException if {@code code} is {@code null} or empty
     * @throws IllegalArgumentException if any of {@code parameter}'s {@code String} keys are {@code null} or empty
     * @throws IllegalArgumentException if any of {@code parameter}'s {@code Float} values are {@code null} or not in the range (-inf,inf)
     */
    public VariationDefinition(String name, String code, Map<String,Float> parameters) {
        this(name, code, parameters, null);
    }
    
    /**
     * Constructs a new {@code VariationDefinition} with the given name, source
     * code, and parameters. If {@code null} is provided instead of parameter 
     * map, then the {@code VariationDefinition}'s parameter map will be empty.
     * 
     * Because this method invokes the Java Compiler, this function optionally
     * takes a {@link DiagnosticListener} as an argument. If the argument is not
     * {@code null}, warning and error messages generated by the compiler will 
     * be passed to the  {@code DiagnosticListener}.
     * 
     * @param name the name
     * @param code the source code
     * @param parameters the parameters, optionally null
     * @param dl the compiler diagnostics listener, optionally null
     * @throws IllegalArgumentException if {@code name} is {@code null} or empty
     * @throws IllegalArgumentException if {@code code} is {@code null} or empty
     * @throws IllegalArgumentException if any of {@code parameter}'s {@code String} keys are {@code null} or empty
     * @throws IllegalArgumentException if any of {@code parameter}'s {@code Float} values are {@code null} or not in the range (-inf,inf)
     */
    public VariationDefinition(String name, String code, Map<String,Float> parameters, DiagnosticListener<JavaFileObject> dl) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name is null or empty");
        if (code == null || code.isEmpty())
           throw new IllegalArgumentException("code is null or empty");
        
        this.name = name;
        this.code = code;
        this.parameters = new TreeMap();
        
        if (parameters != null) {
            for (Map.Entry<String, Float> parameter: parameters.entrySet()) {
                String key = parameter.getKey();
                Float value = parameter.getValue();
                if (key == null || key.isEmpty())
                    throw new IllegalArgumentException("parameter key is null or empty");
                if (value == null || !(Float.NEGATIVE_INFINITY<value && value<Float.POSITIVE_INFINITY))
                    throw new IllegalArgumentException("parameter value is not in the range (-inf,inf): "+value);
                this.parameters.put(key, value);
            }
        }
        
        if (dl != null) {
            function = makeFunction(dl);
        }
    }
    
    protected VariationFunction makeFunction(DiagnosticListener<JavaFileObject> dl) {
        // Copy the variation code
        String variationCode = code;
        // Simulate #DEFINE macros
        variationCode = variationCode.replaceAll("AFFINE_A","(AFFINE[0])");
        variationCode = variationCode.replaceAll("AFFINE_B","(AFFINE[2])");
        variationCode = variationCode.replaceAll("AFFINE_C","(AFFINE[4])");
        variationCode = variationCode.replaceAll("AFFINE_D","(AFFINE[1])");
        variationCode = variationCode.replaceAll("AFFINE_E","(AFFINE[3])");
        variationCode = variationCode.replaceAll("AFFINE_F","(AFFINE[5])");
        variationCode = variationCode.replaceAll("RADIUS2","(x*x+y*y)");
        variationCode = variationCode.replaceAll("RADIUS","(sqrt(x*x+y*y))");
        variationCode = variationCode.replaceAll("THETA","(atan2(x,y))");
        variationCode = variationCode.replaceAll("PHI","(atan2(y,x))");
        variationCode = variationCode.replaceAll("OMEGA","(0.5<RAND?0.0:PI)");
        variationCode = variationCode.replaceAll("DELTA","(0.5<RAND?-1.0:1.0)");
        variationCode = variationCode.replaceAll("PSI","(RAND)");
        variationCode = variationCode.replaceAll("RAND","(RAND.nextDouble())");
        // Fix parameters
        int paramIndex = 0;
        for (String parameterName: this.parameters.keySet()) {
            variationCode = variationCode.replaceAll(parameterName, "(PARAMETERS["+paramIndex+"])");
            paramIndex++;
        }
        // Format the variation's source code
        variationCode = variationCode.replaceAll("\n","\n        ").trim(); 
        // Put the code into the template
        String variationTemplate = ""+
            "package fff.flame;\n" +
            "\n" +
            "import fff.util.Point2D;\n" +
            "import java.util.Random;\n" +
            "import fff.flame.VariationDefinition.*;\n" +
            "import static java.lang.Math.*;\n" +
            "\n" +
            "public class VariationImpl implements VariationFunction {\n" +
            "    Random RAND = new Random();\n" +
            "    \n" +
            "    public void apply(Point2D RESULT, double COEF, double[] PARAMETERS, double[] AFFINE) {\n" +
            "        final double x = RESULT.x;\n" +
            "        final double y = RESULT.y;\n" +
            "        double X, Y;\n" +
            "        ___VARIATION_CODE___\n"+
            "        RESULT.x = X;\n" +
            "        RESULT.y = Y;\n" +
            "    }\n" +
            "    \n" +
            "    double fmod(double a, double b) { return a-b*floor(a/b); };\n" +
            "    double trunc(double a) { return Math.floor(a)+(a>0?0:1); };\n" +
            "    double fabs(double a) { return Math.abs(a); };\n" +
            "}";
        variationCode = variationTemplate.replace("___VARIATION_CODE___", variationCode);
        // Compile the function
        return DynamicJavaCompiler.compile(VariationFunction.class, "fff.flame.VariationImpl", variationCode, dl);
    }
    
    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the function's source code.
     * 
     * @return the source code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Returns an unmodifiable view of the parameters.
     * 
     * @return the parameters
     */
    public Map<String,Float> getParameters() {
        return parameters;
    }

    // Used by the TreeSet in Flame
    @Override
    public int compareTo(VariationDefinition v) {
        if (v == null)
            return 1;
        return name.compareTo(v.name);
    }
    
    /**
     * Apply this variation transformation to the given point with the given
     * coefficient, parameters, and affine.
     * @param point the point to transform
     * @param coefficient the variation coefficient, should be positive
     * @param parameters the parameter map - if null, default parameters will be used
     * @param affineArray the affine transform - if null, an identity affine will be used
     * @throws IllegalArgumentException if the affine is not null and is not length six
     * @throws IllegalArgumentException if the keys of the parameter map do not equal the keys of this variation
     */
    public void apply(Point2D point, float coefficient, Map<String,Float> parameters, double[] affineArray) {
        if (affineArray != null && affineArray.length != 6)
            throw new IllegalArgumentException("affineArray is not length six");
        // If no parameters were provided, use default apramters
        if (parameters == null)
            parameters = this.parameters;
        else if (!parameters.keySet().equals(this.parameters.keySet()))
            throw new IllegalArgumentException("The given parameter keys are not the same as this variation's parameter keys.");
        // Fill the parameter array
        double[] parameterArray = new double[this.parameters.size()];
        int parameterIndex = 0;
        for (Float value: parameters.values())
            parameterArray[parameterIndex++] = value;
        // Fill the affine array
        if (affineArray == null) {
            affineArray = new double[]{ 1, 1, 0, 0, 0, 0 };
        }
        // Apply the transform
        if (function == null)
            function = makeFunction(null);
        function.apply(point, coefficient, parameterArray, affineArray);
        point.x *= coefficient;
        point.y *= coefficient;
    }
    
    /**
     * Apply this variation transformation to the given point with the given
     * coefficient, parameters, and an identity affine.
     * @param point the point to transform
     * @param coefficient the variation coefficient, should be positive
     * @param parameters the parameter map - if null, default parameters will be used
     * @throws IllegalArgumentException if the keys of the parameter map do not equal the keys of this variation
     */
    public void apply(Point2D point, float coefficient, Map<String,Float> parameters) {
        apply(point, coefficient, parameters, null);
    }
    
    /**
     * Apply this variation transformation to the given point with the given
     * coefficient, default parameters, and an identity affine.
     * @param point the point to transform
     * @param coefficient the variation coefficient, should be positive
     */
    public void apply(Point2D point, float coefficient) {
        apply(point, coefficient, null);
    }
    
    protected static interface VariationFunction {
        public void apply(Point2D point, double coefficient, double[] parameters, double[] affine);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
//    public static VariationDefinition LINEAR = new VariationDefinition(
//        "Linear",
//        "X = x;\n" +
//        "Y = y;\n",
//        null);
//    public static VariationDefinition SINUSOIDAL = new VariationDefinition(
//        "Sinusoidal",
//        "X = sin(x);\n" +
//        "Y = sin(y);\n",
//        null);
//    public static VariationDefinition SPHERICAL = new VariationDefinition(
//        "Spherical",
//        "X = x/RADIUS2;\n" + 
//        "Y = y/RADIUS2;\n",
//        null);
//    public static VariationDefinition SWIRL = new VariationDefinition(
//        "Swirl",
//        "double s = sin(RADIUS2);\n" + 
//        "double c = cos(RADIUS2);\n" + 
//        "X = x*s-y*c;\n" + 
//        "Y = x*c+y*s;\n",
//        null);
//    public static VariationDefinition HORSESHOE = new VariationDefinition(
//        "Horseshoe",
//        "double r = RADIUS;\n" +
//        "X = (x-y)*(x+y)/r;\n" +
//        "Y = 2.0*x*y/r;\n",
//        null);
//    
//    
//    public static Map<String, VariationDefinition> makeStandardDefinitionMap() {
//        Map<String, VariationDefinition> map = new LinkedHashMap();
//        
//        VariationDefinition definition;
//        Map<String,Float> parameters = new TreeMap();
//        
//        map.put("Linear", LINEAR);
//        
//        definition = new VariationDefinition("Sinusoidal", ""
//            + "X = sin(x);\n"
//            + "Y = sin(y);\n", null);
//        map.put("Sinusoidal", definition);
//        
//        definition = new VariationDefinition("Spherical", ""
//            + "X = x/RADIUS2;\n"
//            + "Y = y/RADIUS2;\n", null);
//        map.put("Spherical", definition);
//        
//        definition = new VariationDefinition("Swirl", ""
//            + "double s = sin(RADIUS2);\n"
//            + "double c = cos(RADIUS2);\n"
//            + "X = x*s-y*c;\n"
//            + "Y = x*c+y*s;\n", null);
//        map.put("Swirl", definition);
//        
//        definition = new VariationDefinition("Horseshoe", ""
//            + "double r = RADIUS;\n"
//            + "X = (x-y)*(x+y)/r;\n"
//            + "Y = 2.0*x*y/r;\n", null);
//        map.put("Horseshoe", definition);
//        
//        definition = new VariationDefinition("Polar", ""
//            + "X = THETA/PI;\n"
//            + "Y = RADIUS-1.0;\n", null);
//        map.put("Polar", definition);
//        
//        definition = new VariationDefinition("Handkerchief", ""
//            + "double r = RADIUS;\n"
//            + "double t = THETA;\n"
//            + "X = r*(sin(t+r));\n"
//            + "Y = r*(cos(t-r));\n", null);
//        map.put("Handkerchief", definition);
//        
//        definition = new VariationDefinition("Heart", ""
//            + "double r = RADIUS;\n"
//            + "double t = THETA;\n"
//            + "X = r*sin(t*r);\n"
//            + "Y =-r*cos(t*r);\n", null);
//        map.put("Heart", definition);
//        
//        definition = new VariationDefinition("Disc", ""
//            + "double r = RADIUS*PI;\n"
//            + "double t = THETA/PI;\n"
//            + "X = t*sin(r);\n"
//            + "Y = t*cos(r);\n", null);
//        map.put("Disc", definition);
//        
//        definition = new VariationDefinition("Spiral", ""
//            + "double r = RADIUS;\n"
//            + "double t = THETA;\n"
//            + "X = (cos(t)+sin(r))/r;\n"
//            + "Y = (sin(t)-cos(r))/r;\n", null);
//        map.put("Spiral", definition);
//        
//        definition = new VariationDefinition("Hyperbolic", ""
//            + "double r = RADIUS;\n"
//            + "double t = THETA;\n"
//            + "X = sin(t)/r;\n"
//            + "Y = cos(t)*r;\n", null);
//        map.put("Hyperbolic", definition);
//        
//        definition = new VariationDefinition("Diamond", ""
//            + "double r = RADIUS;\n"
//            + "double t = THETA;\n"
//            + "X = sin(t)*cos(r);\n"
//            + "Y = cos(t)*sin(r);\n", null);
//        map.put("Diamond", definition);
//        
//        definition = new VariationDefinition("Ex", ""
//            + "double r = RADIUS;\n"
//            + "double t = THETA;\n"
//            + "double s = sin(t+r);\n"
//            + "double c = cos(t-r);\n"
//            + "X = r*(s*s*s+c*c*c);\n"
//            + "Y = r*(s*s*s-c*c*c);\n", null);
//        map.put("Ex", definition);
//        
//        definition = new VariationDefinition("Julia", ""
//            + "double r = sqrt(RADIUS);\n"
//            + "double t = THETA/2.0+OMEGA;\n"
//            + "X = r*cos(t);\n"
//            + "Y = r*sin(t);\n", null);
//        map.put("Julia", definition);
//        
//        definition = new VariationDefinition("Bent", ""
//            + "X = x>=0?x:x*2.0;\n"
//            + "Y = y>=0?y:y/2.0;\n", null);
//        map.put("Bent", definition);
//        
//        definition = new VariationDefinition("Waves", ""
//            + "X = x+AFFINE_B*sin(y/(AFFINE_C*AFFINE_C));\n"
//            + "Y = y+AFFINE_E*sin(x/(AFFINE_F*AFFINE_F));\n", null);
//        map.put("Waves", definition);
//        
//        definition = new VariationDefinition("Fisheye", ""
//            + "double r = 2.0/(RADIUS+1.0);\n"
//            + "X = x*r;\n"
//            + "Y = y*r;\n", null);
//        map.put("Fisheye", definition);
//        
//        definition = new VariationDefinition("Popcorn", ""
//            + "X = x+AFFINE_C*sin(tan(3.0*y));\n"
//            + "Y = y+AFFINE_F*sin(tan(3.0*x));\n", null);
//        map.put("Popcorn", definition);
//        
//        definition = new VariationDefinition("Exponential", ""
//            + "double e = exp(x-1.0);\n"
//            + "X = e*cos(PI*y);\n"
//            + "Y = e*sin(PI*y);\n", null);
//        map.put("Exponential", definition);
//        
//        definition = new VariationDefinition("Power", ""
//            + "double t = THETA;\n"
//            + "double r = pow(RADIUS,sin(t));\n"
//            + "X = r*cos(t);\n"
//            + "Y = r*sin(t);\n", null);
//        map.put("Power", definition);
//        
//        definition = new VariationDefinition("Cosine", ""
//            + "X =  cos(PI*x)*cosh(y);\n"
//            + "Y = -sin(PI*x)*sinh(y);\n", null);
//        map.put("Cosine", definition);
//        
//        definition = new VariationDefinition("Rings", ""
//            + "double r = RADIUS;\n"
//            + "double t = AFFINE_C*AFFINE_C;\n"
//            + "r = fmod(r+t,2.0*t)-t+r*(1.0-t);\n"
//            + "t = THETA;\n"
//            + "X = r*cos(t);\n"
//            + "Y = r*sin(t);\n", null);
//        map.put("Rings", definition);
//        
//        definition = new VariationDefinition("Fan", ""
//            + "double u = PI*AFFINE_C*AFFINE_C/2.0;\n"
//            + "u *= fmod(THETA+AFFINE_F,u*2.0)>u?-1.0:1.0;\n"
//            + "double r = RADIUS;\n"
//            + "double t = THETA;\n"
//            + "X = r*cos(t+u);\n"
//            + "Y = r*sin(t+u);\n", null);
//        map.put("Fan", definition);
//        
//        parameters.clear();
//        parameters.put("Blob_Low",1.0f);
//        parameters.put("Blob_High",2.0f);
//        parameters.put("Blob_Waves",4.0f);
//        definition = new VariationDefinition("Blob", ""
//            + "double t = THETA;\n"
//            + "double r = RADIUS * (Blob_Low+(Blob_High-Blob_Low)*(sin(Blob_Waves*t)+1.0)/2.0);\n"
//            + "X = r*cos(t);\n"
//            + "Y = r*sin(t);\n", parameters);
//        map.put("Blob", definition);
//        
//        parameters.clear();
//        parameters.put("PDJ_A",1.0f);
//        parameters.put("PDJ_B",1.0f);
//        parameters.put("PDJ_C",1.0f);
//        parameters.put("PDJ_D",1.0f);
//        definition = new VariationDefinition("PDJ", ""
//            + "X = sin(PDJ_A*y)-cos(PDJ_B*x);\n"
//            + "Y = sin(PDJ_C*x)-cos(PDJ_D*y);\n", parameters);
//        map.put("PDJ", definition);
//        
//        parameters.clear();
//        parameters.put("Fan2_X",1.0f);
//        parameters.put("Fan2_Y",1.0f);
//        definition = new VariationDefinition("Fan2", ""
//            + "double t = THETA;\n"
//            + "double p = PI*Fan2_X*Fan2_X/2.0;\n"
//            + "double u = t+Fan2_Y-p*trunc(2.0*PI*Fan2_Y/p);\n"
//            + "u *= u>p?-1.0:1.0;\n"
//            + "p = RADIUS;\n"
//            + "X = p*cos(t+u);\n"
//            + "Y = p*sin(t+u);\n", parameters);
//        map.put("Fan2", definition);
//        
//        parameters.clear();
//        parameters.put("Rings2_Val",1.0f);
//        definition = new VariationDefinition("Rings2", ""
//            + "double p = Rings2_Val*Rings2_Val;\n"
//            + "double r = RADIUS;\n"
//            + "p = r-2.0*p*trunc((r+p)/(2.0*p))+r*(1.0-p);\n"
//            + "double t = THETA;\n"
//            + "X = t*sin(p);\n"
//            + "Y = t*cos(p);\n", parameters);
//        map.put("Rings2", definition);
//        
//        definition = new VariationDefinition("Eyefish", ""
//            + "double r = 2.0/(RADIUS+1.0);\n"
//            + "X = x*r;\n"
//            + "Y = y*r;\n", null);
//        map.put("Eyeﬁsh", definition);
//        
//        definition = new VariationDefinition("Bubble", ""
//            + "double r = RADIUS;\n"
//            + "r = 4.0/(r*r+4.0);\n"
//            + "X = x*r;\n"
//            + "Y = y*r;\n", null);
//        map.put("Bubble", definition);
//        
//        definition = new VariationDefinition("Cylinder", ""
//            + "X = sin(x);\n"
//            + "Y = y;\n", null);
//        map.put("Cylinder", definition);
//        
//        parameters.clear();
//        parameters.put("Perspective_Angle",1.0f);
//        parameters.put("Perspective_Dist",1.0f);
//        definition = new VariationDefinition("Perspective", ""
//            + "double p = Perspective_Dist/(Perspective_Dist-y*sin(Perspective_Angle));\n"
//            + "X = x*p;\n"
//            + "Y = y*p*cos(Perspective_Angle);\n", parameters);
//        map.put("Perspective", definition);
//        
//        definition = new VariationDefinition("Noise", ""
//            + "double u = PSI;\n"
//            + "double v = 2.0*PI*PSI;\n"
//            + "X = u*x*cos(v);\n"
//            + "Y = u*y*sin(v);\n", null);
//        map.put("Noise", definition);
//        
//        parameters.clear();
//        parameters.put("JuliaN_Power",1.0f);
//        parameters.put("JuliaN_Dist",1.0f);
//        definition = new VariationDefinition("JuliaN", ""
//            + "double p = trunc(fabs(JuliaN_Power)*PSI);\n"
//            + "double t = (PHI+2.0*PI*p)/JuliaN_Power;\n"
//            + "p = pow(RADIUS,JuliaN_Power/JuliaN_Dist);\n"
//            + "X = p*cos(t);\n"
//            + "Y = p*sin(t);\n", parameters);
//        map.put("JuliaN", definition);
//        
//        parameters.clear();
//        parameters.put("JuliaScope_Power",1.0f);
//        parameters.put("JuliaScope_Dist",1.0f);
//        definition = new VariationDefinition("JuliaScope", ""
//            + "double p = trunc(fabs(JuliaScope_Power)*PSI);\n"
//            + "double t = (DELTA*PHI+2.0*PI*p)/JuliaScope_Power;\n"
//            + "p = pow(RADIUS,JuliaScope_Power/JuliaScope_Dist);\n"
//            + "X = p*cos(t);\n"
//            + "Y = p*sin(t);\n", parameters);
//        map.put("JuliaScope", definition);
//        
//        definition = new VariationDefinition("Blur", ""
//            + "double u = PSI;\n"
//            + "double v = PSI*2.0*PI;\n"
//            + "X = u*cos(v);\n"
//            + "Y = u*sin(v);\n", null);
//        map.put("Blur", definition);
//        
//        definition = new VariationDefinition("Gaussian", ""
//            + "double u = PSI+PSI+PSI+PSI-2.0;\n"
//            + "double v = 2.0*PI*PSI;\n"
//            + "X = u*cos(v);\n"
//            + "Y = u*sin(v);\n", null);
//        map.put("Gaussian", definition);
//        
//        parameters.clear();
//        parameters.put("RadialBlur_Angle",1.0f);
//        definition = new VariationDefinition("RadialBlur", ""
//            + "double p = RadialBlur_Angle*PI/2.0;\n"
//            + "double u = COEF*(PSI*PSI*PSI*PSI-8.0);\n"
//            + "double v = PHI+u*sin(p);\n"
//            + "double w = u*cos(p)-1.0;\n"
//            + "double r = RADIUS;\n"
//            + "X = (r*cos(v)+w*x)/COEF;\n"
//            + "Y = (r*sin(v)+w*y)/COEF;\n", parameters);
//        map.put("RadialBlur", definition);
//        
//        parameters.clear();
//        parameters.put("Pie_Slices",1.0f);
//        parameters.put("Pie_Rotation",1.0f);
//        parameters.put("Pie_Thickness",1.0f);
//        definition = new VariationDefinition("Pie", ""
//            + "double u = trunc(PSI*Pie_Slices+0.5);\n"
//            + "double v = Pie_Rotation+2.0*PI*(u+PSI*Pie_Thickness)/Pie_Slices;\n"
//            + "u = PSI;\n"
//            + "X = u*cos(v);\n"
//            + "Y = u*sin(v);\n", parameters);
//        map.put("Pie", definition);
//        
//        parameters.clear();
//        parameters.put("Ngon_Power",1.0f);
//        parameters.put("Ngon_Sides",1.0f);
//        parameters.put("Ngon_Corners",1.0f);
//        parameters.put("Ngon_Circle",1.0f);
//        definition = new VariationDefinition("Ngon", ""
//            + "double p = 2.0*PI/Ngon_Power;\n"
//            + "double t = PHI-p*floor(PHI/p);\n"
//            + "t = t>p/2.0?t:t-p;\n"
//            + "p = (p*(1.0/cos(t)-1.0)+Ngon_Circle)/pow(RADIUS,Ngon_Power);\n"
//            + "X = x*p;\n"
//            + "Y = y*p;\n", parameters);
//        map.put("Ngon", definition);
//        
//        parameters.clear();
//        parameters.put("Curl_C1",1.0f);
//        parameters.put("Curl_C2",1.0f);
//        definition = new VariationDefinition("Curl", ""
//            + "double u = 1.0+Curl_C1*x+Curl_C2*(x*x-y*y);\n"
//            + "double v = Curl_C1*y+2.0*Curl_C2*x*y;\n"
//            + "X = (x*u+y*v)/(u*u+v*v);\n"
//            + "Y = (y*u+x*v)/(u*u+v*v);\n", parameters);
//        map.put("Curl", definition);
//        
//        parameters.clear();
//        parameters.put("Rectangles_X",1.0f);
//        parameters.put("Rectangles_Y",1.0f);
//        definition = new VariationDefinition("Rectangles", ""
//            + "X = (2.0*floor(x/Rectangles_X)+1.0)*Rectangles_X-x;\n"
//            + "Y = (2.0*floor(y/Rectangles_Y)+1.0)*Rectangles_Y-y;\n", parameters);
//        map.put("Rectangles", definition);
//        
//        definition = new VariationDefinition("Arch", ""
//            + "double p = PSI;\n"
//            + "double s = sin(p*PI*COEF);\n"
//            + "X = s;\n"
//            + "Y = s*s/cos(p*PI*COEF);\n", null);
//        map.put("Arch", definition);
//        
//        definition = new VariationDefinition("Tangent", ""
//            + "X = sin(x)/cos(y);\n"
//            + "Y = tan(y);\n", null);
//        map.put("Tangent", definition);
//        
//        definition = new VariationDefinition("Square", ""
//            + "X = PSI-0.5;\n"
//            + "Y = PSI-0.5;\n", null);
//        map.put("Square", definition);
//        
//        definition = new VariationDefinition("Rays", ""
//            + "double u = COEF*tan(PSI*PI*COEF)/RADIUS2;\n"
//            + "X = cos(x)*u;\n"
//            + "Y = sin(y)*u;\n", null);
//        map.put("Rays", definition);
//        
//        definition = new VariationDefinition("Blade", ""
//            + "double p = PSI*RADIUS*COEF;\n"
//            + "double s = sin(p);\n"
//            + "double c = cos(p);\n"
//            + "X = x*(c+s);\n"
//            + "Y = x*(c-s);\n", null);
//        map.put("Blade", definition);
//        
//        definition = new VariationDefinition("Secant", ""
//            + "X = x;\n"
//            + "Y = 1.0/(COEF*cos(COEF*RADIUS));\n", null);
//        map.put("Secant", definition);
//        
//        definition = new VariationDefinition("Twintrian", ""
//            + "double p = PSI*RADIUS*COEF;\n"
//            + "double s = sin(p);\n"
//            + "p = log10(s*s+cos(p));\n"
//            + "X = x*p;\n"
//            + "Y = x*(p-PI*s);\n", null);
//        map.put("Twintrian", definition);
//        
//        definition = new VariationDefinition("Cross", ""
//            + "double u = sqrt(1.0/((x*x-y*y)*(x*x-y*y)));\n"
//            + "X = x*u;\n"
//            + "Y = y*u;\n", null);
//        map.put("Cross", definition);
//        
//        definition = new VariationDefinition("Butterfly", ""
//            + "double r = RADIUS;\n"
//            + "double s = sin(r/12.0);\n"
//            + "double v = exp(cos(r)) - 2.0*cos(4.0*r) - s*s*s*s*s;\n"
//            + "X = AFFINE_C+sin(x)*v;\n"
//            + "Y = AFFINE_F+cos(y)*v;\n", null);
//        map.put("Butterfly", definition);
//        
//        return map;
//    }
    
    
    public static final VariationDefinition LINEAR;       //  0
    public static final VariationDefinition SINUSOIDAL;   //  1
    public static final VariationDefinition SPHERICAL;    //  2
    public static final VariationDefinition SWIRL;        //  3
    public static final VariationDefinition HORSESHOE;    //  4
    public static final VariationDefinition POLAR;        //  5
    public static final VariationDefinition HANDKERCHIEF; //  6
    public static final VariationDefinition HEART;        //  7
    public static final VariationDefinition DISC;         //  8
    public static final VariationDefinition SPIRAL;       //  9
    public static final VariationDefinition HYPERBOLIC;   // 10
    public static final VariationDefinition DIAMOND;      // 11
    public static final VariationDefinition EX;           // 12
    public static final VariationDefinition JULIA;        // 13
    public static final VariationDefinition BENT;         // 14
    public static final VariationDefinition WAVES;        // 15
    public static final VariationDefinition FISHEYE;      // 16
    public static final VariationDefinition POPCORN;      // 17
    public static final VariationDefinition EXPONENTIAL;  // 18
    public static final VariationDefinition POWER;        // 19
    public static final VariationDefinition COSINE;       // 20
    public static final VariationDefinition RINGS;        // 21
    public static final VariationDefinition FAN;          // 22
    public static final VariationDefinition BLOB;         // 23
    public static final VariationDefinition PDJ;          // 24
    public static final VariationDefinition FAN2;         // 25
    public static final VariationDefinition RINGS2;       // 26
    public static final VariationDefinition EYEFISH;      // 27
    public static final VariationDefinition BUBBLE;       // 28
    public static final VariationDefinition CYLINDER;     // 29
    public static final VariationDefinition PERSPECTIVE;  // 30
    public static final VariationDefinition NOISE;        // 31
    public static final VariationDefinition JULIAN;       // 32
    public static final VariationDefinition JULIASCOPE;   // 33
    public static final VariationDefinition BLUR;         // 34
    public static final VariationDefinition GAUSSIAN;     // 35
    public static final VariationDefinition RADIALBLUR;   // 36
    public static final VariationDefinition PIE;          // 37
    public static final VariationDefinition NGON;         // 38
    public static final VariationDefinition CURL;         // 39
    public static final VariationDefinition RECTANGLES;   // 40
    public static final VariationDefinition ARCH;         // 41
    public static final VariationDefinition TANGENT;      // 42
    public static final VariationDefinition SQUARE;       // 43
    public static final VariationDefinition RAYS;         // 44
    public static final VariationDefinition BLADE;        // 45
    public static final VariationDefinition SECANT;       // 46
    public static final VariationDefinition TWINTRIAN;    // 47
    public static final VariationDefinition CROSS;        // 48
    public static final VariationDefinition BUTTERFLY;    // 49
    public static final Map<String,VariationDefinition> DEFAULT_MAP;

    static {
        Map<String,Float> parameters = new TreeMap();

        //  0 Linear
        LINEAR = new VariationDefinition("Linear", ""
            + "X = x;\n"
            + "Y = y;\n"
            , null);

        //  1 Sinusoidal
        SINUSOIDAL = new VariationDefinition("Sinusoidal", ""
            + "X = sin(x);\n"
            + "Y = sin(y);\n"
            , null);

        //  2 Spherical
        SPHERICAL = new VariationDefinition("Spherical", ""
            + "X = x/RADIUS2;\n"
            + "Y = y/RADIUS2;\n"
            , null);

        //  3 Swirl
        SWIRL = new VariationDefinition("Swirl", ""
            + "double s = sin(RADIUS2);\n"
            + "double c = cos(RADIUS2);\n"
            + "X = x*s-y*c;\n"
            + "Y = x*c+y*s;\n"
            , null);

        //  4 Horseshoe
        HORSESHOE = new VariationDefinition("Horseshoe", ""
            + "double r = RADIUS;\n"
            + "X = (x-y)*(x+y)/r;\n"
            + "Y = 2.0f*x*y/r;\n"
            , null);

        //  5 Polar
        POLAR = new VariationDefinition("Polar", ""
            + "X = THETA/PI;\n"
            + "Y = RADIUS-1.0f;\n"
            , null);

        //  6 Handkerchief
        HANDKERCHIEF = new VariationDefinition("Handkerchief", ""
            + "double r = RADIUS;\n"
            + "double t = THETA;\n"
            + "X = r*(sin(t+r));\n"
            + "Y = r*(cos(t-r));\n"
            , null);

        //  7 Heart
        HEART = new VariationDefinition("Heart", ""
            + "double r = RADIUS;\n"
            + "double t = THETA;\n"
            + "X = r*sin(t*r);\n"
            + "Y =-r*cos(t*r);\n"
            , null);

        //  8 Disc
        DISC = new VariationDefinition("Disc", ""
            + "double r = RADIUS*PI;\n"
            + "double t = THETA/PI;\n"
            + "X = t*sin(r);\n"
            + "Y = t*cos(r);\n"
            , null);

        //  9 Spiral
        SPIRAL = new VariationDefinition("Spiral", ""
            + "double r = RADIUS;\n"
            + "double t = THETA;\n"
            + "X = (cos(t)+sin(r))/r;\n"
            + "Y = (sin(t)-cos(r))/r;\n"
            , null);

        // 10 Hyperbolic
        HYPERBOLIC = new VariationDefinition("Hyperbolic", ""
            + "double r = RADIUS;\n"
            + "double t = THETA;\n"
            + "X = sin(t)/r;\n"
            + "Y = cos(t)*r;\n"
            , null);

        // 11 Diamond
        DIAMOND = new VariationDefinition("Diamond", ""
            + "double r = RADIUS;\n"
            + "double t = THETA;\n"
            + "X = sin(t)*cos(r);\n"
            + "Y = cos(t)*sin(r);\n"
            , null);

        // 12 Ex
        EX = new VariationDefinition("Ex", ""
            + "double r = RADIUS;\n"
            + "double t = THETA;\n"
            + "double s = sin(t+r);\n"
            + "double c = cos(t-r);\n"
            + "X = r*(s*s*s+c*c*c);\n"
            + "Y = r*(s*s*s-c*c*c);\n"
            , null);

        // 13 Julia
        JULIA = new VariationDefinition("Julia", ""
            + "double r = sqrt(RADIUS);\n"
            + "double t = THETA/2.0f+OMEGA;\n"
            + "X = r*cos(t);\n"
            + "Y = r*sin(t);\n"
            , null);

        // 14 Bent
        BENT = new VariationDefinition("Bent", ""
            + "X = x>=0?x:x*2.0f;\n"
            + "Y = y>=0?y:y/2.0f;\n"
            , null);

        // 15 Waves
        WAVES = new VariationDefinition("Waves", ""
            + "X = x+AFFINE_B*sin(y/(AFFINE_C*AFFINE_C));\n"
            + "Y = y+AFFINE_E*sin(x/(AFFINE_F*AFFINE_F));\n"
            , null);

        // 16 Fisheye
        FISHEYE = new VariationDefinition("Fisheye", ""
            + "double r = 2.0f/(RADIUS+1.0f);\n"
            + "X = x*r;\n"
            + "Y = y*r;\n"
            , null);

        // 17 Popcorn
        POPCORN = new VariationDefinition("Popcorn", ""
            + "X = x+AFFINE_C*sin(tan(3.0f*y));\n"
            + "Y = y+AFFINE_F*sin(tan(3.0f*x));\n"
            , null);

        // 18 Exponential
        EXPONENTIAL = new VariationDefinition("Exponential", ""
            + "double e = exp(x-1.0f);\n"
            + "X = e*cos(PI*y);\n"
            + "Y = e*sin(PI*y);\n"
            , null);

        // 19 Power
        POWER = new VariationDefinition("Power", ""
            + "double t = THETA;\n"
            + "double r = pow(RADIUS,sin(t));\n"
            + "X = r*cos(t);\n"
            + "Y = r*sin(t);\n"
            , null);

        // 20 Cosine
        COSINE = new VariationDefinition("Cosine", ""
            + "X =  cos(PI*x)*cosh(y);\n"
            + "Y = -sin(PI*x)*sinh(y);\n"
            , null);

        // 21 Rings
        RINGS = new VariationDefinition("Rings", ""
            + "double r = RADIUS;\n"
            + "double t = AFFINE_C*AFFINE_C;\n"
            + "r = fmod(r+t,2.0f*t)-t+r*(1.0f-t);\n"
            + "t = THETA;\n"
            + "X = r*cos(t);\n"
            + "Y = r*sin(t);\n"
            , null);

        // 22 Fan
        FAN = new VariationDefinition("Fan", ""
            + "double u = PI*AFFINE_C*AFFINE_C/2.0f;\n"
            + "u *= fmod(THETA+AFFINE_F,u*2.0f)>u?-1.0f:1.0f;\n"
            + "double r = RADIUS;\n"
            + "double t = THETA;\n"
            + "X = r*cos(t+u);\n"
            + "Y = r*sin(t+u);\n"
            , null);

        // 23 Blob
        parameters.clear();
        parameters.put("Blob_High", 2.0f);
        parameters.put("Blob_Low", 1.0f);
        parameters.put("Blob_Waves", 4.0f);
        BLOB = new VariationDefinition("Blob", ""
            + "double t = THETA;\n"
            + "double r = RADIUS * (Blob_Low+(Blob_High-Blob_Low)*(sin(Blob_Waves*t)+1.0f)/2.0f);\n"
            + "X = r*cos(t);\n"
            + "Y = r*sin(t);\n"
            , parameters);

        // 24 PDJ
        parameters.clear();
        parameters.put("PDJ_A", 1.0f);
        parameters.put("PDJ_B", 1.0f);
        parameters.put("PDJ_C", 1.0f);
        parameters.put("PDJ_D", 1.0f);
        PDJ = new VariationDefinition("PDJ", ""
            + "X = sin(PDJ_A*y)-cos(PDJ_B*x);\n"
            + "Y = sin(PDJ_C*x)-cos(PDJ_D*y);\n"
            , parameters);

        // 25 Fan2
        parameters.clear();
        parameters.put("Fan2_X", 1.0f);
        parameters.put("Fan2_Y", 1.0f);
        FAN2 = new VariationDefinition("Fan2", ""
            + "double t = THETA;\n"
            + "double p = PI*Fan2_X*Fan2_X/2.0f;\n"
            + "double u = t+Fan2_Y-p*trunc(2.0f*PI*Fan2_Y/p);\n"
            + "u *= u>p?-1.0f:1.0f;\n"
            + "p = RADIUS;\n"
            + "X = p*cos(t+u);\n"
            + "Y = p*sin(t+u);\n"
            , parameters);

        // 26 Rings2
        parameters.clear();
        parameters.put("Rings2_Val", 1.0f);
        RINGS2 = new VariationDefinition("Rings2", ""
            + "double p = Rings2_Val*Rings2_Val;\n"
            + "double r = RADIUS;\n"
            + "p = r-2.0f*p*trunc((r+p)/(2.0f*p))+r*(1.0f-p);\n"
            + "double t = THETA;\n"
            + "X = t*sin(p);\n"
            + "Y = t*cos(p);\n"
            , parameters);

        // 27 Eyefish
        EYEFISH = new VariationDefinition("Eyefish", ""
            + "double r = 2.0f/(RADIUS+1.0f);\n"
            + "X = x*r;\n"
            + "Y = y*r;\n"
            , null);

        // 28 Bubble
        BUBBLE = new VariationDefinition("Bubble", ""
            + "double r = RADIUS;\n"
            + "r = 4.0f/(r*r+4.0f);\n"
            + "X = x*r;\n"
            + "Y = y*r;\n"
            , null);

        // 29 Cylinder
        CYLINDER = new VariationDefinition("Cylinder", ""
            + "X = sin(x);\n"
            + "Y = y;\n"
            , null);

        // 30 Perspective
        parameters.clear();
        parameters.put("Perspective_Angle", 1.0f);
        parameters.put("Perspective_Dist", 1.0f);
        PERSPECTIVE = new VariationDefinition("Perspective", ""
            + "double p = Perspective_Dist/(Perspective_Dist-y*sin(Perspective_Angle));\n"
            + "X = x*p;\n"
            + "Y = y*p*cos(Perspective_Angle);\n"
            , parameters);

        // 31 Noise
        NOISE = new VariationDefinition("Noise", ""
            + "double u = PSI;\n"
            + "double v = 2.0f*PI*PSI;\n"
            + "X = u*x*cos(v);\n"
            + "Y = u*y*sin(v);\n"
            , null);

        // 32 JuliaN
        parameters.clear();
        parameters.put("JuliaN_Dist", 1.0f);
        parameters.put("JuliaN_Power", 1.0f);
        JULIAN = new VariationDefinition("JuliaN", ""
            + "double p = trunc(fabs(JuliaN_Power)*PSI);\n"
            + "double t = (PHI+2.0f*PI*p)/JuliaN_Power;\n"
            + "p = pow(RADIUS,JuliaN_Power/JuliaN_Dist);\n"
            + "X = p*cos(t);\n"
            + "Y = p*sin(t);\n"
            , parameters);

        // 33 JuliaScope
        parameters.clear();
        parameters.put("JuliaScope_Dist", 1.0f);
        parameters.put("JuliaScope_Power", 1.0f);
        JULIASCOPE = new VariationDefinition("JuliaScope", ""
            + "double p = trunc(fabs(JuliaScope_Power)*PSI);\n"
            + "double t = (DELTA*PHI+2.0f*PI*p)/JuliaScope_Power;\n"
            + "p = pow(RADIUS,JuliaScope_Power/JuliaScope_Dist);\n"
            + "X = p*cos(t);\n"
            + "Y = p*sin(t);\n"
            , parameters);

        // 34 Blur
        BLUR = new VariationDefinition("Blur", ""
            + "double u = PSI;\n"
            + "double v = PSI*2.0f*PI;\n"
            + "X = u*cos(v);\n"
            + "Y = u*sin(v);\n"
            , null);

        // 35 Gaussian
        GAUSSIAN = new VariationDefinition("Gaussian", ""
            + "double u = PSI+PSI+PSI+PSI-2.0f;\n"
            + "double v = 2.0f*PI*PSI;\n"
            + "X = u*cos(v);\n"
            + "Y = u*sin(v);\n"
            , null);

        // 36 RadialBlur
        parameters.clear();
        parameters.put("RadialBlur_Angle", 1.0f);
        RADIALBLUR = new VariationDefinition("RadialBlur", ""
            + "double p = RadialBlur_Angle*PI/2.0f;\n"
            + "double u = COEF*(PSI*PSI*PSI*PSI-8.0f);\n"
            + "double v = PHI+u*sin(p);\n"
            + "double w = u*cos(p)-1.0f;\n"
            + "double r = RADIUS;\n"
            + "X = (r*cos(v)+w*x)/COEF;\n"
            + "Y = (r*sin(v)+w*y)/COEF;\n"
            , parameters);

        // 37 Pie
        parameters.clear();
        parameters.put("Pie_Rotation", 1.0f);
        parameters.put("Pie_Slices", 1.0f);
        parameters.put("Pie_Thickness", 1.0f);
        PIE = new VariationDefinition("Pie", ""
            + "double u = trunc(PSI*Pie_Slices+0.5f);\n"
            + "double v = Pie_Rotation+2.0f*PI*(u+PSI*Pie_Thickness)/Pie_Slices;\n"
            + "u = PSI;\n"
            + "X = u*cos(v);\n"
            + "Y = u*sin(v);\n"
            , parameters);

        // 38 Ngon
        parameters.clear();
        parameters.put("Ngon_Circle", 1.0f);
        parameters.put("Ngon_Corners", 1.0f);
        parameters.put("Ngon_Power", 1.0f);
        parameters.put("Ngon_Sides", 1.0f);
        NGON = new VariationDefinition("Ngon", ""
            + "double p = 2.0f*PI/Ngon_Power;\n"
            + "double t = PHI-p*floor(PHI/p);\n"
            + "t = t>p/2.0f?t:t-p;\n"
            + "p = (p*(1.0f/cos(t)-1.0f)+Ngon_Circle)/pow(RADIUS,Ngon_Power);\n"
            + "X = x*p;\n"
            + "Y = y*p;\n"
            , parameters);

        // 39 Curl
        parameters.clear();
        parameters.put("Curl_C1", 1.0f);
        parameters.put("Curl_C2", 1.0f);
        CURL = new VariationDefinition("Curl", ""
            + "double u = 1.0f+Curl_C1*x+Curl_C2*(x*x-y*y);\n"
            + "double v = Curl_C1*y+2.0f*Curl_C2*x*y;\n"
            + "X = (x*u+y*v)/(u*u+v*v);\n"
            + "Y = (y*u+x*v)/(u*u+v*v);\n"
            , parameters);

        // 40 Rectangles
        parameters.clear();
        parameters.put("Rectangles_X", 1.0f);
        parameters.put("Rectangles_Y", 1.0f);
        RECTANGLES = new VariationDefinition("Rectangles", ""
            + "X = (2.0f*floor(x/Rectangles_X)+1.0f)*Rectangles_X-x;\n"
            + "Y = (2.0f*floor(y/Rectangles_Y)+1.0f)*Rectangles_Y-y;\n"
            , parameters);

        // 41 Arch
        ARCH = new VariationDefinition("Arch", ""
            + "double p = PSI;\n"
            + "double s = sin(p*PI*COEF);\n"
            + "X = s;\n"
            + "Y = s*s/cos(p*PI*COEF);\n"
            , null);

        // 42 Tangent
        TANGENT = new VariationDefinition("Tangent", ""
            + "X = sin(x)/cos(y);\n"
            + "Y = tan(y);\n"
            , null);

        // 43 Square
        SQUARE = new VariationDefinition("Square", ""
            + "X = PSI-0.5f;\n"
            + "Y = PSI-0.5f;\n"
            , null);

        // 44 Rays
        RAYS = new VariationDefinition("Rays", ""
            + "double u = COEF*tan(PSI*PI*COEF)/RADIUS2;\n"
            + "X = cos(x)*u;\n"
            + "Y = sin(y)*u;\n"
            , null);

        // 45 Blade
        BLADE = new VariationDefinition("Blade", ""
            + "double p = PSI*RADIUS*COEF;\n"
            + "double s = sin(p);\n"
            + "double c = cos(p);\n"
            + "X = x*(c+s);\n"
            + "Y = x*(c-s);\n"
            , null);

        // 46 Secant
        SECANT = new VariationDefinition("Secant", ""
            + "X = x;\n"
            + "Y = 1.0f/(COEF*cos(COEF*RADIUS));\n"
            , null);

        // 47 Twintrian
        TWINTRIAN = new VariationDefinition("Twintrian", ""
            + "double p = PSI*RADIUS*COEF;\n"
            + "double s = sin(p);\n"
            + "p = log10(s*s+cos(p));\n"
            + "X = x*p;\n"
            + "Y = x*(p-PI*s);\n"
            , null);

        // 48 Cross
        CROSS = new VariationDefinition("Cross", ""
            + "double u = sqrt(1.0f/((x*x-y*y)*(x*x-y*y)));\n"
            + "X = x*u;\n"
            + "Y = y*u;\n"
            , null);

        // 49 Butterfly
        BUTTERFLY = new VariationDefinition("Butterfly", ""
            + "double r = RADIUS;\n"
            + "double s = sin(r/12.0f);\n"
            + "double v = exp(cos(r)) - 2.0f*cos(4.0f*r) - s*s*s*s*s;\n"
            + "X = AFFINE_C+sin(x)*v;\n"
            + "Y = AFFINE_F+cos(y)*v;\n"
            , null);

        Map<String,VariationDefinition> map = new LinkedHashMap();
        map.put("Linear", LINEAR);
        map.put("Sinusoidal", SINUSOIDAL);
        map.put("Spherical", SPHERICAL);
        map.put("Swirl", SWIRL);
        map.put("Horseshoe", HORSESHOE);
        map.put("Polar", POLAR);
        map.put("Handkerchief", HANDKERCHIEF);
        map.put("Heart", HEART);
        map.put("Disc", DISC);
        map.put("Spiral", SPIRAL);
        map.put("Hyperbolic", HYPERBOLIC);
        map.put("Diamond", DIAMOND);
        map.put("Ex", EX);
        map.put("Julia", JULIA);
        map.put("Bent", BENT);
        map.put("Waves", WAVES);
        map.put("Fisheye", FISHEYE);
        map.put("Popcorn", POPCORN);
        map.put("Exponential", EXPONENTIAL);
        map.put("Power", POWER);
        map.put("Cosine", COSINE);
        map.put("Rings", RINGS);
        map.put("Fan", FAN);
        map.put("Blob", BLOB);
        map.put("PDJ", PDJ);
        map.put("Fan2", FAN2);
        map.put("Rings2", RINGS2);
        map.put("Eyefish", EYEFISH);
        map.put("Bubble", BUBBLE);
        map.put("Cylinder", CYLINDER);
        map.put("Perspective", PERSPECTIVE);
        map.put("Noise", NOISE);
        map.put("JuliaN", JULIAN);
        map.put("JuliaScope", JULIASCOPE);
        map.put("Blur", BLUR);
        map.put("Gaussian", GAUSSIAN);
        map.put("RadialBlur", RADIALBLUR);
        map.put("Pie", PIE);
        map.put("Ngon", NGON);
        map.put("Curl", CURL);
        map.put("Rectangles", RECTANGLES);
        map.put("Arch", ARCH);
        map.put("Tangent", TANGENT);
        map.put("Square", SQUARE);
        map.put("Rays", RAYS);
        map.put("Blade", BLADE);
        map.put("Secant", SECANT);
        map.put("Twintrian", TWINTRIAN);
        map.put("Cross", CROSS);
        map.put("Butterfly", BUTTERFLY);

        DEFAULT_MAP = Collections.unmodifiableMap(map);
}
}
