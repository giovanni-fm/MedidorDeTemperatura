package br.edu.fatecgru;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import br.edu.fatecgru.bluetooth.BluetoothConnectionListener;
import br.edu.fatecgru.bluetooth.BluetoothService;
import br.edu.fatecgru.data.model.TemperatureReading;
import br.edu.fatecgru.data.model.ThermalStats;
import br.edu.fatecgru.data.repository.ReadingRepository;
import br.edu.fatecgru.notification.HeatAlertNotificationHelper;
import br.edu.fatecgru.ui.ThermalGraphView;

public class MainActivity extends AppCompatActivity implements BluetoothConnectionListener {

    private View viewStatusDot;
    private TextView tvConnectionStatus;
    private MaterialButton btnConnectBt;
    private Spinner spLocation;
    private MaterialButton btnAddLocation;

    private TextView tvCurrentTemp;
    private TextView tvCurrentHum;
    private TextView tvLastUpdate;

    private TextView tvMinTemp;
    private TextView tvAvgTemp;
    private TextView tvMaxTemp;
    private TextView tvTotalReadings;

    private ThermalGraphView thermalGraphView;
    private TextView tvThresholdDisplay;
    private Slider sliderThreshold;
    private MaterialButton btnSimulateReading;
    private MaterialButton btnClearHistory;

    private ReadingRepository repository;
    private BluetoothService bluetoothService;

    private final List<String> locationList = new ArrayList<>();
    private ArrayAdapter<String> locationAdapter;
    private String selectedLocation = "Área Asfaltada / Estacionamento";

    private float criticalThreshold = 32.0f;
    private final Random random = new Random();

    // Launcher para solicitação de múltiplas permissões
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    showDeviceSelectionDialog();
                } else {
                    Toast.makeText(this, "Permissões necessárias para usar Bluetooth e Notificações.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.coordinatorLayout);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        initServices();
        setupLocationSpinner();
        setupListeners();
        loadHistoryAndStats();
    }

    private void initViews() {
        viewStatusDot = findViewById(R.id.viewStatusDot);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        btnConnectBt = findViewById(R.id.btnConnectBt);
        spLocation = findViewById(R.id.spLocation);
        btnAddLocation = findViewById(R.id.btnAddLocation);

        tvCurrentTemp = findViewById(R.id.tvCurrentTemp);
        tvCurrentHum = findViewById(R.id.tvCurrentHum);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);

        tvMinTemp = findViewById(R.id.tvMinTemp);
        tvAvgTemp = findViewById(R.id.tvAvgTemp);
        tvMaxTemp = findViewById(R.id.tvMaxTemp);
        tvTotalReadings = findViewById(R.id.tvTotalReadings);

        thermalGraphView = findViewById(R.id.thermalGraphView);
        tvThresholdDisplay = findViewById(R.id.tvThresholdDisplay);
        sliderThreshold = findViewById(R.id.sliderThreshold);
        btnSimulateReading = findViewById(R.id.btnSimulateReading);
        btnClearHistory = findViewById(R.id.btnClearHistory);

        // Trava inicial: botão de simulação desabilitado (cinza) até que haja conexão Bluetooth ativa
        updateSimulationButtonState(false);
    }

    private void updateSimulationButtonState(boolean isConnected) {
        if (btnSimulateReading != null) {
            btnSimulateReading.setEnabled(isConnected);
            if (isConnected) {
                btnSimulateReading.setText(R.string.btn_simulate);
                btnSimulateReading.setAlpha(1.0f);
            } else {
                btnSimulateReading.setText("Simular Leitura (Requer Conexão)");
                btnSimulateReading.setAlpha(0.5f);
            }
        }
    }

    private void initServices() {
        repository = new ReadingRepository(this);
        bluetoothService = new BluetoothService(this);
        HeatAlertNotificationHelper.createNotificationChannel(this);
    }

    private void setupLocationSpinner() {
        locationList.clear();
        locationList.add("Área Asfaltada / Estacionamento");
        locationList.add("Área Arborizada / Bosque");
        locationList.add("Pátio Escolar / Praça");
        locationList.add("Ambiente Interno / Sala");
        locationList.add("Ponto Genérico");
        locationList.add("Todos os Pontos");

        locationAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locationList);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLocation.setAdapter(locationAdapter);

        spLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLocation = locationList.get(position);
                loadHistoryAndStats();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupListeners() {
        btnConnectBt.setOnClickListener(v -> {
            if (bluetoothService.isConnected()) {
                bluetoothService.disconnect();
            } else {
                checkPermissionsAndConnect();
            }
        });

        btnAddLocation.setOnClickListener(v -> showAddLocationDialog());

        sliderThreshold.addOnChangeListener((slider, value, fromUser) -> {
            criticalThreshold = value;
            tvThresholdDisplay.setText(String.format(Locale.getDefault(), "%.1f °C", criticalThreshold));
        });

        btnSimulateReading.setOnClickListener(v -> simulateClimateReading());

        btnClearHistory.setOnClickListener(v -> showClearHistoryDialog());
    }

    private void checkPermissionsAndConnect() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissions.isEmpty()) {
            permissionLauncher.launch(permissions.toArray(new String[0]));
        } else {
            showDeviceSelectionDialog();
        }
    }

    @SuppressLint("MissingPermission")
    private void showDeviceSelectionDialog() {
        if (!bluetoothService.isBluetoothSupported()) {
            Toast.makeText(this, "Bluetooth não suportado neste aparelho.", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Toast.makeText(this, "Por favor, ative o Bluetooth nas configurações.", Toast.LENGTH_LONG).show();
            return;
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices == null || pairedDevices.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Nenhum HC-05 Pareado")
                    .setMessage("Para conectar ao Arduino, primeiro pareie o módulo HC-05 nas configurações de Bluetooth do Android (código PIN padrão: 1234 ou 0000).")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        List<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);
        String[] deviceNames = new String[deviceList.size()];
        int defaultHc05Index = -1;

        for (int i = 0; i < deviceList.size(); i++) {
            BluetoothDevice dev = deviceList.get(i);
            String name = dev.getName() != null ? dev.getName() : "Dispositivo desconhecido";
            deviceNames[i] = name + "\n" + dev.getAddress();
            if (name.toUpperCase().contains("HC-05") || name.toUpperCase().contains("HC05")) {
                defaultHc05Index = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione o Módulo Bluetooth (HC-05)")
                .setSingleChoiceItems(deviceNames, defaultHc05Index, (dialog, which) -> {
                    dialog.dismiss();
                    BluetoothDevice chosen = deviceList.get(which);
                    bluetoothService.connect(chosen);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAddLocationDialog() {
        EditText input = new EditText(this);
        input.setHint("Ex: Quadra Coberta, Telhado Verde");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Novo Ponto de Medição")
                .setMessage("Informe a identificação do novo microclima para coleta:")
                .setView(input)
                .setPositiveButton("Adicionar", (dialog, which) -> {
                    String newLoc = input.getText().toString().trim();
                    if (!newLoc.isEmpty()) {
                        if (!locationList.contains(newLoc)) {
                            // Insere antes de "Todos os Pontos"
                            int insertIdx = Math.max(0, locationList.size() - 1);
                            locationList.add(insertIdx, newLoc);
                            locationAdapter.notifyDataSetChanged();
                            spLocation.setSelection(insertIdx);
                        } else {
                            spLocation.setSelection(locationList.indexOf(newLoc));
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Limpar Histórico")
                .setMessage("Deseja apagar as leituras do ponto selecionado ('" + selectedLocation + "')?")
                .setPositiveButton("Sim, Limpar", (dialog, which) -> {
                    String target = selectedLocation.equalsIgnoreCase("Todos os Pontos") ? null : selectedLocation;
                    repository.clearHistory(target, deleted -> {
                        Toast.makeText(this, "Histórico limpo (" + deleted + " registros removidos).", Toast.LENGTH_SHORT).show();
                        loadHistoryAndStats();
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void simulateClimateReading() {
        // Gera valores coerentes com microclimas urbanos:
        // Asfalto tende a esquentar mais (30°C - 38°C), área arborizada mais amena (23°C - 28°C)
        float baseTemp;
        if (selectedLocation.contains("Asfaltada") || selectedLocation.contains("Estacionamento")) {
            baseTemp = 31.0f + (random.nextFloat() * 6.5f); // 31.0°C a 37.5°C (picos de ilha de calor!)
        } else if (selectedLocation.contains("Arborizada") || selectedLocation.contains("Bosque")) {
            baseTemp = 23.5f + (random.nextFloat() * 4.0f); // 23.5°C a 27.5°C (ilha de frescor)
        } else {
            baseTemp = 26.0f + (random.nextFloat() * 7.0f); // 26.0°C a 33.0°C
        }

        float hum = 45.0f + (random.nextFloat() * 35.0f); // 45% a 80%
        onDataReceived(Math.round(baseTemp * 10f) / 10f, Math.round(hum * 10f) / 10f);

        Toast.makeText(this, "Leitura simulada gerada!", Toast.LENGTH_SHORT).show();
    }

    private void loadHistoryAndStats() {
        String filter = selectedLocation.equalsIgnoreCase("Todos os Pontos") ? null : selectedLocation;

        repository.getStats(filter, stats -> {
            if (stats != null && stats.hasData()) {
                tvMinTemp.setText(String.format(Locale.getDefault(), "%.1f °C", stats.getMinTemperature()));
                tvAvgTemp.setText(String.format(Locale.getDefault(), "%.1f °C", stats.getAvgTemperature()));
                tvMaxTemp.setText(String.format(Locale.getDefault(), "%.1f °C", stats.getMaxTemperature()));
                tvTotalReadings.setText(String.format(Locale.getDefault(), "Amostras no banco: %d", stats.getTotalReadings()));
            } else {
                tvMinTemp.setText("--.- °C");
                tvAvgTemp.setText("--.- °C");
                tvMaxTemp.setText("--.- °C");
                tvTotalReadings.setText("Amostras no banco: 0");
            }
        });

        repository.getReadings(filter, readings -> {
            if (readings != null) {
                thermalGraphView.setData(readings);
                if (!readings.isEmpty()) {
                    TemperatureReading last = readings.get(readings.size() - 1);
                    updateCurrentReadingUi(last.getTemperature(), last.getHumidity(), last.getTimestamp());
                }
            }
        });
    }

    private void updateCurrentReadingUi(float temp, float hum, long timestamp) {
        tvCurrentTemp.setText(String.format(Locale.getDefault(), "%.1f °C", temp));
        tvCurrentHum.setText(String.format(Locale.getDefault(), "%.1f %%", hum));

        // Cor dinâmica para o valor atual da temperatura
        if (temp >= criticalThreshold) {
            tvCurrentTemp.setTextColor(ContextCompat.getColor(this, R.color.temp_hot));
        } else if (temp >= 26.0f) {
            tvCurrentTemp.setTextColor(ContextCompat.getColor(this, R.color.temp_medium));
        } else {
            tvCurrentTemp.setTextColor(ContextCompat.getColor(this, R.color.temp_cool));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss (dd/MM)", Locale.getDefault());
        tvLastUpdate.setText(String.format("Última atualização: %s", sdf.format(new Date(timestamp))));
    }

    // ==========================================================
    // Callbacks do BluetoothConnectionListener
    // ==========================================================

    @Override
    public void onConnecting(String deviceName) {
        runOnUiThread(() -> {
            viewStatusDot.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.temp_medium));
            tvConnectionStatus.setText(getString(R.string.status_connecting));
            btnConnectBt.setEnabled(false);
        });
    }

    @Override
    public void onConnected(String deviceName) {
        runOnUiThread(() -> {
            viewStatusDot.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.average_color));
            tvConnectionStatus.setText(String.format(getString(R.string.status_connected), deviceName));
            btnConnectBt.setText(getString(R.string.btn_disconnect_bt));
            btnConnectBt.setEnabled(true);
            updateSimulationButtonState(true);
            Toast.makeText(this, "Conectado ao Arduino (" + deviceName + ")", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDataReceived(float temperature, float humidity) {
        runOnUiThread(() -> {
            long now = System.currentTimeMillis();
            updateCurrentReadingUi(temperature, humidity, now);

            // Alerta Push de Onda de Calor / Temperatura Crítica
            HeatAlertNotificationHelper.checkAndTriggerAlert(
                    this,
                    selectedLocation,
                    temperature,
                    criticalThreshold
            );

            // Salva a leitura no banco de dados SQLite local
            String saveLocation = selectedLocation.equalsIgnoreCase("Todos os Pontos") ? "Ponto Geral" : selectedLocation;
            TemperatureReading reading = new TemperatureReading(saveLocation, temperature, humidity, now);

            repository.insertReading(reading, id -> loadHistoryAndStats());
        });
    }

    @Override
    public void onError(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            onDisconnected();
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            viewStatusDot.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.temp_hot));
            tvConnectionStatus.setText(getString(R.string.status_disconnected));
            btnConnectBt.setText(getString(R.string.btn_connect_bt));
            btnConnectBt.setEnabled(true);
            updateSimulationButtonState(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothService != null) {
            bluetoothService.disconnect();
        }
    }
}
