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

import jetsetilly.mandelbrot.Widgets.IterationsSlider;

public class IterationsDialog extends DialogFragment {
    private IterationsSlider iterations;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Iteration_Dialog_Theme);
        builder.setTitle(R.string.settings_max_iterations_label);
        builder.setView(createView());

        builder.setPositiveButton(R.string.dialog_max_iteration_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (iterations.fixate()) {
                    MainActivity.render_canvas.startRender();
                }
            }
        }).setNeutralButton(R.string.dialog_max_iteration_more, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Activity curr_activity = getActivity();

                Intent settings_intent = new Intent(curr_activity, SettingsActivity.class);
                settings_intent.putExtra(SettingsActivity.ITERATIONS_VALUE_INTENT, iterations.getInteger());

                startActivity(settings_intent);
                curr_activity.overridePendingTransition(R.anim.from_left_nofade, R.anim.from_left_fade_out);
            }
        }).setNegativeButton(R.string.dialog_max_iteration_cancel, null);

        // Create the AlertDialog object and return it
        return builder.create();
    }

    private View createView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_max_iterations, null);

        iterations = (IterationsSlider) view.findViewById(R.id.iterations);

        return view;
    }
}
