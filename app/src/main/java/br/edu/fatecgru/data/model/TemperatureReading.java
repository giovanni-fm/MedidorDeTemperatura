package br.edu.fatecgru.data.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Modelo de dados que representa uma medição climática coletada pelo Arduino.
 */
public class TemperatureReading {
    private long id;
    private String location; // Ex: "Área Arborizada", "Área Asfaltada"
    private float temperature; // Em graus Celsius
    private float humidity;    // Em porcentagem (%)
    private long timestamp;    // Epoch timestamp em milissegundos

    public TemperatureReading() {
    }

    public TemperatureReading(String location, float temperature, float humidity, long timestamp) {
        this.location = location;
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }

    public TemperatureReading(long id, String location, float temperature, float humidity, long timestamp) {
        this.id = id;
        this.location = location;
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
