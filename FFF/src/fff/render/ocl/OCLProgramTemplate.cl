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

/** Optional Kernel Flags
 * #define USE_JITTER
 * #define USE_VARIATIONS
 * #define USE_POST_AFFINES
 * #define USE_FINAL_TRANSFORM
 */

___FLAGS___
#define PI (3.14159265359f)
#define AFFINE_A (AFFINE[0].x)
#define AFFINE_B (AFFINE[1].x)
#define AFFINE_C (AFFINE[2].x)
#define AFFINE_D (AFFINE[1].y)
#define AFFINE_E (AFFINE[0].y)
#define AFFINE_F (AFFINE[2].y)
#define RADIUS2 (x*x+y*y)
#define RADIUS (hypot(x,y))
#define THETA (atan2(x,y))
#define PHI (atan2(y,x))
#define OMEGA (0.5<RAND?0.0:PI)
#define DELTA (0.5<RAND?-1.0:1.0)
#define PSI (RAND)
#define RAND (randFloat(RSTATE))

#define double float

// Adapted from http://cas.ee.ic.ac.uk/people/dt10/research/rngs-gpu-mwc64x.html
float randFloat(global uint2* rstate) {
    uint x = rstate->x, y = rstate->y;
    uint h = mul_hi(x,y);
    x = x*4294883355U + y;
    y = h + (x<(y));
    (*rstate) = (uint2)(x,y);
    return (x^y) * 2.32830643e-10f;
}

#ifdef USE_VARIATIONS
float2 applyVariations(
    global const float*  VARIATIONS,
    global const float*  PARAMETERS,
    global const float2* AFFINE,
    global       uint2*  RSTATE,
           const float   x,
           const float   y)
{
    float2 RESULT = 0;
    float X, Y;
    
    ___VARIATIONS___
    return RESULT;
}
#endif

kernel void initKernel(
    global       uint2*  rstate,          //  0
    global       float2* points,          //  1
    global       float4* colors)          //  2
{
    int myIndex = get_global_id(0);
    global uint2* rng = &(rstate[myIndex]);
    points[myIndex] = (float2)(randFloat(rng),randFloat(rng))*2.0f - 1.0f;
    colors[myIndex] = (float4)(randFloat(rng),randFloat(rng),randFloat(rng),randFloat(rng));
}


kernel void warmKernel(
           const int     numTransforms,   //  0
    global const float*  xformWeight,     //  1
    global const float*  xformCmixes,     //  2
    global const float4* xformColors,     //  3
    global const float2* xformAffine,     //  4
    global const float*  xformVariations, //  5
    global const float*  xformParameters, //  6
    global       uint2*  rstate,          //  7
    global       float2* points,          //  8
    global       float4* colors)          //  9
{
    int myIndex = get_global_id(0); 
    global uint2* rng = &(rstate[myIndex]);
    float2 p = points[myIndex];
    float4 c = colors[myIndex];

    int index;
    float rand; 

    for (int i=0; i<10; i++) {
        // Select a Transform
        rand = randFloat(rng);
        for(index=1; xformWeight[index]<=rand && index<numTransforms-1; ++index);

        // Perform the Transform
        p = mad(p,xformAffine[index*6+0],mad(p.yx,xformAffine[index*6+1],xformAffine[index*6+2]));
#ifdef USE_VARIATIONS
        p = applyVariations(&(xformVariations[index*___NUM_VARIATIONS___]),&(xformParameters[index*___NUM_PARAMETERS___]),&(xformAffine[index*6]),rng,p.x,p.y);
#endif
#ifdef USE_POST_AFFINES
        p = mad(p,xformAffine[index*6+3],mad(p.yx,xformAffine[index*6+4],xformAffine[index*6+5]));
#endif
        c = mix(c,xformColors[index],xformCmixes[index]);
    }

    // Store the Results
    points[myIndex] = p;
    colors[myIndex] = c;
}


kernel void plotKernel(
           const float   width,           //  0
           const float   height,          //  1
           const int     numTransforms,   //  2
    global const float*  xformWeight,     //  3
    global const float*  xformCmixes,     //  4
    global const float4* xformColors,     //  5
    global const float2* xformAffine,     //  6
    global const float*  xformVariations, //  7
    global const float*  xformParameters, //  8
    global const float2* flameViewAffine, //  9
    global       uint2*  rstate,          // 10
    global       float2* points,          // 11
    global       float4* colors,          // 12
    global       float4* histogram,       // 13
    global       int2*   hitcounts,       // 14
           const int     batchSize)       // 15
{
    int myIndex = get_global_id(0); 
    global uint2* rng = &(rstate[myIndex]);
    float2 pf, p = points[myIndex];
    float4 cf, c = colors[myIndex];

    // Select a Transform
    int index;
    float rand;
    int batch;
    for (batch = 0; batch<batchSize; batch++) {
        rand = randFloat(rng);
        for(index=1; xformWeight[index]<=rand && index<numTransforms-1; ++index);

        // Perform the Transform
        p = mad(p,xformAffine[index*6+0],mad(p.yx,xformAffine[index*6+1],xformAffine[index*6+2]));
#ifdef USE_VARIATIONS
        p = applyVariations(&(xformVariations[index*___NUM_VARIATIONS___]),&(xformParameters[index*___NUM_PARAMETERS___]),&(xformAffine[index*6]),rng,p.x,p.y);
#endif
#ifdef USE_POST_AFFINES
        p = mad(p,xformAffine[index*6+3],mad(p.yx,xformAffine[index*6+4],xformAffine[index*6+5]));
#endif
        c = mix(c,xformColors[index],xformCmixes[index]);

        // Prepare the 'final' point and color
        pf = p;
        cf = c;

        // Final Transform
#ifdef USE_FINAL_TRANSFORM
        pf = mad(pf,xformAffine[0],mad(pf.yx,xformAffine[1],xformAffine[2]));
#ifdef USE_VARIATIONS
        pf = applyVariations(xformVariations,xformParameters,xformAffine,rng,pf.x,pf.y);
#endif
#ifdef USE_POST_AFFINES
        pf = mad(pf,xformAffine[3],mad(pf.yx,xformAffine[4],xformAffine[5]));
#endif
        cf = mix(cf,*colors,*xformCmixes);
#endif

        // View Transform
        pf = mad(pf,flameViewAffine[0],mad(pf.yx,flameViewAffine[1],flameViewAffine[2]));

        // Jitter (opftional)
#ifdef USE_JITTER
        pf += (float2)(randFloat(rng),randFloat(rng))-0.5f;
#endif

        // Check to see if the image is in bounds
        if (0<=pf.x && pf.x<width && 0<=pf.y && pf.y<height) {

            // Get the pixel's index
            //index = mad((int)pf.y,width,pf.x); // causes index out of bounds exceptions
            index = ((int)pf.y)*width + (int)(pf.x);
            
            // Increment the hit counters
            *hitcounts += (int2)(1,histogram[index].x<1.0f);

            // Add the color to the histogram
            histogram[index] += cf;
        }
    }

    // Store the Results
    points[myIndex] = p;
    colors[myIndex] = c;
}




float4 colorToneMap( float4 pix,
                     float  scaleConstant,
        global const float* coloration) {
    if (pix.x < 1.0f)
        return 0;
    float q = coloration[0] * native_log10(1.0f + pix.x*scaleConstant);
    pix *= q/pix.x;
    float z = native_powr(q, coloration[1]);
    pix = mix(native_powr(pix, coloration[1]), pix*(z/q), coloration[2]);
    pix.x = z;
    return clamp(pix, 0.0f, 1.0f);
}

int toRaster(float4 pix, float4 b) {
    // Apha Blend Background Color
    float z = pix.x + b.x - pix.x*b.x;
    pix = mad(pix.x/z, pix-b, b);
    pix.x = z;
    pix *= 255.0f;
    return ((int)(pix.x)<<24) | ((int)(pix.y)<<16) | ((int)(pix.z)<<8) | (int)(pix.w);
}


kernel void quckFinishKernel(
           const float   scaleConstant, // 0 
    global const float*  coloration,    // 1
    global const float4* background,    // 2
    global const float4* histogram,     // 3
    global       int*    raster)        // 4
{
    int index = get_global_id(0);
    raster[index] = toRaster(colorToneMap(histogram[index], scaleConstant, coloration), *background);
}

kernel void blurFinishKernel1(
           const float   scaleConstant, // 0
    global const float*  coloration,    // 1
    global       float4* histogram,     // 2
    global       float4* preraster)     // 3
{
    int index = get_global_id(0);
    preraster[index] = colorToneMap(histogram[index], scaleConstant, coloration);
}

kernel void blurFinishKernel2(
           const int     width,         // 0
           const int     height,        // 1
    global const float*  blurParams,    // 2
    global const float4* background,    // 3
    global const float4* histogram,     // 4
    global const float4* preraster,     // 5
    global       int*    raster)        // 6
{
    int index = get_global_id(0);

    float4 pix = 0;
    int kernelWidth = (int)clamp(blurParams[2]/native_powr(histogram[index].x,blurParams[0]), blurParams[1], blurParams[2]);
    int xMin = max(index%width-kernelWidth, 0);
    int xMax = min(index%width+kernelWidth, width-1);
    int yMin = max(index/width-kernelWidth, 0);
    int yMax = min(index/width+kernelWidth, height-1);
    for (int X=xMin; X<=xMax; X++)
        for (int Y=yMin; Y<=yMax; Y++)
            pix += preraster[Y*width+X];
    pix /= (xMax-xMin+1)*(yMax-yMin+1);

    raster[index] = toRaster(pix,*background);
}

/*
kernel void colorKernel(
           const float   scaleConstant,  //  0
    global const float4* background,     //  1
    global const float*  coloration,     //  2
           const float   kernelWidthMin, //  3
           const float   kernelWidthMax, //  4
           const float   kernelAlpha,    //  5
           const int     width,          //  6
           const int     height,         //  7
    global       float4* histogram,      //  8
    global       float4* preraster,      //  9
    global       int*    raster)         // 10
{
    int index = get_global_id(0);
    float4 pix = histogram[index];
    float z = pix.x;
    if (z != 0.0f) {
        // Coloration: Log Scale and Gamma
        float brightness = coloration[0];
        float invGamma = coloration[1];
        float vibrancy = coloration[2];
        float q = brightness * native_log10(1.0f + z*scaleConstant);
        pix *= q/z;
        float a = native_powr(q, invGamma);
        pix = mix(native_powr(pix, invGamma), pix*(a/q), vibrancy);
        pix.x = a;
        pix = clamp(pix, 0.0f, 1.0f);
        preraster[index] = pix;
    } else {
        preraster[index] = 0;
    }
    
    barrier(CLK_GLOBAL_MEM_FENCE);
    
    pix = 0;
    int kernelWidth = (int)clamp(kernelWidthMax/native_powr(z,kernelAlpha), kernelWidthMin, kernelWidthMax);
    int xMin = max(index%width-kernelWidth, 0);
    int xMax = min(index%width+kernelWidth, width-1);
    int yMin = max(index/width-kernelWidth, 0);
    int yMax = min(index/width+kernelWidth, height-1);
    for (int X=xMin; X<=xMax; X++)
        for (int Y=yMin; Y<=yMax; Y++)
            pix += preraster[Y*width+X];
    pix /= (xMax-xMin+1)*(yMax-yMin+1);
    
    // Apha Blend Background Color
    float4 b = *background;
    z = pix.x + b.x - pix.x*b.x;
    pix = mad(pix.x/z, pix-b, b);
    pix.x = z;
    
    // Pack and store the results
    pix *= 255.0f;
    raster[index] = ((int)(pix.x)<<24) | ((int)(pix.y)<<16) | ((int)(pix.z)<<8) | (int)(pix.w);
}





kernel void colorKernel(
           const float   scaleConstant,  //  0
    global const float4* background,     //  1
    global const float*  coloration,     //  2
           const float   kernelWidthMin, //  3
           const float   kernelWidthMax, //  4
           const float   kernelAlpha,    //  5
           const int     width,          //  6
           const int     height,         //  7
    global       float4* histogram,      //  8
    global       float4* preraster,      //  9
    global       int*    raster)         // 10
{
    int index = get_global_id(0);
    float4 pix = histogram[index];
    float z = pix.x;
    if (z != 0.0f) {
        // Coloration: Log Scale and Gamma
        float brightness = coloration[0];
        float invGamma = coloration[1];
        float vibrancy = coloration[2];
        float q = brightness * native_log10(1.0f + z*scaleConstant);
        pix *= q/z;
        float a = native_powr(q, invGamma);
        pix = mix(native_powr(pix, invGamma), pix*(a/q), vibrancy);
        pix.x = a;
        pix = clamp(pix, 0.0f, 1.0f);
        preraster[index] = pix;
    } else {
        preraster[index] = 0;
    }
    
    barrier(CLK_GLOBAL_MEM_FENCE);
    
    pix = 0;
    int kernelWidth = (int)clamp(kernelWidthMax/native_powr(z,kernelAlpha), kernelWidthMin, kernelWidthMax);
    int xMin = max(index%width-kernelWidth, 0);
    int xMax = min(index%width+kernelWidth, width-1);
    int yMin = max(index/width-kernelWidth, 0);
    int yMax = min(index/width+kernelWidth, height-1);
    for (int X=xMin; X<=xMax; X++)
        for (int Y=yMin; Y<=yMax; Y++)
            pix += preraster[Y*width+X];
    pix /= (xMax-xMin+1)*(yMax-yMin+1);
    
    // Apha Blend Background Color
    float4 b = *background;
    z = pix.x + b.x - pix.x*b.x;
    pix = mad(pix.x/z, pix-b, b);
    pix.x = z;
    
    // Pack and store the results
    pix *= 255.0f;
    raster[index] = ((int)(pix.x)<<24) | ((int)(pix.y)<<16) | ((int)(pix.z)<<8) | (int)(pix.w);
}


kernel void previewKernel(
           const float   scaleConstant, // 0 
    global const float4* background,    // 1
    global const float*  coloration,    // 2
    global       float4* histogram,     // 3
    global       int*    raster)        // 4
{
    int index = get_global_id(0);
    float4 b = *background;
    float4 pix = histogram[index];
    if (pix.x < 1.0f) {
       pix = b;
    } else {
        // Coloration: Log Scale and Gamma
        float brightness = coloration[0];
        float invGamma = coloration[1];
        float vibrancy = coloration[2];
        float q = brightness * native_log10(1.0f + pix.x*scaleConstant);
        pix *= q/pix.x;
        float z = native_powr(q, invGamma);
        pix = mix(native_powr(pix, invGamma), pix*(z/q), vibrancy);
        pix.x = z;
        pix = clamp(pix, 0.0f, 1.0f);
        // Apha Blend Background Color
        z = pix.x + b.x - pix.x*b.x;
        pix = mad(pix.x/z, pix-b, b);
        pix.x = z;
    }

    // Pack and store the results
    pix *= 255.0f;
    raster[index] = ((int)(pix.x)<<24) | ((int)(pix.y)<<16) | ((int)(pix.z)<<8) | (int)(pix.w);
}

*/