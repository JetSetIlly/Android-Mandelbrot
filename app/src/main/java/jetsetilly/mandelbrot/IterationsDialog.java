package jetsetilly.mandelbrot;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import jetsetilly.mandelbrot.Mandelbrot.Settings;

public class IterationsDialog extends DialogFragment {
    private Settings mandelbrot_settings = Settings.getInstance();

    private boolean dirty_settings = false;
    private int rendered_iterations;
    private int iteration_min;
    private int iteration_max;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.settings_max_iterations_label);
        builder.setView(createView());

        builder.setPositiveButton(R.string.dialog_max_iter_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dirty_settings) {
                    MainActivity.render_canvas.startRender();
                }
            }
        }).setNeutralButton(R.string.dialog_max_iter_more, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Activity curr_activity = getActivity();

                Intent settings_intent = new Intent(curr_activity, SettingsActivity.class);
                settings_intent.putExtra(getString(R.string.settings_intent_dirty_settings), dirty_settings);
                settings_intent.putExtra(getString(R.string.settings_intent_rendered_iterations), rendered_iterations);

                startActivity(settings_intent);
                curr_activity.overridePendingTransition(R.anim.push_left_fade_in, R.anim.push_left_fade_out);
            }
        }).setNegativeButton(R.string.dialog_max_iter_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mandelbrot_settings.max_iterations = rendered_iterations;
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    private View createView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_max_iterations, null);

        final SeekBar seek_bar =  (SeekBar) view.findViewById(R.id.seek_iterations);
        final TextView iterations= (TextView) view.findViewById(R.id.iterations);

        iteration_min = (int) (mandelbrot_settings.max_iterations * 0.25);
        iteration_max = (int) (mandelbrot_settings.max_iterations * 1.5);
        seek_bar.setMax(iteration_max);
        seek_bar.setProgress(mandelbrot_settings.max_iterations - iteration_min);
        iterations.setText("" + (seek_bar.getProgress() + iteration_min));

        // record initial iterations value in case user cancels
        rendered_iterations = mandelbrot_settings.max_iterations;

        seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                iterations.setText("" + (seek_bar.getProgress() + iteration_min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mandelbrot_settings.max_iterations = Integer.parseInt(iterations.getText().toString());
                dirty_settings = rendered_iterations != mandelbrot_settings.max_iterations;
            }
        });

        return view;
    }
}
