package br.edu.fatecgru.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Serviço responsável por gerenciar a conexão Bluetooth SPP com o módulo HC-05.
 */
public class BluetoothService {

    private static final String TAG = "BluetoothService";

    // UUID padrão para comunicação Serial Port Profile (SPP / RFCOMM)
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter bluetoothAdapter;
    private final Handler mainHandler;
    private BluetoothConnectionListener listener;

    private ConnectThread connectThread;
    private ConnectedThread connectedThread;
    private boolean isConnecting = false;

    public BluetoothService(BluetoothConnectionListener listener) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.listener = listener;
    }

    public void setListener(BluetoothConnectionListener listener) {
        this.listener = listener;
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean isConnected() {
        return connectedThread != null && connectedThread.isRunning();
    }

    public boolean isConnecting() {
        return isConnecting;
    }

    /**
     * Inicia a tentativa de conexão com o dispositivo HC-05.
     */
    @SuppressLint("MissingPermission")
    public synchronized void connect(BluetoothDevice device) {
        if (device == null) {
            notifyError("Dispositivo Bluetooth inválido.");
            return;
        }

        disconnect();

        isConnecting = true;
        notifyConnecting(device.getName() != null ? device.getName() : device.getAddress());

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    /**
     * Encerra a conexão e libera recursos.
     */
    public synchronized void disconnect() {
        isConnecting = false;

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    private void notifyConnecting(String name) {
        mainHandler.post(() -> {
            if (listener != null) listener.onConnecting(name);
        });
    }

    private void notifyConnected(String name) {
        mainHandler.post(() -> {
            if (listener != null) listener.onConnected(name);
        });
    }

    private void notifyData(float temp, float hum) {
        mainHandler.post(() -> {
            if (listener != null) listener.onDataReceived(temp, hum);
        });
    }

    private void notifyError(String error) {
        mainHandler.post(() -> {
            if (listener != null) listener.onError(error);
        });
    }

    private void notifyDisconnected() {
        mainHandler.post(() -> {
            if (listener != null) listener.onDisconnected();
        });
    }

    /**
     * Thread para abrir o socket RFCOMM com o dispositivo remoto.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Falha ao criar socket RFCOMM", e);
            }
            this.socket = tmp;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            if (socket == null) {
                isConnecting = false;
                notifyError("Falha ao inicializar socket Bluetooth.");
                return;
            }

            // Cancela a descoberta para não degradar a velocidade da conexão
            try {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (Exception ignored) {
            }

            try {
                socket.connect();
            } catch (IOException connectException) {
                Log.e(TAG, "Não foi possível conectar ao socket", connectException);
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                isConnecting = false;
                notifyError("Não foi possível conectar ao dispositivo HC-05. Verifique se ele está ligado.");
                return;
            }

            synchronized (BluetoothService.this) {
                isConnecting = false;
                connectThread = null;
                connectedThread = new ConnectedThread(socket, device.getName() != null ? device.getName() : device.getAddress());
                connectedThread.start();
            }
        }

        public void cancel() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Thread para leitura contínua de dados do socket Bluetooth.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final String deviceName;
        private volatile boolean running = true;

        public ConnectedThread(BluetoothSocket socket, String deviceName) {
            this.socket = socket;
            this.deviceName = deviceName;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Erro ao obter streams", e);
            }
            this.inputStream = tmpIn;
            this.outputStream = tmpOut;
        }

        public boolean isRunning() {
            return running && socket != null && socket.isConnected();
        }

        @Override
        public void run() {
            notifyConnected(deviceName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (running) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    parseAndProcessLine(line.trim());
                } catch (IOException e) {
                    if (running) {
                        Log.e(TAG, "Conexão perdida", e);
                        notifyError("Conexão com o Arduino perdida.");
                    }
                    break;
                }
            }

            cancel();
            notifyDisconnected();
        }

        private void parseAndProcessLine(String line) {
            if (line.isEmpty()) return;

            try {
                // Formato 1: JSON {"temp": 29.4, "hum": 60.2}
                if (line.startsWith("{") && line.endsWith("}")) {
                    JSONObject json = new JSONObject(line);
                    float temp = (float) json.getDouble("temp");
                    float hum = (float) json.optDouble("hum", 0.0);
                    notifyData(temp, hum);
                    return;
                }

                // Formato 2: Protocolo textual TEMP:29.4;HUM:60.2 ou Umidade: 60 % Temperatura: 25.3 *C
                if (line.contains("TEMP:") || line.contains("temp:")) {
                    String clean = line.toUpperCase();
                    float temp = 0;
                    float hum = 0;
                    String[] parts = clean.split(";");
                    for (String part : parts) {
                        String[] kv = part.split(":");
                        if (kv.length == 2) {
                            String key = kv[0].trim();
                            float val = Float.parseFloat(kv[1].trim());
                            if (key.contains("TEMP")) temp = val;
                            if (key.contains("HUM")) hum = val;
                        }
                    }
                    notifyData(temp, hum);
                    return;
                }

                // Formato 2B: Saída do Monitor Serial "Umidade: 60 %\tTemperatura: 25.3 *C"
                if (line.contains("Umidade:") && line.contains("Temperatura:")) {
                    try {
                        int humIdx = line.indexOf("Umidade:") + 8;
                        int pctIdx = line.indexOf("%", humIdx);
                        int tempIdx = line.indexOf("Temperatura:") + 12;
                        int cIdx = line.indexOf("*C", tempIdx);
                        if (cIdx == -1) cIdx = line.indexOf("°C", tempIdx);
                        if (cIdx == -1) cIdx = line.indexOf("C", tempIdx);

                        float parsedHum = Float.parseFloat(line.substring(humIdx, pctIdx).trim());
                        float parsedTemp = Float.parseFloat(line.substring(tempIdx, cIdx).trim());
                        notifyData(parsedTemp, parsedHum);
                        return;
                    } catch (Exception ignored) {
                    }
                }

                // Formato 3: CSV simples (ex: 29.4,60.2)
                if (line.contains(",")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        float temp = Float.parseFloat(parts[0].trim());
                        float hum = Float.parseFloat(parts[1].trim());
                        notifyData(temp, hum);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Linha recebida com formato não reconhecido: " + line);
            }
        }

        public void cancel() {
            running = false;
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException ignored) {
            }
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException ignored) {
            }
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
