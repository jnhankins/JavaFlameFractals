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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author jnhankins
 */
public class FlameFactory {
    
    /**
     * Constructs a Sierpinksi Triangle {@code Flame}.
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/siertri/siertri.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/siertri/siertri.htm</a>
     * 
     * @return the new Sierpinksi Triangle {@code Flame}
     */
    public static Flame newSierpinskiTriangle() {
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
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
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a Sierpinksi Pedal Triangle {@code Flame}.
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/siertri/sierpedal.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/siertri/sierpedal.htm</a>
     * 
     * @param a the first inner angle
     * @param b the second inner angle
     * @return the new Sierpinksi Pedal Triangle {@code Flame}
     * @throws IllegalArgumentException if {@code a} + {@code b} is greater than or equal to pi
     */
    public static Flame newSierpinskiPedalTriangle(double a, double b) {
        if (a + b >= Math.PI)
            throw new IllegalArgumentException("a+b >= Math.pi: a="+a+" b="+b);
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Calc constants
        final double c = Math.PI - a - b;
        final float sinB = (float)Math.sin(b);
        final float cosB = (float)Math.cos(b);
        final float sinC = (float)Math.sin(c);
        final float cosC = (float)Math.cos(c);
        final float cosA = (float)Math.cos(a);
        final float sinCB = (float)Math.sin(c-b);
        final float cosCB = (float)Math.cos(c-b);
        // First Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1.0f, 0.0f, 0.0f);
        transform.getPreAffine().set(
                cosB*cosB,  cosB*sinB, 0,
                cosB*sinB, -cosB*cosB, 0);
        // Second Trasnform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(0.0f, 0.0f, 1.0f);
        transform.getPreAffine().set(
                cosC*cosC, -cosC*sinC, sinC*sinC,
               -cosC*sinC, -cosC*cosC, cosC*sinC);
        // Third Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(0.0f, 1.0f, 0.0f);
        transform.getPreAffine().set(
               -cosA*cosCB, cosA*sinCB, sinC*sinC,
                cosA*sinCB, cosA*cosCB, cosC*sinC);
        // View
        flame.getView().set(0.5f, 0.35f, 0, 2);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Sierpinksi Carpet {@code Flame}.
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/carpet/carpet.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/carpet/carpet.htm</a>
     * 
     * @return the new Sierpinksi Carpet {@code Flame}
     */
    public static Flame newSierpinskiCarpet() {
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Calc constants
        final float a = 1.0f/3.0f;
        final float b = (float)(1.0/Math.sqrt(2.0));
        // Transform #1
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().set(a, 0, b, 0, a, 0);
        // Transform #2
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 1);
        transform.getPreAffine().set(a, 0, b, 0, a, b);
        // Transform #3
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 0, 1);
        transform.getPreAffine().set(a, 0, 0, 0, a, b);
        // Transform #4
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 0, 1);
        transform.getPreAffine().set(a, 0, -b, 0, a, b);
        // Transform #5
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 0, 0);
        transform.getPreAffine().set(a, 0, -b, 0, a, 0);
        // Transform #6
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 0, 0);
        transform.getPreAffine().set(a, 0, -b, 0, a, -b);
        // Transform #7
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().set(a, 0, 0, 0, a, -b);
        // Transform #8
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 1);
        transform.getPreAffine().set(a, 0, b, 0, a, -b);
        // View
        flame.getView().set(0, 0, 0, 0.8f);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs new a Sierpinski Gasket {@code Flame} of degree N.
     * 
     * @param n the degree of the Sierpinski Gasket
     * @return the new Sierpinksi Gasket {@code Flame}
     * @throws IllegalArgumentException if {@code n} is not in range [3,inf)
     */
    public static Flame newSierpinksiNgon(int n) {
        if (n < 3)
            throw new IllegalArgumentException("n is not in range [3,inf): "+n);
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Calc constants
        final float a = 1.0f/3.0f;
        // Add transforms
        for (int i=0; i<n; i++) {
            // Calc constants
            final double b = 2.0*Math.PI*i/n + Math.PI/2.0;
            final double c = 2.0*Math.PI*(i-1)/n + Math.PI/2.0;
            // First transform
            transform = flame.addTransform();
            transform.setColorWeight(0.5f);
            transform.getColor().set(1, 1, 0);
            transform.getPreAffine().set(a, 0, 0, 0, a, 0);
            transform.getPreAffine().translate(
                    (float)Math.cos(b), 
                    (float)Math.sin(b));
            // Second transform
            transform = flame.addTransform();
            transform.setColorWeight(0.5f);
            transform.getColor().set(1, 0, 0);
            transform.getPreAffine().set(a, 0, 0, 0, a, 0);
            transform.getPreAffine().translate(
                    (float)((Math.cos(b) + Math.cos(c))/2.0), 
                    (float)((Math.sin(b) + Math.sin(c))/2.0));
        }
        // View
        flame.getView().set(0, 0, 0, 0.8f);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs new a Dragon Curve {@code Flame}.
     * 
     * @param t controls the angle and shape of the dragon curve
     * @return the new Dragon Curve {@code Flame}
     * @throws IllegalArgumentException if {@code t} is not in range [0,1]
     */
    public static Flame newDragonCurve(double t) {
        if(t<0 || 1<t)
            throw new IllegalArgumentException("t is not in range [0,1]: "+t);
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Calc constants
        final float b = (float)(1.0/Math.sqrt(2.0));
        // First Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().rotate(Math.toRadians(45));
        transform.getPreAffine().scale(b, b);
        // Second Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 0, 0);
        transform.getPreAffine().rotate(Math.toRadians(135-180*t));
        transform.getPreAffine().scale(b, b);
        transform.getPreAffine().translate((float)(1.0-0.5*t), (float)(0.5*t));
        // View
        flame.getView().set(0.25f, 0.5f, 0, 1);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs new a Golden Dragon Curve {@code Flame}.
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/heighway/goldenDragon.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/heighway/goldenDragon.htm</a>
     * 
     * @return the new Golden Dragon Curve {@code Flame}
     */
    public static Flame newGoldenDragonCurve() {
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // First Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 1);
        transform.getPreAffine().set(
                0.62367f, -0.40337f, 0.0f, 0.40337f, 0.62367f, 0.0f);
        // Second Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().set(
                -0.37633f, -0.40337f, 1.0f, 0.40337f, -0.37633f, 0.0f);
        // View
        flame.getView().set(0.4f, 0.2f, 0, 2);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Twindragon Curve (@code Flame).
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/heighway/twindragon.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/heighway/twindragon.htm</a>
     * 
     * @return the new Twindragon Curve {@code Flame}
     */
    public static Flame newTwindragonCurve() {
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // First Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 0, 1);
        transform.getPreAffine().set(0.5f, -0.5f, 0.0f, 0.5f, 0.5f, 0.0f);
        // Second Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().set(0.5f, -0.5f, 0.5f, 0.5f, 0.5f, -0.5f);
        // View
        flame.getView().set(0.5f, 0, 0, 1);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Twindragon Curve (@code Flame).
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/heighway/terdragon.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/heighway/terdragon.htm</a>
     * 
     * @return the new Twindragon Curve {@code Flame}
     */
    public static Flame newTerdragonCurve() {
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // First Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.6f);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().set(
                0.5f, 
                -0.28867513459481288225457439025098f, 
                0.0f, 
                0.28867513459481288225457439025098f, 
                0.5f, 
                0.0f);
        // Second Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.6f);
        transform.getColor().set(0, 1, 1);
        transform.getPreAffine().set(
                0.0f, 
                0.57735026918962576450914878050196f, 
                0.5f, 
                -0.57735026918962576450914878050196f, 
                0.0f, 
                0.28867513459481288225457439025098f);
        // Third Tranform
        transform = flame.addTransform();
        transform.setColorWeight(0.6f);
        transform.getColor().set(1, 0, 1);
        transform.getPreAffine().set(
                0.5f, 
                -0.28867513459481288225457439025098f, 
                0.5f, 
                0.28867513459481288225457439025098f, 
                0.5f, 
                -0.28867513459481288225457439025098f);
        // View
        flame.getView().set(0.5f, 0, 0, 2.0f);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Koch Curve (@code Flame).
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/kcurve/kcurve.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/kcurve/kcurve.htm</a>
     * 
     * @return the new Koch Curve {@code Flame}
     */
    public static Flame newKochCurve() {
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Calc constants
        float inv3 = 1.0f/3.0f;
        // First Transform
        transform = flame.addTransform();
        transform.getColor().set(1, 1, 1);
        transform.getPreAffine().scale(inv3, inv3);
        // Second Transform
        transform = flame.addTransform();
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().rotate(Math.toRadians(60));
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(inv3,0);
        // Third Transform
        transform = flame.addTransform();
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().rotate(Math.toRadians(-60));
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(0.5f, (float)(Math.sqrt(3)/6.0));
        // Fourth Transform
        transform = flame.addTransform();
        transform.getColor().set(1, 1, 1);
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(2*inv3,0);
        // View
        flame.getView().set(0.5f, 0, 0, 3);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Koch Snowflake (@code Flame).
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/ksnow/ksnow.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/ksnow/ksnow.htm</a>
     * 
     * @return the new Koch Snowflake {@code Flame}
     */
    public static Flame newKochSnowflake() {
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Calc constant
        float inv3 = 1.0f/3.0f;
        float invSqrt3 = (float)(1.0/Math.sqrt(3.0));
        // Transform #1
        transform = flame.addTransform();
        transform.setColorWeight(0);
        transform.getPreAffine().scale(invSqrt3, invSqrt3);
        transform.getPreAffine().rotate(Math.toRadians(30));
        // Transform #2
        transform = flame.addTransform();
        transform.setColorWeight(1);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(invSqrt3, inv3);
        // Transform #3
        transform = flame.addTransform();
        transform.setColorWeight(1);
        transform.getColor().set(1, 1, 1);
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(0, 2*inv3);
        // Transform #4
        transform = flame.addTransform();
        transform.setColorWeight(1);
        transform.getColor().set(1, 0, 1);
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(-invSqrt3, inv3);
        // Transform #5
        transform = flame.addTransform();
        transform.getColor().set(1, 0, 1);
        transform.setColorWeight(1);
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(-invSqrt3, -inv3);
        // Transform #6
        transform = flame.addTransform();
        transform.setColorWeight(1);
        transform.getColor().set(1, 0, 0);
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(0, -2*inv3);
        // Transform #7
        transform = flame.addTransform();
        transform.setColorWeight(1);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().scale(inv3, inv3);
        transform.getPreAffine().translate(invSqrt3, -inv3);
        // View
        flame.getView().set(0, 0, 0, 1);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Binary Tree (@code Flame).
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/pythagorean/symbinarytree.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/pythagorean/symbinarytree.htm</a>
     * 
     * @param t the angle between branches in radians
     * @param r the length of the first branch
     * @return the new Binary Tree {@code Flame}
     */
    public static Flame newBinaryTree(double t, double r) {
        if (!Double.isFinite(t))
            throw new IllegalArgumentException("t is not in range (-inf,inf): "+t);
        if (!Double.isFinite(r))
            throw new IllegalArgumentException("r is not in range (-inf,inf): "+r);
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Calc constants
        float sin = (float)(r*Math.sin(t));
        float cos = (float)(r*Math.cos(t));
        // First Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 1);
        transform.getPreAffine().set(cos, -sin, 0.0f, sin, cos, 1.0f);
        // Second Transform
        transform = flame.addTransform();
        transform.setColorWeight(0.5f);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().set(cos, sin, 0.0f, -sin, cos, 1.0f);
        // View
        flame.getView().set(0, 1, 0, 0.5f);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Barsley Fern (@code Flame).
     * <p>
     * <a href="https://en.wikipedia.org/wiki/Barnsley_fern">https://en.wikipedia.org/wiki/Barnsley_fern</a>
     * 
     * @return the new Barsley Fern {@code Flame}
     */
    public static Flame newBarnsleyFern() {
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Transform #1
        transform = flame.addTransform();
        transform.setColorWeight(0.75f);
        transform.getColor().set(1, 1, 0);
        transform.getPreAffine().set(0.0f, 0.0f, 0.0f, 0.0f, 0.16f, 0.0f);
        transform.setWeight(0.01f);
        // Transform #2
        transform = flame.addTransform();
        transform.getColor().set(0, 1, 0);
        transform.setColorWeight(0.2f);
        transform.getPreAffine().set(0.85f, 0.04f, 0.0f, -0.04f, 0.85f, 1.6f);
        transform.setWeight(0.85f);
        // Transform #3
        transform = flame.addTransform();
        transform.getColor().set(0, 1, 1);
        transform.setColorWeight(0.4f);
        transform.getPreAffine().set(0.2f, -0.26f, 0.0f, 0.23f, 0.22f, 1.6f);
        transform.setWeight(0.07f);
        // Transform #4
        transform = flame.addTransform();
        transform.getColor().set(0, 1, 1);
        transform.setColorWeight(0.4f);
        transform.getPreAffine().set(-0.15f, 0.28f, 0.0f, 0.26f, 0.24f, 0.44f);
        transform.setWeight(0.07f);
        // View
        flame.getView().set(1, 4, 0, 0.15f);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Pentadentrite (@code Flame).
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/pentaden/penta.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/pentaden/penta.htm</a>
     * 
     * @return the new Pentadentrite Fern {@code Flame}
     */
    public static Flame newPentadentrite() {
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Transform #1
        transform = flame.addTransform();
        transform.getColor().set(1, 0, 0);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(
                0.341f, -0.071f,  0.0f,
                0.071f,  0.341f,  0.0f);
        // Transform #2
        transform = flame.addTransform();
        transform.getColor().set(0, 1, 0);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(
                0.038f, -0.346f,  0.341f,
                0.346f,  0.038f,  0.071f);
        // Transform #3
        transform = flame.addTransform();
        transform.getColor().set(0, 0, 1);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(
                0.341f, -0.071f,  0.379f,
                0.071f,  0.341f,  0.418f);
        // Transform #4
        transform = flame.addTransform();
        transform.getColor().set(1, 1, 0);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(
               -0.234f,  0.258f,  0.720f,
               -0.258f, -0.234f,  0.489f);
        // Transform #5
        transform = flame.addTransform();
        transform.getColor().set(0, 1, 1);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(
                0.173f,  0.302f,  0.486f,
               -0.302f,  0.173f,  0.231f);
        // Transform #6
        transform = flame.addTransform();
        transform.getColor().set(1, 0, 1);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(
                0.341f, -0.071f,  0.659f,
                0.071f,  0.341f, -0.071f);
        // View
        flame.getView().set(0.5f, 0.25f, 0, 2);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Levy Dragon (@code Flame).
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/levy/levy.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/levy/levy.htm</a>
     * 
     * @return the new Levy Dragon {@code Flame}
     */
    public static Flame newLevyDragon() {
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Transform #1
        transform = flame.addTransform();
        transform.getColor().set(0, 0, 1);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(0.5f, -0.5f, 0.0f, 0.5f, 0.5f, 0.0f);
        // Transform #2
        transform = flame.addTransform();
        transform.getColor().set(1, 1, 0);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f);
        // View
        flame.getView().set(0.5f, 0.25f, 0, 1);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
        // Return the flame
        return flame;
    }
    
    /**
     * Constructs a new Pythagorean Tree (@code Flame).
     * <p>
     * <a href="http://ecademy.agnesscott.edu/~lriddle/ifs/pythagorean/pythTree.htm">http://ecademy.agnesscott.edu/~lriddle/ifs/pythagorean/pythTree.htm</a>
     * 
     * @param a the angle in radians
     * @return the new Pythagorean Tree {@code Flame}
     * @throws IllegalArgumentException if {@code a} is not in range (0,PI)
     */
    public static Flame newPythagoreanTree(double a) {
        if (!(0 < a && a < Math.PI))
            throw new IllegalArgumentException("a is not in range (0,PI): "+a);
        // Construct the flame
        Flame flame = new Flame();
        // Final Transform
        Transform transform = flame.addTransform();
        transform.setColorWeight(0.0f);
        // Calc constants
        float cos = (float)Math.cos(a);
        float sin = (float)Math.sin(a);
        // Transform #1
        transform = flame.addTransform();
        transform.getColor().set(0, 0.2f, 1);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(
                cos*cos, -cos*sin, 0,
                cos*sin,  cos*cos, 1);
        // Transform #2
        transform = flame.addTransform();
        transform.getColor().set(0, 1, 0);
        transform.setColorWeight(0.5f);
        transform.getPreAffine().set(
                sin*sin, cos*sin, cos*cos,
               -cos*sin, sin*sin, cos*sin+1);
        // View
        flame.getView().set(1.5f, 2.0f, 0, 0.25f);
        // Coloration
        flame.getColoration().set(2, 2, 1);
        // Backgorund Color
        flame.getBackground().set(0, 0, 0, 1);
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
}
