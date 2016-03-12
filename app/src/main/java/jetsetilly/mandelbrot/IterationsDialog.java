package jetsetilly.mandelbrot;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import jetsetilly.mandelbrot.Gadgets.IterationsSeekBar;

public class IterationsDialog extends DialogFragment {
    private IterationsSeekBar iterations;

    public static final String ITERATIONS_DIALOG_INTENT = "ITERATIONS_DIALOG";
    public static final String INTENT_ACTION = "ITERATIONS_DIALOG_SET";
    public static final String ACTION_SET = "ITERATIONS_DIALOG_SET";
    public static final String ACTION_MORE = "ITERATIONS_DIALOG_MORE";
    public static final String SET_VALUE = "SET_ITERATIONS_VALUE";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Dialog_Theme);
        builder.setTitle(R.string.settings_max_iterations_label);
        builder.setView(createView());

        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (iterations.hasChanged()) {
                    Intent intent = new Intent(ITERATIONS_DIALOG_INTENT);
                    intent.putExtra(INTENT_ACTION, ACTION_SET);
                    intent.putExtra(SET_VALUE, iterations.getInteger());
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                }
            }
        }).setNeutralButton(R.string.dialog_more, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(ITERATIONS_DIALOG_INTENT);
                intent.putExtra(INTENT_ACTION, ACTION_MORE);
                intent.putExtra(SET_VALUE, iterations.getInteger());
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        }).setNegativeButton(R.string.dialog_cancel, null);

        // Create the AlertDialog object and return it
        Dialog dialog = builder.create();

        // set width to match parent (setting this in the layout file does not work as expected)
        WindowManager.LayoutParams layout = new WindowManager.LayoutParams();
        layout.copyFrom(dialog.getWindow().getAttributes());
        layout.width = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.show();
        dialog.getWindow().setAttributes(layout);

        return dialog;
    }

    private View createView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_iterations, null);

        iterations = (IterationsSeekBar) view.findViewById(R.id.iterations);

        return view;
    }
}
