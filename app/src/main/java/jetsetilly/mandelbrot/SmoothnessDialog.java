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

import jetsetilly.mandelbrot.Settings.Settings;
import jetsetilly.mandelbrot.View.ReportingSeekBar;
import jetsetilly.tools.SimpleDialog;

public class SmoothnessDialog extends SimpleDialog {
    private ReportingSeekBar smoothness;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Dialog_Theme);
        builder.setTitle(R.string.palette_smoothness_label);
        builder.setView(createView());

        final Bundle args = getArguments();
        smoothness.set(args.getInt(INIT_PARAMS));

        builder.setPositiveButton(R.string.dialog_ok, null);
        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                smoothness.set(args.getInt(INIT_PARAMS));
            }
        });
        builder.setNeutralButton(R.string.dialog_default, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                smoothness.set(Settings.getInstance().DEF_PALETTE_SMOOTHNESS);
            }
        });

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

    @Override
    public void onDismiss(DialogInterface dialog) {
        Intent intent = new Intent(RESULT_ID);
        intent.putExtra(RESULT_ACTION, ACTION_POSITIVE);
        intent.putExtra(RESULT_PAYLOAD, smoothness.getInteger());
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    private View createView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_smoothness, null);
        smoothness = (ReportingSeekBar) view.findViewById(R.id.smoothness);
        return view;
    }
}
