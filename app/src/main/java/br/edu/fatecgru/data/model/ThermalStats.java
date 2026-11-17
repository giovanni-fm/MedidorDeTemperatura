package br.edu.fatecgru.data.model;

/**
 * Estatísticas calculadas para um conjunto de leituras térmicas.
 */
public class ThermalStats {
    private final float minTemperature;
    private final float maxTemperature;
    private final float avgTemperature;
    private final float currentHumidity;
    private final int totalReadings;

    public ThermalStats(float minTemperature, float maxTemperature, float avgTemperature, float currentHumidity, int totalReadings) {
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.avgTemperature = avgTemperature;
        this.currentHumidity = currentHumidity;
        this.totalReadings = totalReadings;
    }

    public static ThermalStats empty() {
        return new ThermalStats(0f, 0f, 0f, 0f, 0);
    }

    public float getMinTemperature() {
        return minTemperature;
    }

    public float getMaxTemperature() {
        return maxTemperature;
    }

    public float getAvgTemperature() {
        return avgTemperature;
    }

    public float getCurrentHumidity() {
        return currentHumidity;
    }

    public int getTotalReadings() {
        return totalReadings;
    }

    public boolean hasData() {
        return totalReadings > 0;
    }
}
