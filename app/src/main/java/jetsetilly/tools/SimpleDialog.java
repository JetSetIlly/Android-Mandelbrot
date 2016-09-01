package jetsetilly.tools;

import android.app.DialogFragment;

/*  Overview of usage BroadcastReceiver implementation. (pseudo-code)

    if intent.getAction() == RESULT_ID {
        switch (intent.RESULT_ACTION) {
            case ACTION_SET:
                get intent.RESULT_PAYLOAD

            case ACTION_MORE:
                get intent.RESULT_PAYLOAD
    }
*/

public class SimpleDialog extends DialogFragment {
    public static final String RESULT_ID = "ITERATIONS_DIALOG";
    public static final String RESULT_ACTION = "ACTION";
    public static final String ACTION_SET = "SET";
    public static final String ACTION_MORE = "MORE";

    // payload type is context sensitive depending on RESULT_ACTION
    // eg. if payload is an integer then pack payload with:
    //
    //      intent.putExtra(RESULT_PAYLOAD, (integer) payload)
    //
    // and retrieve with:
    //
    //      i = intent.getIntExtra(RESULT_PAYLOAD, (integer) default)
    public static final String RESULT_PAYLOAD = "PAYLOAD";
}
