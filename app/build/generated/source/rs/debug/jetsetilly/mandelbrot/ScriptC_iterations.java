/*
 * Copyright (C) 2011-2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is auto-generated. DO NOT MODIFY!
 * The source Renderscript file: /home/steve/AndroidStudioProjects/Mandelbrot/app/src/main/rs/iterations.rs
 */

package jetsetilly.mandelbrot;

import android.support.v8.renderscript.*;
import jetsetilly.mandelbrot.iterationsBitCode;

/**
 * @hide
 */
public class ScriptC_iterations extends ScriptC {
    private static final String __rs_resource_name = "iterations";
    // Constructor
    public  ScriptC_iterations(RenderScript rs) {
        super(rs,
              __rs_resource_name,
              iterationsBitCode.getBitCode32(),
              iterationsBitCode.getBitCode64());
        __I32 = Element.I32(rs);
        __F64 = Element.F64(rs);
    }

    private Element __F64;
    private Element __I32;
    private FieldPacker __rs_fp_F64;
    private FieldPacker __rs_fp_I32;
    private final static int mExportVarIdx_canvas_height = 0;
    private int mExportVar_canvas_height;
    public synchronized void set_canvas_height(int v) {
        setVar(mExportVarIdx_canvas_height, v);
        mExportVar_canvas_height = v;
    }

    public int get_canvas_height() {
        return mExportVar_canvas_height;
    }

    public Script.FieldID getFieldID_canvas_height() {
        return createFieldID(mExportVarIdx_canvas_height, null);
    }

    private final static int mExportVarIdx_canvas_width = 1;
    private int mExportVar_canvas_width;
    public synchronized void set_canvas_width(int v) {
        setVar(mExportVarIdx_canvas_width, v);
        mExportVar_canvas_width = v;
    }

    public int get_canvas_width() {
        return mExportVar_canvas_width;
    }

    public Script.FieldID getFieldID_canvas_width() {
        return createFieldID(mExportVarIdx_canvas_width, null);
    }

    private final static int mExportVarIdx_max_iterations = 2;
    private int mExportVar_max_iterations;
    public synchronized void set_max_iterations(int v) {
        setVar(mExportVarIdx_max_iterations, v);
        mExportVar_max_iterations = v;
    }

    public int get_max_iterations() {
        return mExportVar_max_iterations;
    }

    public Script.FieldID getFieldID_max_iterations() {
        return createFieldID(mExportVarIdx_max_iterations, null);
    }

    private final static int mExportVarIdx_null_iteration = 3;
    private int mExportVar_null_iteration;
    public synchronized void set_null_iteration(int v) {
        setVar(mExportVarIdx_null_iteration, v);
        mExportVar_null_iteration = v;
    }

    public int get_null_iteration() {
        return mExportVar_null_iteration;
    }

    public Script.FieldID getFieldID_null_iteration() {
        return createFieldID(mExportVarIdx_null_iteration, null);
    }

    private final static int mExportVarIdx_bailout_value = 4;
    private double mExportVar_bailout_value;
    public synchronized void set_bailout_value(double v) {
        setVar(mExportVarIdx_bailout_value, v);
        mExportVar_bailout_value = v;
    }

    public double get_bailout_value() {
        return mExportVar_bailout_value;
    }

    public Script.FieldID getFieldID_bailout_value() {
        return createFieldID(mExportVarIdx_bailout_value, null);
    }

    private final static int mExportVarIdx_imaginary_lower = 5;
    private double mExportVar_imaginary_lower;
    public synchronized void set_imaginary_lower(double v) {
        setVar(mExportVarIdx_imaginary_lower, v);
        mExportVar_imaginary_lower = v;
    }

    public double get_imaginary_lower() {
        return mExportVar_imaginary_lower;
    }

    public Script.FieldID getFieldID_imaginary_lower() {
        return createFieldID(mExportVarIdx_imaginary_lower, null);
    }

    private final static int mExportVarIdx_imaginary_upper = 6;
    private double mExportVar_imaginary_upper;
    public synchronized void set_imaginary_upper(double v) {
        setVar(mExportVarIdx_imaginary_upper, v);
        mExportVar_imaginary_upper = v;
    }

    public double get_imaginary_upper() {
        return mExportVar_imaginary_upper;
    }

    public Script.FieldID getFieldID_imaginary_upper() {
        return createFieldID(mExportVarIdx_imaginary_upper, null);
    }

    private final static int mExportVarIdx_real_left = 7;
    private double mExportVar_real_left;
    public synchronized void set_real_left(double v) {
        setVar(mExportVarIdx_real_left, v);
        mExportVar_real_left = v;
    }

    public double get_real_left() {
        return mExportVar_real_left;
    }

    public Script.FieldID getFieldID_real_left() {
        return createFieldID(mExportVarIdx_real_left, null);
    }

    private final static int mExportVarIdx_real_right = 8;
    private double mExportVar_real_right;
    public synchronized void set_real_right(double v) {
        setVar(mExportVarIdx_real_right, v);
        mExportVar_real_right = v;
    }

    public double get_real_right() {
        return mExportVar_real_right;
    }

    public Script.FieldID getFieldID_real_right() {
        return createFieldID(mExportVarIdx_real_right, null);
    }

    private final static int mExportVarIdx_pixel_scale = 9;
    private double mExportVar_pixel_scale;
    public synchronized void set_pixel_scale(double v) {
        setVar(mExportVarIdx_pixel_scale, v);
        mExportVar_pixel_scale = v;
    }

    public double get_pixel_scale() {
        return mExportVar_pixel_scale;
    }

    public Script.FieldID getFieldID_pixel_scale() {
        return createFieldID(mExportVarIdx_pixel_scale, null);
    }

    private final static int mExportVarIdx_render_left = 10;
    private int mExportVar_render_left;
    public synchronized void set_render_left(int v) {
        setVar(mExportVarIdx_render_left, v);
        mExportVar_render_left = v;
    }

    public int get_render_left() {
        return mExportVar_render_left;
    }

    public Script.FieldID getFieldID_render_left() {
        return createFieldID(mExportVarIdx_render_left, null);
    }

    private final static int mExportVarIdx_render_right = 11;
    private int mExportVar_render_right;
    public synchronized void set_render_right(int v) {
        setVar(mExportVarIdx_render_right, v);
        mExportVar_render_right = v;
    }

    public int get_render_right() {
        return mExportVar_render_right;
    }

    public Script.FieldID getFieldID_render_right() {
        return createFieldID(mExportVarIdx_render_right, null);
    }

    private final static int mExportVarIdx_render_top = 12;
    private int mExportVar_render_top;
    public synchronized void set_render_top(int v) {
        setVar(mExportVarIdx_render_top, v);
        mExportVar_render_top = v;
    }

    public int get_render_top() {
        return mExportVar_render_top;
    }

    public Script.FieldID getFieldID_render_top() {
        return createFieldID(mExportVarIdx_render_top, null);
    }

    private final static int mExportVarIdx_render_bottom = 13;
    private int mExportVar_render_bottom;
    public synchronized void set_render_bottom(int v) {
        setVar(mExportVarIdx_render_bottom, v);
        mExportVar_render_bottom = v;
    }

    public int get_render_bottom() {
        return mExportVar_render_bottom;
    }

    public Script.FieldID getFieldID_render_bottom() {
        return createFieldID(mExportVarIdx_render_bottom, null);
    }

    //private final static int mExportForEachIdx_root = 0;
    private final static int mExportForEachIdx_pixel = 1;
    public Script.KernelID getKernelID_pixel() {
        return createKernelID(mExportForEachIdx_pixel, 42, null, null);
    }

    public void forEach_pixel(Allocation aout) {
        forEach_pixel(aout, null);
    }

    public void forEach_pixel(Allocation aout, Script.LaunchOptions sc) {
        // check aout
        if (!aout.getType().getElement().isCompatible(__I32)) {
            throw new RSRuntimeException("Type mismatch with I32!");
        }
        forEach(mExportForEachIdx_pixel, (Allocation) null, aout, null, sc);
    }

}

