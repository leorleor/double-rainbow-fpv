// usb otg rc receiver
// by leorleor



// Includes

#include <Servo.h>


// constants

// Arduino defined values
const int ASCII_ZERO = '0';

const int BYTE_MIN = 0;
const int BYTE_MAX = 255;

const int ANALOG_MIN = BYTE_MIN;
const int ANALOG_MAX = BYTE_MAX;

const int READ_MIN = 0;
const int READ_MAX = 1023;

const int IN_MIN = BYTE_MIN;
const int IN_MAX = 255;

// const int zeroMin = ASCII_ZERO;
// const int zeroMax = 126;
const int SERVO_MIN = 1000;
const int SERVO_MAX = 2000;

 const long BAUD = 115200;
// const long BAUD = 9600;

const int BUFFER_SIZE = 3;

const int SERIAL_UNAVAILABLE = -1;
const int ERROR_PIN = 13;
const int ANALOG_AUX_PIN = 11;
const int MIN_PWM_PIN = 2;
 const int PWM_PIN_COUNT = ERROR_PIN - MIN_PWM_PIN;
// const int PWM_PIN_COUNT = 6;
const int SERVO_COUNT = 5;

const int MIN_ANALOG_PIN = 0;
const int ANALOG_PIN_COUNT = 16;

const int MIN_DIGITAL_PIN = 22;
const int DIGITAL_PIN_COUNT = 32;

// messsage values

const int MESSAGE_TYPE_RESET = '!';

const int MESSAGE_TYPE_HELLO = '>';
const int MESSAGE_TYPE_CLEAR = ' ';
const int MESSAGE_TYPE_PING = '.';
const int MESSAGE_TYPE_HELP = '?';
const int MESSAGE_TYPE_ZERO = '0';
const int MESSAGE_TYPE_SERVO = 'S';
const int MESSAGE_TYPE_PIN = 'P';
const int MESSAGE_TYPE_DIGITAL = 'D';
const int MESSAGE_TYPE_ANALOG = 'A';
const int MESSAGE_TYPE_READ = 'R';

const int RESPONSE_TYPE_READ = 'r';

const int BLINK_MAX = 63;
const int TEST_A_MIN = 64;
const int TEST_A_MAX = 196;

// Changed to A7 after adding sensors
//const int RESET_PIN = 19;
const int RESET_PIN = 26;
const int RESET_DELAY_MS = 250;

const int DIGITAL_MIN = '0';
const int DIGITAL_MAX = '1';

const int INPUT_PIN = 'I';
const int OUTPUT_PIN = 'O';

const int TEST_SERVO_INDEX = (ERROR_PIN - 1) - MIN_PWM_PIN;

int zeroMin = IN_MIN;
int zeroMax = IN_MAX;


// global variables

Servo servoArray[PWM_PIN_COUNT];

int messageBuffer[BUFFER_SIZE];
int messageIndex = 0;
int nullCount = 0;
boolean messageOk = false;

long blinkPeriodMs = 666 * 2;
long blinkMs = 0;
int blinkState = LOW;



// forward declarations

// arduino overrides
void setup();
void loop();
void serialEvent();

void readSerial();
void readMessage();
void messageEvent();
void clearMessage(boolean ok);
void updateBlink();

// method definitions

void setup()
{
  pinMode(ERROR_PIN, OUTPUT);
  pinMode(ANALOG_AUX_PIN, OUTPUT);

  for (int servoIndex = 0; servoIndex < SERVO_COUNT; ++servoIndex) {
    // Let the throttle start at 0
    if (servoIndex == 2) {
      servoArray[servoIndex].writeMicroseconds(SERVO_MIN);
    } else {
      servoArray[servoIndex].writeMicroseconds(SERVO_MIN + ((SERVO_MAX - SERVO_MIN) / 2));
    }
    servoArray[servoIndex].attach(MIN_PWM_PIN + servoIndex);
  }

  messageBuffer[0] = MESSAGE_TYPE_HELLO;
  
  delay(100);
  
  Serial.begin(BAUD);
  clearMessage(true);
}

void loop() 
{
  readSerial();
//  updateBlink();
}

void updateBlink() {
  unsigned long nowMs = millis();
  int blinkValue = map((nowMs % blinkPeriodMs), 0, blinkPeriodMs, ANALOG_MIN, BLINK_MAX);
  analogWrite(ERROR_PIN, blinkValue);
  // int testValue = map((nowMs % blinkPeriodMs), 0, blinkPeriodMs, 128, 232);
  // int testServoValue = map((nowMs % blinkPeriodMs), 0, blinkPeriodMs, SERVO_MIN, SERVO_MAX);
  // servoArray[TEST_SERVO_INDEX].writeMicroseconds(testServoValue);
}

void serialEvent() {
  updateBlink();
//  do 
//  {
//    readSerial();
//  }
//  while (Serial.available());
}

void readSerial() 
{
  if (Serial.available()) 
  {
    updateBlink();
    int value = Serial.read();
    //    int value = Serial.parseInt();

    //    Serial.write(value);
    //    Serial.println("");
    //    Serial.flush();

    if (value == SERIAL_UNAVAILABLE) 
    {
      clearMessage(false);
    }

    readMessage(value);
  }
}

void clearMessage(boolean ok) {
  //  Serial.write(messageBuffer[0]);
  //  Serial.write(messageIndex);
  //  Serial.write(ok);
//  Serial.write(MESSAGE_TYPE_HELLO);
  Serial.write((byte)(messageBuffer[0]));
  Serial.write(ASCII_ZERO + messageIndex);
  Serial.write(ASCII_ZERO + ok);
//  Serial.println("");

  Serial.flush();

  messageBuffer[0] = MESSAGE_TYPE_CLEAR;
  messageIndex = 0;
  messageOk = ok;

  if (messageOk) 
  {
    analogWrite(ERROR_PIN, 127);
  } 
  else
  {
    analogWrite(ERROR_PIN, 191);
  }
}

void readMessage(int value) 
{
  if (!messageOk)
  {
    // Checks bounds.
    if ((messageIndex < 0) ||
      (messageIndex > BUFFER_SIZE)) 
    {
      clearMessage(false);
    }

    if (value == MESSAGE_TYPE_CLEAR) 
    {
      // Counts up to a full message of resets.
      if (messageIndex < BUFFER_SIZE) 
      {
        messageIndex++;
      }
    } 
    else 
    {
      // If a full message of clears was counted,
      // then this non-clear value begins an ok message.
      clearMessage(messageIndex == BUFFER_SIZE);
    }
  }

  if (messageOk) {
    // Checks bounds.
    if ((messageIndex >= 0) &&
      (messageIndex < BUFFER_SIZE)) 
    {
      messageBuffer[messageIndex++] = value;
      messageEvent();
    } 
    else 
    {
      clearMessage(false);
    }
  } 
}

void messageEvent() 
{
  // Checks for common and urgent messages first.
  if (messageBuffer[0] == MESSAGE_TYPE_ZERO)
  {
    if (messageIndex == 3) 
    {
      zeroMin = messageBuffer[1];
      zeroMax = messageBuffer[2];
      clearMessage(true);
    }
  }
  else if (messageBuffer[0] == MESSAGE_TYPE_SERVO) 
  {
    if (messageIndex == 3) 
    {
      int servoIndex = (messageBuffer[1] - ASCII_ZERO) - MIN_PWM_PIN;
      if ((servoIndex >= 0) && (servoIndex < PWM_PIN_COUNT))
      {
        servoArray[servoIndex].writeMicroseconds(map(messageBuffer[2], zeroMin, zeroMax, SERVO_MIN, SERVO_MAX));
        clearMessage(true);
      } 
      else 
      {
        clearMessage(false);
      }
    }
  } 
  else if (messageBuffer[0] == MESSAGE_TYPE_DIGITAL) 
  {
    if (messageIndex == 3) 
    {
      int pin = messageBuffer[1] - ASCII_ZERO;
      int value = map(messageBuffer[2], DIGITAL_MIN, DIGITAL_MAX, LOW, HIGH);
      if ((pin >= MIN_DIGITAL_PIN) && 
        (pin < MIN_DIGITAL_PIN + DIGITAL_PIN_COUNT) && 
        ((value == LOW) || 
        (value == HIGH)))
      {
        digitalWrite(pin, value);
        clearMessage(true);
        delay(1);
      } 
      else 
      {
        clearMessage(false);
      }
    }
  } 
  else if (messageBuffer[0] == MESSAGE_TYPE_PIN) 
  {
    if (messageIndex == 3) 
    {
      int pin = messageBuffer[1] - ASCII_ZERO;
      int value = map(messageBuffer[2], INPUT_PIN, OUTPUT_PIN, INPUT, OUTPUT);
      if (((value == INPUT) || 
        (value == OUTPUT))) 
      {
        if ((pin >= MIN_DIGITAL_PIN) && 
          (pin < MIN_DIGITAL_PIN + DIGITAL_PIN_COUNT))
        {
          pinMode(pin, value);
          clearMessage(true);
          delay(1);
        } 
        else if ((pin >= MIN_PWM_PIN) && 
          (pin < MIN_PWM_PIN + PWM_PIN_COUNT))
        {
          boolean valueAttach = (value == OUTPUT);
          int servoIndex = pin - MIN_PWM_PIN;
          if (valueAttach != servoArray[servoIndex].attached()) {
            if (valueAttach) 
            {
              servoArray[servoIndex].attach(pin);
            }
            else
            {
              servoArray[servoIndex].detach();
            }
          }
          clearMessage(true);
        }
        else 
        {
          clearMessage(false);
        }
      }
      else 
      {
        clearMessage(false);
      }
    }
  } 
  else if (messageBuffer[0] == MESSAGE_TYPE_ANALOG) 
  {
    if (messageIndex == 3) 
    {
      int pin = messageBuffer[1] - ASCII_ZERO;
      if ((pin >= MIN_ANALOG_PIN) && (pin < MIN_ANALOG_PIN + ANALOG_PIN_COUNT))
      {
        // analogWrite(5+pin, map(messageBuffer[2], zeroMin, zeroMax, ANALOG_MIN, ANALOG_MAX));
        analogWrite(ANALOG_AUX_PIN, map(messageBuffer[2], zeroMin, zeroMax, ANALOG_MIN, ANALOG_MAX));
        clearMessage(true);
        delay(1);
      } 
      else 
      {
        clearMessage(false);
      }
    }
  } 
  else if (messageBuffer[0] == MESSAGE_TYPE_READ) 
  {
    if (messageIndex == 2) 
    {
      int pin = messageBuffer[1] - ASCII_ZERO;
      if ((pin >= MIN_ANALOG_PIN) && (pin < MIN_ANALOG_PIN + ANALOG_PIN_COUNT))
      {
        clearMessage(true);
        delay(1);
        int value = analogRead(pin);
        Serial.write(RESPONSE_TYPE_READ);
        Serial.write((byte)(pin + ASCII_ZERO));
        Serial.write((byte)map(value, READ_MIN, READ_MAX, BYTE_MIN, BYTE_MAX));
//        Serial.write(ASCII_ZERO + (byte)map(value, READ_MIN, READ_MAX, 0, 9));
//        Serial.println(" ");
        delay(1);
      } 
      else 
      {
        clearMessage(false);
      }
    }
  } 
  else if (messageBuffer[0] == MESSAGE_TYPE_PING) 
  {
    clearMessage(true);
  } 
  else if (messageBuffer[0] == MESSAGE_TYPE_RESET) 
  {
    delay(RESET_DELAY_MS);
    clearMessage(true);
    delay(RESET_DELAY_MS);
//    pinMode(RESET_PIN, OUTPUT);
    delay(RESET_DELAY_MS);

    // Makes it clear that the reset failed.
    clearMessage(true);
  } 
  else if (messageBuffer[0] == MESSAGE_TYPE_HELP) 
  {
    Serial.write(MESSAGE_TYPE_SERVO);
    Serial.println(" - Servo");
    Serial.write(MESSAGE_TYPE_DIGITAL);
    Serial.println(" - Digital");
    Serial.write(MESSAGE_TYPE_PIN);
    Serial.println(" - Pin");
    Serial.write(MESSAGE_TYPE_ANALOG);
    Serial.println(" - Analog");
    Serial.write(MESSAGE_TYPE_READ);
    Serial.println(" - Read");
    Serial.write(MESSAGE_TYPE_PING);
    Serial.println(" - Ping");
    Serial.write(MESSAGE_TYPE_CLEAR);
    Serial.println(" - Clear");
    Serial.write(MESSAGE_TYPE_RESET);
    Serial.println(" - Reset");
    Serial.write(MESSAGE_TYPE_HELP);
    Serial.println(" - Help");
    Serial.flush();

    clearMessage(true);
  } 
  else
  {
    // Clears ok on any clear or unexpected message.
    clearMessage(false);
  }
}






