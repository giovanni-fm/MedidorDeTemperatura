# 🌡️ Monitor de Microclimas Urbanos & Ilhas de Calor

Projeto integrado com **Arduino (DHT11 + HC-05)** e aplicativo **Android nativo** para coleta, análise comparativa de temperatura, evolução gráfica e alertas de ondas de calor extremas.

---

## 🎯 1. Contexto e Justificativa

### O que são Ilhas de Calor Urbanas?
Nas cidades e centros urbanos, a substituição da vegetação por asfalto, concreto e edificações faz com que o calor seja absorvido intensamente durante o dia e liberado lentamente à noite. Esse fenômeno cria **microclimas com temperaturas sensivelmente maiores** em áreas asfaltadas, pátios e estacionamentos quando comparados a refúgios térmicos como praças, bosques e áreas verdes.

### Justificativa Acadêmica e Prática:
- **Diagnóstico Local:** Permite posicionar o protótipo em diferentes pontos (ex: estacionamento asfaltado vs. área com árvores no campus/bairro).
- **Tomada de Decisão:** Gera dados empíricos para justificar o plantio de árvores, instalação de telhados verdes ou pintura reflexiva.
- **Saúde Pública:** Dispara alertas quando a temperatura ultrapassa faixas críticas de conforto térmico humano (estresse térmico / risco de desidratação e insolação).

---

## 🚀 2. Diferenciais Implementados no Aplicativo

1. **📈 Gráfico de Evolução Térmica Temporal (`ThermalGraphView`):**
   - Renderização limpa e fluida via Custom View nativa (zero dependências externas pesadas que poderiam quebrar builds futuros).
   - Destaque automático do **Pico de Calor (Máxima)** em vermelho.
   - Destaque do **Ponto de Menor Temperatura (Mínima)** em azul.
   - Traçado dinâmico da **Linha de Média Térmica** em verde pontilhado.
2. **🔔 Alertas Push de Onda de Calor Extrema:**
   - O usuário pode regular dinamicamente o **Limite Crítico** (ex: 32.0°C).
   - Ao receber uma medição acima do limite, o app emite uma **Notificação de Alta Prioridade** com canal sonoro e vibração.
3. **💾 Banco de Dados SQLite Local & Repositório:**
   - Salva o histórico completo associando timestamp, temperatura, umidade e identificação do microclima.
4. **📍 Seletor e Cadastro de Microclimas:**
   - Permite comparar "Área Asfaltada", "Área Arborizada", "Pátio Escolar" ou criar novos pontos sob demanda.
5. **🧪 Modo Demonstração / Simulação Integrado:**
   - Botão para gerar dados instantâneos realistas, permitindo testar e demonstrar gráficos, estatísticas e alertas push em sala de aula mesmo quando o hardware não estiver ligado por perto.

---

## 🔌 3. Hardware e Esquema Elétrico do Arduino

### Componentes:
- 1x **Arduino Uno / Nano / Mega**
- 1x **Sensor DHT11** (Temperatura e Umidade)
- 1x **Módulo Bluetooth HC-05** (Serial SPP)
- 1x Resistor 1kΩ e 1x Resistor 2kΩ (Divisor de tensão no RX do HC-05)

### Esquema de Ligação:
```
[Sensor DHT11]
  VCC / +   ------> 5V do Arduino
  GND / -   ------> GND do Arduino
  DATA / S  ------> Pino Digital D2 do Arduino

[Módulo Bluetooth HC-05]
  VCC       ------> 5V do Arduino
  GND       ------> GND do Arduino
  TXD       ------> Pino Digital D10 do Arduino (SoftwareSerial RX)
  RXD       ------> Divisor de tensão com Arduino D11:
                      D11 ----[ 1kΩ ]----+----> RXD do HC-05
                                        |
                                     [ 2kΩ ]
                                        |
                                       GND
```
> **Nota de Segurança:** O pino RXD do HC-05 opera em 3.3V. O divisor 1kΩ / 2kΩ reduz os 5V do Arduino para ~3.3V, protegendo o módulo contra queima.

---

## 🛠️ 4. Como Compilar e Executar

### No Arduino:
1. Abra a Arduino IDE e instale a biblioteca **DHT sensor library** (Adafruit).
2. Abra o arquivo localizado em `arduino/monitor_microclima.ino`.
3. Selecione a placa (ex: Arduino Uno) e a porta COM.
4. Clique em **Upload**.

### No Celular Android:
1. Vá nas **Configurações de Bluetooth do Celular** e pareie o módulo **HC-05** (código PIN padrão: `1234` ou `0000`).
2. Abra este projeto no **Android Studio** e clique em **Run (▶)**.
3. No app, clique no botão **Conectar** e selecione o **HC-05**.
4. Acompanhe as leituras em tempo real, as estatísticas e os gráficos!

---

## 🧩 5. Estrutura e Manutenibilidade do Código

O projeto segue a separação de responsabilidades em camadas limpas:
- `data.model`: Classes de modelo puras (`TemperatureReading`, `ThermalStats`).
- `data.db`: Gerenciador de persistência local SQLite (`DatabaseHelper`).
- `data.repository`: Camada de abstração e execução em background (`ReadingRepository`).
- `bluetooth`: Gerenciamento do ciclo de vida e parsing do socket SPP (`BluetoothService`, `BluetoothConnectionListener`).
- `notification`: Canal de notificação e alertas push (`HeatAlertNotificationHelper`).
- `ui`: Custom View performática (`ThermalGraphView`) e Activity (`MainActivity`).
