package br.edu.fatecgru.notification;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Locale;

import br.edu.fatecgru.MainActivity;
import br.edu.fatecgru.R;

/**
 * Utilitário para emissão de alertas de ondas de calor e temperatura crítica.
 */
public class HeatAlertNotificationHelper {

    private static final String CHANNEL_ID = "heat_alert_channel";
    private static final String CHANNEL_NAME = "Alertas de Onda de Calor";
    private static final String CHANNEL_DESC = "Notificações quando a temperatura atinge níveis críticos";
    private static final int NOTIFICATION_ID = 1001;

    // Cooldown para evitar disparar notificações em loop (mínimo de 3 minutos entre alertas)
    private static long lastNotificationTime = 0;
    private static final long COOLDOWN_MILLIS = 3 * 60 * 1000;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Envia notificação de alerta se a temperatura ultrapassar o limite crítico e o cooldown tiver passado.
     */
    @SuppressLint("MissingPermission")
    public static void checkAndTriggerAlert(Context context, String location, float temperature, float threshold) {
        if (temperature < threshold) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastNotificationTime < COOLDOWN_MILLIS) {
            return;
        }
        lastNotificationTime = now;

        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "⚠️ ALERTA DE ONDA DE CALOR EXTREMA!";
        String message = String.format(
                Locale.getDefault(),
                "Ponto '%s' registrou %.1f°C! (Limite crítico configurado: %.1f°C). Risco térmico elevado.",
                location, temperature, threshold
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }
}
