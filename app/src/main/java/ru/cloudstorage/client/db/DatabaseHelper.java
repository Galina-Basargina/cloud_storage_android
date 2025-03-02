package ru.cloudstorage.client.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SettingsDB";
    private static final int DATABASE_VERSION = 1;

    // Таблица настроек
    public static final String TABLE_SETTINGS = "settings";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE = "value";

    // Создание таблицы
    private static final String CREATE_TABLE_SETTINGS =
        "CREATE TABLE " + TABLE_SETTINGS + " (" +
        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_KEY + " TEXT UNIQUE, " +
        COLUMN_VALUE + " TEXT);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SETTINGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: Это плохой метод обновления базы данных, так как данные будут удалены
        // Правильнее пользоваться параметрами oldVersion и newVersion, и командой ALTER TABLE
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    // Метод для чтения значения по ключу
    public String getSettings(String key, String defaultValue) {
        SQLiteDatabase db = this.getReadableDatabase();

        // SELECT value FROM settings WHERE key = ?;  -- работает быстрее, т.к. есть связываемая переменная
        // SELECT value FROM settings WHERE key = 'token'; -- литерал 'token' в запросе препятствует оптимизации
        Cursor cursor = db.query(
            TABLE_SETTINGS,
            new String[] {COLUMN_VALUE},
            COLUMN_KEY + " = ?",
            new String[] {key},
            null, null, null
        );

        final String value = cursor.moveToFirst() ? cursor.getString(0) : defaultValue;
        cursor.close();
        return value;
    }

    public void setSettings(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues params = new ContentValues();
        params.put(COLUMN_KEY, key);
        params.put(COLUMN_VALUE, value);

        // INSERT INTO settings(key, value) VALUES (:1, :2) ON CONFLICT ... REPLACE;
        db.insertWithOnConflict(
            TABLE_SETTINGS,
            null,
            params,
            SQLiteDatabase.CONFLICT_REPLACE
        );
        db.close();
    }

    public void resetSettings(String key) {
        SQLiteDatabase db = this.getWritableDatabase();

        // DELETE FROM settings WHERE key = ?;
        db.delete(
            TABLE_SETTINGS,
            COLUMN_KEY + " = ?",
            new String[] {key}
        );
    }
}
