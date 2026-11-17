package br.edu.fatecgru.bluetooth;

/**
 * Interface de callback para eventos de ciclo de vida e recebimento de dados via Bluetooth.
 */
public interface BluetoothConnectionListener {
    void onConnecting(String deviceName);
    void onConnected(String deviceName);
    void onDataReceived(float temperature, float humidity);
    void onError(String errorMessage);
    void onDisconnected();
}
