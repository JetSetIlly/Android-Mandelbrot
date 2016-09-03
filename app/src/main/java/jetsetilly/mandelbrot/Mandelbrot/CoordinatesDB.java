package jetsetilly.mandelbrot.Mandelbrot;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.MandelbrotCoordinates;
import jetsetilly.tools.LogTools;

public class CoordinatesDB extends SQLiteOpenHelper {
    private final String DBG_TAG = "CoordinatesDB";

    private Context context;

    private static final String DB_NAME = "coords_db";
    private static final int DB_VERSION = 2;

    private static final String CURRENT_BOOKMARK = "CURRENT";
    private static final String ROOT_BOOKMARK = "ROOT";

    public static class Bookmarks implements BaseColumns {
        public static final String TABLE_NAME = "bookmarks";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_REAL_LEFT = "real_left";
        public static final String COLUMN_REAL_RIGHT = "real_right";
        public static final String COLUMN_IMAGINARY_UPPER = "imaginary_upper";
        public static final String COLUMN_IMAGINARY_LOWER = "imaginary_lower";
        public static final String COLUMN_MAX_ITERATIONS = "max_iterations";
        public static final String COLUMN_BAILOUT_VALUE = "bailout_value";
        public static final String COLUMN_DATE_ADDED = "date_added";
    }

    private static final String[] COMPLETE_PROJECTION = {
            Bookmarks.COLUMN_NAME,
            Bookmarks.COLUMN_REAL_LEFT,
            Bookmarks.COLUMN_REAL_RIGHT,
            Bookmarks.COLUMN_IMAGINARY_UPPER,
            Bookmarks.COLUMN_IMAGINARY_LOWER,
            Bookmarks.COLUMN_MAX_ITERATIONS,
            Bookmarks.COLUMN_BAILOUT_VALUE,
            Bookmarks.COLUMN_DATE_ADDED
    };

    public CoordinatesDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        print();
    }

    public void print() {
        try (SQLiteDatabase db = getReadableDatabase()) {
            Cursor c = db.rawQuery("SELECT * FROM " + Bookmarks.TABLE_NAME, null);
            db.query(Bookmarks.TABLE_NAME, COMPLETE_PROJECTION, null, null, null, null, null);
            LogTools.printDebug(DBG_TAG, "num of rows " + c.getCount());

            {
                String output = "";
                for (int j = 0; j < c.getColumnCount(); ++j) {
                    output += c.getColumnName(j) + ", ";
                }
                LogTools.printDebug(DBG_TAG, output);
            }

            c.moveToFirst();
            for (int i = 0; i < c.getCount(); ++ i) {
                String output = "";
                for (int j = 0; j < c.getColumnCount(); ++ j) {
                    output += c.getString(j) + ", ";
                }
                LogTools.printDebug(DBG_TAG, output);
                c.moveToNext();
            }
        }
    }

    public void restoreCurrent(MandelbrotCoordinates m) {
        restore(CURRENT_BOOKMARK, m);
    }

    public void restore(String bookmark, MandelbrotCoordinates m) {
        try (SQLiteDatabase db = getReadableDatabase()) {
            Cursor c = db.query(Bookmarks.TABLE_NAME, COMPLETE_PROJECTION,
                    Bookmarks.COLUMN_NAME + " = ", new String[] {bookmark},
                    null, null, null);
            if (c.getCount() > 1) throw new SQLiteConstraintException(context.getResources().getString(R.string.db_too_many_rows));
            m.real_left = Double.parseDouble(c.getString(c.getColumnIndex(Bookmarks.COLUMN_REAL_LEFT)));
            m.real_right = Double.parseDouble(c.getString(c.getColumnIndex(Bookmarks.COLUMN_REAL_RIGHT)));
            m.imaginary_upper = Double.parseDouble(c.getString(c.getColumnIndex(Bookmarks.COLUMN_IMAGINARY_UPPER)));
            m.imaginary_lower = Double.parseDouble(c.getString(c.getColumnIndex(Bookmarks.COLUMN_IMAGINARY_LOWER)));
            m.max_iterations = Integer.parseInt(c.getString(c.getColumnIndex(Bookmarks.COLUMN_MAX_ITERATIONS)));
            m.bailout_value = Double.parseDouble(c.getString(c.getColumnIndex(Bookmarks.COLUMN_BAILOUT_VALUE)));
        }
    }

    public void storeCurrent(MandelbrotCoordinates m) {
        store(CURRENT_BOOKMARK, m);
    }

    public void store(String name, MandelbrotCoordinates m) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            store(db, name, m);
        }
    }

    private void store(SQLiteDatabase db, String name, MandelbrotCoordinates m) {
        ContentValues values = new ContentValues();
        values.put(Bookmarks.COLUMN_NAME, name);
        values.put(Bookmarks.COLUMN_REAL_LEFT, m.real_left);
        values.put(Bookmarks.COLUMN_REAL_RIGHT, m.real_right);
        values.put(Bookmarks.COLUMN_IMAGINARY_UPPER, m.imaginary_upper);
        values.put(Bookmarks.COLUMN_IMAGINARY_LOWER, m.imaginary_lower);
        values.put(Bookmarks.COLUMN_MAX_ITERATIONS, m.max_iterations);
        values.put(Bookmarks.COLUMN_BAILOUT_VALUE, m.bailout_value);
        values.put(Bookmarks.COLUMN_DATE_ADDED, System.currentTimeMillis());
        db.insert(Bookmarks.TABLE_NAME, null, values);
    }

    public void delete(String name) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            String selection = Bookmarks.COLUMN_NAME + " LIKE ? AND != ? AND != ?";
            String[] selectionArgs = { name, ROOT_BOOKMARK, CURRENT_BOOKMARK };
            db.delete(Bookmarks.TABLE_NAME, selection, selectionArgs);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_ENTRIES =
                "CREATE TABLE " +
                        Bookmarks.TABLE_NAME + " ("
                        + Bookmarks.COLUMN_NAME + " TEXT PRIMARY KEY, "
                        + Bookmarks.COLUMN_REAL_LEFT + " TEXT, "
                        + Bookmarks.COLUMN_REAL_RIGHT + " TEXT, "
                        + Bookmarks.COLUMN_IMAGINARY_UPPER + " TEXT, "
                        + Bookmarks.COLUMN_IMAGINARY_LOWER + " TEXT, "
                        + Bookmarks.COLUMN_MAX_ITERATIONS + " TEXT, "
                        + Bookmarks.COLUMN_BAILOUT_VALUE + " TEXT,"
                        + Bookmarks.COLUMN_DATE_ADDED + " TEXT"
                        + " )";
        db.execSQL(SQL_CREATE_ENTRIES);
        store(db, ROOT_BOOKMARK, new MandelbrotCoordinates());
    }

    private void recreate(SQLiteDatabase db) {
        String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + Bookmarks.TABLE_NAME;
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // heavy-handed blanket approach to schema upgrades
        recreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // heavy-handed blanket approach to schema downgrades
        recreate(db);
    }
}
