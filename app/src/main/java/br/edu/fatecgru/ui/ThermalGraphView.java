package br.edu.fatecgru.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.edu.fatecgru.data.model.TemperatureReading;

/**
 * Custom View para renderização do gráfico de evolução térmica (microclimas),
 * com destaque automático de Mínima, Máxima e linha de Média.
 */
public class ThermalGraphView extends View {

    private final List<TemperatureReading> readings = new ArrayList<>();

    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint avgLinePaint;
    private Paint maxPointPaint;
    private Paint minPointPaint;

    private Path linePath;
    private Path fillPath;

    private float minTemp = Float.MAX_VALUE;
    private float maxTemp = Float.MIN_VALUE;
    private float avgTemp = 0f;
    private int minIndex = -1;
    private int maxIndex = -1;

    public ThermalGraphView(Context context) {
        super(context);
        init();
    }

    public ThermalGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThermalGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#FF5722")); // Laranja quente
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#616161"));
        textPaint.setTextSize(spToPx(11));

        avgLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avgLinePaint.setColor(Color.parseColor("#4CAF50")); // Verde indicador de média
        avgLinePaint.setStrokeWidth(3f);
        avgLinePaint.setStyle(Paint.Style.STROKE);
        avgLinePaint.setPathEffect(new DashPathEffect(new float[]{12f, 8f}, 0));

        maxPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maxPointPaint.setColor(Color.parseColor("#D32F2F")); // Vermelho pico de calor
        maxPointPaint.setStyle(Paint.Style.FILL);

        minPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minPointPaint.setColor(Color.parseColor("#1976D2")); // Azul mínima
        minPointPaint.setStyle(Paint.Style.FILL);

        linePath = new Path();
        fillPath = new Path();
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    /**
     * Define a lista de medições para plotagem no gráfico.
     */
    public void setData(List<TemperatureReading> newReadings) {
        readings.clear();
        if (newReadings != null) {
            readings.addAll(newReadings);
        }
        recalculateStats();
        invalidate();
    }

    private void recalculateStats() {
        minTemp = Float.MAX_VALUE;
        maxTemp = Float.MIN_VALUE;
        minIndex = -1;
        maxIndex = -1;

        if (readings.isEmpty()) {
            avgTemp = 0f;
            return;
        }

        float sum = 0f;
        for (int i = 0; i < readings.size(); i++) {
            float t = readings.get(i).getTemperature();
            sum += t;
            if (t < minTemp) {
                minTemp = t;
                minIndex = i;
            }
            if (t > maxTemp) {
                maxTemp = t;
                maxIndex = i;
            }
        }
        avgTemp = sum / readings.size();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) return;

        if (readings.size() < 2) {
            String emptyMsg = readings.isEmpty()
                    ? "Nenhuma leitura registrada para este microclima."
                    : "Aguardando mais leituras para traçar a evolução térmica...";
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(emptyMsg, width / 2f, height / 2f, textPaint);
            return;
        }

        float paddingLeft = dpToPx(40);
        float paddingRight = dpToPx(30);
        float paddingTop = dpToPx(30);
        float paddingBottom = dpToPx(30);

        float chartWidth = width - paddingLeft - paddingRight;
        float chartHeight = height - paddingTop - paddingBottom;

        // Escala dinâmica com margem de folga de 2 graus
        float scaleMin = (float) Math.floor(minTemp - 2f);
        float scaleMax = (float) Math.ceil(maxTemp + 2f);
        if (scaleMax - scaleMin < 4f) {
            scaleMax = scaleMin + 4f;
        }
        float tempRange = scaleMax - scaleMin;

        // Desenhar linhas de grade e textos do eixo Y
        int gridSteps = 4;
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= gridSteps; i++) {
            float y = paddingTop + (chartHeight * i / gridSteps);
            float tempValue = scaleMax - (tempRange * i / gridSteps);

            canvas.drawLine(paddingLeft, y, width - paddingRight, y, gridPaint);
            canvas.drawText(String.format(Locale.getDefault(), "%.0f°C", tempValue), paddingLeft - dpToPx(6), y + dpToPx(4), textPaint);
        }

        // Construir caminhos da linha e do preenchimento
        linePath.reset();
        fillPath.reset();

        float stepX = chartWidth / (readings.size() - 1);
        float[] pointsX = new float[readings.size()];
        float[] pointsY = new float[readings.size()];

        for (int i = 0; i < readings.size(); i++) {
            float x = paddingLeft + (i * stepX);
            float temp = readings.get(i).getTemperature();
            float y = paddingTop + chartHeight - ((temp - scaleMin) / tempRange * chartHeight);

            pointsX[i] = x;
            pointsY[i] = y;

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.moveTo(x, paddingTop + chartHeight);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        fillPath.lineTo(pointsX[readings.size() - 1], paddingTop + chartHeight);
        fillPath.close();

        // Shader gradiente no preenchimento
        fillPaint.setShader(new LinearGradient(
                0, paddingTop, 0, paddingTop + chartHeight,
                Color.parseColor("#44FF5722"), Color.parseColor("#05FF5722"),
                Shader.TileMode.CLAMP
        ));
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // Desenhar linha de Média
        if (avgTemp >= scaleMin && avgTemp <= scaleMax) {
            float yAvg = paddingTop + chartHeight - ((avgTemp - scaleMin) / tempRange * chartHeight);
            canvas.drawLine(paddingLeft, yAvg, width - paddingRight, yAvg, avgLinePaint);

            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setColor(Color.parseColor("#2E7D32"));
            canvas.drawText(String.format(Locale.getDefault(), "--- Média: %.1f°C", avgTemp), paddingLeft + dpToPx(10), yAvg - dpToPx(4), textPaint);
            textPaint.setColor(Color.parseColor("#616161"));
        }

        // Desenhar destaque da Mínima
        if (minIndex >= 0) {
            float xMin = pointsX[minIndex];
            float yMin = pointsY[minIndex];
            canvas.drawCircle(xMin, yMin, dpToPx(6), minPointPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.parseColor("#1565C0"));
            canvas.drawText(String.format(Locale.getDefault(), "Mín: %.1f°C", minTemp), xMin, yMin + dpToPx(16), textPaint);
        }

        // Desenhar destaque da Máxima (Pico de Calor)
        if (maxIndex >= 0) {
            float xMax = pointsX[maxIndex];
            float yMax = pointsY[maxIndex];
            canvas.drawCircle(xMax, yMax, dpToPx(6), maxPointPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setColor(Color.parseColor("#C62828"));
            canvas.drawText(String.format(Locale.getDefault(), "Máx: %.1f°C", maxTemp), xMax, yMax - dpToPx(10), textPaint);
        }
    }
}
