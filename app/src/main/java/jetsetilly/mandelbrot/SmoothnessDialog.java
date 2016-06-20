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

import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.View.ReportingSeekBar;

public class SmoothnessDialog extends DialogFragment {
    private ReportingSeekBar smoothness;

    public static final String SMOOTHNESS_DIALOG_INTENT = "SMOOTHNESS_DIALOG";
    public static final String INTENT_ACTION = "SMOOTHNESS_DIALOG_SET";
    public static final String ACTION_SET = "SMOOTHNESS_DIALOG_SET";
    public static final String SET_VALUE = "SET_SMOOTHNESS_VALUE";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Dialog_Theme);
        builder.setTitle(R.string.palette_smoothness_label);
        builder.setView(createView());

        final Bundle args = getArguments();
        smoothness.set(args.getInt(SET_VALUE));

        builder.setPositiveButton(R.string.dialog_ok, null);
        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                smoothness.set(args.getInt(SET_VALUE));
            }
        });
        builder.setNeutralButton(R.string.dialog_default, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                smoothness.set(PaletteSettings.getInstance().DEF_SMOOTHNESS);
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
        Intent intent = new Intent(SMOOTHNESS_DIALOG_INTENT);
        intent.putExtra(INTENT_ACTION, ACTION_SET);
        intent.putExtra(SET_VALUE, smoothness.getInteger());
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    private View createView() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_smoothness, null);

        smoothness = (ReportingSeekBar) view.findViewById(R.id.smoothness);

        return view;
    }
}
