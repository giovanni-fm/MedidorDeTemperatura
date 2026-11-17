/*
====================================================================
 MONITOR DE MICROCLIMAS URBANOS & ILHAS DE CALOR
 Sensor DHT11 + Módulo Bluetooth HC-05
====================================================================

 O QUE ESTE PROGRAMA FAZ:
 - Lê a temperatura do ambiente (em graus Celsius).
 - Lê a umidade do ar (em porcentagem).
 - Mostra as duas informações no Monitor Serial do Arduino IDE.
 - Transmite os dados simultaneamente via Bluetooth (HC-05)
   para o aplicativo Android sem alterar nada na interface visual!
 - Avisa caso o sensor falhe na leitura.

 LIGAÇÕES ELÉTRICAS NO ARDUINO:
 1. Sensor DHT11:
    - DHT11 VCC  -> 5V do Arduino
    - DHT11 DATA -> Pino Digital 10 do Arduino
    - DHT11 GND  -> GND do Arduino

 2. Módulo Bluetooth HC-05:
    - HC-05 VCC  -> 5V do Arduino
    - HC-05 GND  -> GND do Arduino
    - HC-05 TXD  -> Pino Digital 2 (RX do SoftwareSerial no Arduino)
    - HC-05 RXD  -> Pino Digital 3 (TX do SoftwareSerial no Arduino, via divisor 1k/2k)

 COMO USAR:
 1. Instale a biblioteca "DHT sensor library" (Adafruit) na Arduino IDE.
 2. Conecte o Arduino pelo cabo USB e selecione a porta COM correta.
 3. Carregue o programa (Upload).
 4. Abra o Monitor Serial a 9600 baud para acompanhar no PC.
 5. No aplicativo Android, clique em "Conectar" no HC-05.
====================================================================
*/

#include <SoftwareSerial.h>
#include "DHT.h"

// Pinos do Bluetooth HC-05
#define BT_RX 2  // Conectado ao TXD do HC-05
#define BT_TX 3  // Conectado ao RXD do HC-05 (divisor resistivo)

SoftwareSerial bluetooth(BT_RX, BT_TX);

// Variáveis para guardar umidade e temperatura
float u, t;

// Sensor DHT11 conectado no pino digital 10
DHT DHT_sens(10, DHT11);

void setup() {
  // Prepara o sensor DHT11 para funcionar
  DHT_sens.begin();

  // Inicia a comunicação serial com o computador (9600 baud)
  Serial.begin(9600);

  // Inicia a comunicação serial com o Bluetooth HC-05 (9600 baud padrão)
  bluetooth.begin(9600);

  // Mensagem de boas-vindas no Monitor Serial do computador
  Serial.println("========================================");
  Serial.println("Monitor de Microclimas: DHT11 + HC-05");
  Serial.println("========================================");
  Serial.println(" ");
}

void loop() {

  // ===== ETAPA 1: LER O SENSOR =====
  u = DHT_sens.readHumidity();    // Mede a umidade
  t = DHT_sens.readTemperature(); // Mede a temperatura

  // ===== ETAPA 2: VERIFICAR SE A LEITURA DEU CERTO =====
  if (isnan(u) || isnan(t)) {
    Serial.println("Erro ao ler o DHT11!");
    delay(2000);
    return;
  }

  // ===== ETAPA 3: MOSTRAR O RESULTADO NO COMPUTADOR (MONITOR SERIAL) =====
  Serial.print("Umidade: ");
  Serial.print(u, 0);
  Serial.print(" %\t");

  Serial.print("Temperatura: ");
  Serial.print(t, 1);
  Serial.println(" *C");

  // ===== ETAPA 4: ENVIAR PARA O APLICATIVO ANDROID VIA BLUETOOTH =====
  // Envia em formato padronizado que o app Android interpreta imediatamente:
  bluetooth.print("{\"temp\":");
  bluetooth.print(t, 1);
  bluetooth.print(",\"hum\":");
  bluetooth.print(u, 1);
  bluetooth.println("}");

  // Espera 2 segundos antes da próxima medição
  delay(2000);
}
