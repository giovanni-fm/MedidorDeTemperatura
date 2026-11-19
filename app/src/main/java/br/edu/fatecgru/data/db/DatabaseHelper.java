package br.edu.fatecgru.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.data.model.TemperatureReading;
import br.edu.fatecgru.data.model.ThermalStats;

/**
 * Gerenciador do banco de dados local SQLite para armazenamento do histórico térmico.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "microclima_monitor.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_READINGS = "thermal_readings";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_HUMIDITY = "humidity";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_READINGS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_LOCATION + " TEXT NOT NULL, " +
                    COLUMN_TEMPERATURE + " REAL NOT NULL, " +
                    COLUMN_HUMIDITY + " REAL NOT NULL, " +
                    COLUMN_TIMESTAMP + " INTEGER NOT NULL" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_READINGS);
        onCreate(db);
    }

    /**
     * Insere uma nova leitura térmica no banco de dados.
     */
    public long insertReading(TemperatureReading reading) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LOCATION, reading.getLocation());
        values.put(COLUMN_TEMPERATURE, reading.getTemperature());
        values.put(COLUMN_HUMIDITY, reading.getHumidity());
        values.put(COLUMN_TIMESTAMP, reading.getTimestamp());

        return db.insert(TABLE_READINGS, null, values);
    }

    /**
     * Retorna a lista de leituras ordenadas cronologicamente para um determinado local.
     * Se location for nulo ou "Todos", retorna de todos os locais.
     */
    public List<TemperatureReading> getReadings(String location) {
        List<TemperatureReading> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = null;
        String[] selectionArgs = null;

        if (location != null && !location.trim().isEmpty() && !location.equalsIgnoreCase("Todos")) {
            selection = COLUMN_LOCATION + " = ?";
            selectionArgs = new String[]{location};
        }

        Cursor cursor = db.query(
                TABLE_READINGS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                COLUMN_TIMESTAMP + " ASC"
        );

        if (cursor != null) {
            try {
                int idIndex = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int locIndex = cursor.getColumnIndexOrThrow(COLUMN_LOCATION);
                int tempIndex = cursor.getColumnIndexOrThrow(COLUMN_TEMPERATURE);
                int humIndex = cursor.getColumnIndexOrThrow(COLUMN_HUMIDITY);
                int timeIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP);

                while (cursor.moveToNext()) {
                    TemperatureReading r = new TemperatureReading(
                            cursor.getLong(idIndex),
                            cursor.getString(locIndex),
                            cursor.getFloat(tempIndex),
                            cursor.getFloat(humIndex),
                            cursor.getLong(timeIndex)
                    );
                    list.add(r);
                }
            } finally {
                cursor.close();
            }
        }

        return list;
    }

    /**
     * Calcula estatísticas (Mínima, Máxima e Média) diretamente via SQL.
     */
    public ThermalStats getStats(String location) {
        SQLiteDatabase db = this.getReadableDatabase();

        String query;
        String[] args;

        if (location != null && !location.trim().isEmpty() && !location.equalsIgnoreCase("Todos")) {
            query = "SELECT MIN(" + COLUMN_TEMPERATURE + ") as min_t, " +
                    "MAX(" + COLUMN_TEMPERATURE + ") as max_t, " +
                    "AVG(" + COLUMN_TEMPERATURE + ") as avg_t, " +
                    "COUNT(*) as total, " +
                    "(SELECT " + COLUMN_HUMIDITY + " FROM " + TABLE_READINGS + " WHERE " + COLUMN_LOCATION + " = ? ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 1) as last_h " +
                    "FROM " + TABLE_READINGS + " WHERE " + COLUMN_LOCATION + " = ?";
            args = new String[]{location, location};
        } else {
            query = "SELECT MIN(" + COLUMN_TEMPERATURE + ") as min_t, " +
                    "MAX(" + COLUMN_TEMPERATURE + ") as max_t, " +
                    "AVG(" + COLUMN_TEMPERATURE + ") as avg_t, " +
                    "COUNT(*) as total, " +
                    "(SELECT " + COLUMN_HUMIDITY + " FROM " + TABLE_READINGS + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 1) as last_h " +
                    "FROM " + TABLE_READINGS;
            args = null;
        }

        Cursor cursor = db.rawQuery(query, args);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int total = cursor.getInt(cursor.getColumnIndexOrThrow("total"));
                    if (total > 0) {
                        float min = cursor.getFloat(cursor.getColumnIndexOrThrow("min_t"));
                        float max = cursor.getFloat(cursor.getColumnIndexOrThrow("max_t"));
                        float avg = cursor.getFloat(cursor.getColumnIndexOrThrow("avg_t"));
                        float lastH = cursor.getFloat(cursor.getColumnIndexOrThrow("last_h"));
                        return new ThermalStats(min, max, avg, lastH, total);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return ThermalStats.empty();
    }

    /**
     * Obtém os nomes distintos de locais cadastrados.
     */
    public List<String> getLocations() {
        List<String> locations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                true,
                TABLE_READINGS,
                new String[]{COLUMN_LOCATION},
                null,
                null,
                null,
                null,
                COLUMN_LOCATION + " ASC",
                null
        );

        if (cursor != null) {
            try {
                int locIndex = cursor.getColumnIndexOrThrow(COLUMN_LOCATION);
                while (cursor.moveToNext()) {
                    locations.add(cursor.getString(locIndex));
                }
            } finally {
                cursor.close();
            }
        }
        return locations;
    }

    /**
     * Limpa o histórico de medições para um local (ou todos se null/Todos).
     */
    public int clearHistory(String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (location != null && !location.trim().isEmpty() && !location.equalsIgnoreCase("Todos")) {
            return db.delete(TABLE_READINGS, COLUMN_LOCATION + " = ?", new String[]{location});
        } else {
            return db.delete(TABLE_READINGS, null, null);
        }
    }

    /**
     * Limpa o histórico de medições para múltiplos locais selecionados.
     */
    public int clearHistory(List<String> locations) {
        if (locations == null || locations.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        StringBuilder where = new StringBuilder(COLUMN_LOCATION + " IN (");
        String[] args = new String[locations.size()];
        for (int i = 0; i < locations.size(); i++) {
            where.append("?");
            if (i < locations.size() - 1) {
                where.append(",");
            }
            args[i] = locations.get(i);
        }
        where.append(")");
        return db.delete(TABLE_READINGS, where.toString(), args);
    }
}
