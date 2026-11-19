package br.edu.fatecgru.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.edu.fatecgru.data.db.DatabaseHelper;
import br.edu.fatecgru.data.model.TemperatureReading;
import br.edu.fatecgru.data.model.ThermalStats;

/**
 * Repositório responsável por isolar a persistência de dados e executar operações em background.
 */
public class ReadingRepository {

    private final DatabaseHelper dbHelper;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onResult(T result);
    }

    public ReadingRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context.getApplicationContext());
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void insertReading(TemperatureReading reading, Callback<Long> callback) {
        executor.execute(() -> {
            long id = dbHelper.insertReading(reading);
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(id));
            }
        });
    }

    public void getReadings(String location, Callback<List<TemperatureReading>> callback) {
        executor.execute(() -> {
            List<TemperatureReading> list = dbHelper.getReadings(location);
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(list));
            }
        });
    }

    public void getStats(String location, Callback<ThermalStats> callback) {
        executor.execute(() -> {
            ThermalStats stats = dbHelper.getStats(location);
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(stats));
            }
        });
    }

    public void getLocations(Callback<List<String>> callback) {
        executor.execute(() -> {
            List<String> list = dbHelper.getLocations();
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(list));
            }
        });
    }

    public void clearHistory(String location, Callback<Integer> callback) {
        executor.execute(() -> {
            int deleted = dbHelper.clearHistory(location);
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(deleted));
            }
        });
    }

    public void clearHistory(List<String> locations, Callback<Integer> callback) {
        executor.execute(() -> {
            int deleted = dbHelper.clearHistory(locations);
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(deleted));
            }
        });
    }
}
