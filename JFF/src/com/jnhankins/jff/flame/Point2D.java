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

/**
 * Point2D is a simple container class pairing an x and y coordinate.
 * 
 * @author Jeremiah N. Hankins
 */
public class Point2D {
    public double x;
    public double y;
   
    /**
     * Constructs a new Point2D object with x and y coordinate set to zero.
     */
    public Point2D() {
        x = 0;
        y = 0;
    }
    
    /**
     * Constructs a new Point2D object with the given x and y coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Constructs a new Point2D object with the same x and y coordinates as the 
     * given point.
     * @param point the point to copy
     */
    public Point2D(Point2D point) {
        x = point.x;
        y = point.y;
    }
    
    /**
     * Sets the x and y coordinates.
     * @param x the x coordinate
     * @param y the y coordinate
     * @return return this {@code Point2D}
     */
    public Point2D set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    /**
     * Copies the x and y coordinates of the given point.
     * @param point the point to copy
     * @return return this {@code Point2D}
     */
    public Point2D set(Point2D point) {
        x = point.x;
        y = point.y;
        return this;
    }
    
    /**
     * Adds the given values to the x and y coordinates respectively.
     * @param dx the amount to sum with the x coordinate
     * @param dy the amount to sum with the y coordinate
     * @return return this {@code Point2D}
     */
    public Point2D sum(double dx, double dy) {
        x += dx;
        y += dy;
        return this;
    }
    
    /**
     * Sums this point with the given point component-wise.
     * @param point the point to add
     * @return return this {@code Point2D}
     */
    public Point2D sum(Point2D point) {
        x += point.x;
        y += point.y;
        return this;
    }
    
    /**
     * Scales the x and y coordinates by the given value.
     * @param scale the amount to scale
     * @return return this {@code Point2D}
     */
    public Point2D scale(double scale) {
        x *= scale;
        y *= scale;
        return this;
    }
    
    /**
     * Rotates the x and y coordinates by the amount specified.
     * @param theta the amount to rotate in radians
     * @return return this {@code Point2D}
     */
    public Point2D rotate(double theta) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        set(x*cos + y*sin, x*sin - y*cos);
        return this;
    }
    
    /**
     * Returns the distance to the given point.
     * @param point the other point
     * @return the distance
     */
    public double dist(Point2D point) {
        return dist(point.x, point.y);
    }
    
    /**
     * Returns the distance to the point at (x,y).
     * @param x the x-coordinate of the other point
     * @param y the y-coordinate of the other point
     * @return the distance
     */
    public double dist(double x, double y) {
        return Math.sqrt((this.x-x)*(this.x-x)+(this.y-y)*(this.y-y));
    }
    
    /**
     * Returns atan2(this.y, this.x).
     * @return the theta angle relative to the origin
     */
    public double theta() {
        return Math.atan2(this.y, this.x);
    }
    
    /**
     * Returns atan2(this.y-point.y, this.x-point.x).
     * @param point the origin point
     * @return the theta angle
     */
    public double theta(Point2D point) {
        return theta(point.x, point.y);
    }
    
    /**
     * Returns atan2(this.y-y, this.x-x).
     * @param x the x-coordinate of the origin
     * @param y the y-coordinate of the origin
     * @return the theta angle
     */
    public double theta(double x, double y) {
        return Math.atan2(this.y-y, this.x-x);
    }
    
    /**
     * Returns a string representation of this point.
     * @return a string representation of this point
     */
    @Override
    public String toString() {
        return "Point2D("+x+", "+y+")";
    }
}
