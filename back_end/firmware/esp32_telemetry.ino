/*
 * FlowLab ESP32 sketch (optional).
 * Board: ESP32, library: ArduinoJson (Arduino Library Manager).
 *
 * Posts temperature / humidity / a short message to the backend.
 * The phone's Flow collector receives the same event over SSE.
 *
 * 1. Put your Wi-Fi and the computer's LAN IP below.
 * 2. Run the backend with: uvicorn app.main:app --host 0.0.0.0 --port 8000
 * 3. The seeded device key is esp32-demo-key (see README).
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

const char* WIFI_SSID = "YOUR_WIFI";
const char* WIFI_PASSWORD = "YOUR_PASSWORD";

// Computer running FlowLab, same network as the ESP32. Not "localhost".
const char* API_URL = "http://192.168.1.20:8000/api/devices/telemetry";
const char* DEVICE_KEY = "esp32-demo-key";

void setup() {
  Serial.begin(115200);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Wi-Fi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(400);
    Serial.print(".");
  }
  Serial.println();
  Serial.println(WiFi.localIP());
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    delay(2000);
    return;
  }

  // Replace with a real sensor (DHT22, BME280, etc.) when you have one.
  float temperature = 24.0 + (esp_random() % 70) / 10.0;
  float humidity = 45.0 + (esp_random() % 250) / 10.0;

  StaticJsonDocument<256> doc;
  doc["temperature"] = temperature;
  doc["humidity"] = humidity;
  doc["message"] = "esp32";

  String payload;
  serializeJson(doc, payload);

  HTTPClient http;
  http.begin(API_URL);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Key", DEVICE_KEY);
  int code = http.POST(payload);
  Serial.printf("POST %d  %s\n", code, payload.c_str());
  http.end();

  delay(4000);
}
