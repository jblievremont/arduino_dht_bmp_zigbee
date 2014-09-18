#include <dht.h>

dht DHT;

#define DHT22_PIN 2

void setup()
{
  Serial.begin(115200);
}

void loop()
{
  int chk = DHT.read22(DHT22_PIN);
  switch (chk)
  {
    case DHTLIB_OK:  
		Serial.print("{\"sensor\":\"DHT22\",\"type\":\"temperature\",\"value\":");
                Serial.print(DHT.temperature);
                Serial.println("}");                
		Serial.print("{\"sensor\":\"DHT22\",\"type\":\"humidity\",\"value\":");
                Serial.print(DHT.humidity);
                Serial.println("}");
		break;
    case DHTLIB_ERROR_CHECKSUM: 
		//Serial.print('Checksum error,\t'); 
		break;
    case DHTLIB_ERROR_TIMEOUT: 
		//Serial.print("Time out error,\t"); 
		break;
    default: 
		//Serial.print("Unknown error,\t"); 
		break;
  }

  delay(3000);
}

