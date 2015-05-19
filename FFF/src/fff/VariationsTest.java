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

package fff;

import fff.flame.Flame;
import fff.flame.Transform;
import fff.flame.VariationDefinition;
import fff.render.FlameRenderer;
import fff.render.FlameRendererListener;
import fff.render.FlameRendererSettings;
import fff.render.FlameRendererTask;
import fff.render.FlameRendererTaskSingle;
import fff.render.ocl.FlameRendererOpenCL;
import fff.render.ocl.FlameRendererOpenCL.DeviceType;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author Jeremiah N. Hankins
 */
public class VariationsTest {
    
    public static void main(String[] args) {
        FlameRenderer renderer = new FlameRendererOpenCL(DeviceType.ALL);

        FlameRendererSettings settings = new FlameRendererSettings();
        settings.setUseFinalTransform(false);
        settings.setUsePostAffines(false);
        
        settings.setMaxTime(200);
        
        
//        Map<String,VariationDefinition> map = VariationDefinition.DEFAULT_MAP;
//        for (VariationDefinition var : map.values()) {
//            String code = var.getCode();
//            
//            ArrayList<Character> list = new ArrayList();
//            for (char c: code.toCharArray())
//                list.add(c);
//            
//            int offset = 0;
//            
//            int idx = 0;
//            idx = code.indexOf('.', idx);
//            while (idx != -1) {
//                if (idx+1 < code.length()) {
//                    if ('0'<=code.charAt(idx+1) && code.charAt(idx+1)<'9') {
//                        list.add(offset+idx+2, 'f');
//                        offset++;
//                    }
//                }
//                idx = code.indexOf('.', idx);
//            }
//            String s = "";
//            for (char c : list)
//                s += c;
//            
//        }
        
//        int i = 0;
//        for (VariationDefinition var : map.values()) {
//            System.out.print("public static final VariationDefinition "+ var.getName().toUpperCase()+";");
//            for(int j="HANDKERCHIEF".length()-var.getName().length(); j>0; j--)
//                System.out.print(" ");
//            System.out.println(" // "+(i<10?" ":"")+(i++));
//        }
//        System.out.println();
//        System.out.println("public static final Map<String,VariationDefinition> DEFAULT_MAP;");
//        System.out.println();
//        
//        i = 0;
//        System.out.println("static {");
//        System.out.println("    Map<String,Float> parameters = new TreeMap()");
//        
//        for (VariationDefinition var : map.values()) {
//            System.out.println("    ");
//            System.out.println("    // "+(i<10?" ":"")+(i++)+" "+var.getName());
//            if (!var.getParameters().isEmpty())
//                System.out.println("    parameters.clear();");
//            for (Map.Entry<String,Float> e: var.getParameters().entrySet()) {
//                System.out.println("    parameters.put(\""+e.getKey()+"\", "+
//                        String.format("%.1ff", e.getValue())+");");
//            }
//            System.out.println("    "+var.getName().toUpperCase()+" = new VariationDefinition(\""+var.getName()+"\", \"\"");
//            
//            
//            String code = var.getCode();
//            
//            ArrayList<Character> list = new ArrayList();
//            for (char c: code.toCharArray())
//                list.add(c);
//            
//            int offset = 0;
//            
//            int idx = 0;
//            idx = code.indexOf('.', idx);
//            while (idx != -1) {
//                if (idx+1 < code.length()) {
//                    if ('0'<=code.charAt(idx+1) && code.charAt(idx+1)<'9') {
//                        list.add(offset+idx+2, 'f');
//                        offset++;
//                    }
//                }
//                idx = code.indexOf('.', idx+1);
//            }
//            
//            StringBuilder builder = new StringBuilder();
//            for (char c : list)
//                builder.append(c);
//            String s = builder.toString();
//            
//            
//            for (String line: s.split("\n"))
//                System.out.println("        + \""+line+"\\n\"");
//            if (!var.getParameters().isEmpty())
//                System.out.println("        , parameters);");
//            else
//                System.out.println("        , null);");
//        }
//        
//        System.out.println("    ");
//        System.out.println("    Map<String,VariationDefinition> map = new LinkedHashMap();");
//        
//        for (VariationDefinition var : map.values()) {
//            System.out.println("    map.put(\""+var.getName()+"\", "+var.getName().toUpperCase()+");");
//        }
//        System.out.println("    ");
//        System.out.println("    DEFAULT_MAP = Collections.unmodifiableMap(map);");
//        
//        System.out.println("}");
        

//        FlameRendererListener listener = new FlameRendererListener() {
//            @Override
//            public void flameImageEvent(FlameRendererTask task, Flame flame, BufferedImage image, double quality, double points, double elapTime, boolean isFinished) {
//                points /= 1e7;
//                System.out.println(String.format("Drawn %.2fM dots at %.2fM dots/sec for quality of %.2f.", points, points/elapTime, quality));
//                if (isFinished) {
//                    try {
//                        ImageIO.write(image, "png", new File("test.png"));
//                    } catch (IOException ex) {
//                        ex.printStackTrace(System.err);
//                    }
//                    System.exit(0);
//                }
//            }
//        };

        
        
        for (VariationDefinition variation: VariationDefinition.DEFAULT_MAP.values()) {
//            System.out.println("name: "+variation.getName());
            Flame flame = Flame.newSierpinski();
            Transform xform = flame.getTransform(0);
            if (variation!=VariationDefinition.LINEAR) {
                xform.addVariation(variation);
                xform.removeVariation(VariationDefinition.LINEAR);
            }
            FlameRendererTaskSingle task = new FlameRendererTaskSingle(flame, settings, new FlameRendererListener() {
                @Override
                public void flameImageEvent(FlameRendererTask task, Flame flame, BufferedImage image, double quality, double points, double elapsedTime, boolean isFinished) {
                    if (isFinished) {
                        System.out.println(flame.getTransform(0).getVariations().iterator().next().getDefinition().getName());
                    }
                }
            });
            renderer.enqueueTask(task);
        }
    }
}
