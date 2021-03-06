package jetsetilly.mandelbrot;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import jetsetilly.mandelbrot.View.IterationsSeekBar;
import jetsetilly.tools.SimpleDialog;

public class IterationsDialog extends SimpleDialog {
    private IterationsSeekBar iterations;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Dialog_Theme);
        builder.setTitle(R.string.settings_max_iterations_label);
        builder.setView(createView());

        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (iterations.hasChanged()) {
                    Intent intent = new Intent(RESULT_ID);
                    intent.putExtra(RESULT_ACTION, ACTION_POSITIVE);
                    intent.putExtra(RESULT_PAYLOAD, iterations.getInteger());
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                }
            }
        }).setNeutralButton(R.string.dialog_more, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(RESULT_ID);
                intent.putExtra(RESULT_ACTION, ACTION_NEUTRAL);
                intent.putExtra(RESULT_PAYLOAD, iterations.getInteger());
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            }
        }).setNegativeButton(R.string.dialog_cancel, null);

        // create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();

        // set width to match parent (setting this in the layout file does not work as expected)
        WindowManager.LayoutParams layout = new WindowManager.LayoutParams();
        layout.copyFrom(dialog.getWindow().getAttributes());
        layout.width = WindowManager.LayoutParams.MATCH_PARENT;
        layout.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(layout);
    }

    private View createView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_iterations, null);
        iterations = (IterationsSeekBar) view.findViewById(R.id.iterations);
        return view;
    }
}
