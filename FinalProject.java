package FinalContentProject;

import org.firmata4j.I2CDevice;
import org.firmata4j.Pin;
import org.firmata4j.firmata.FirmataDevice;
import org.firmata4j.ssd1306.SSD1306;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class FinalProject {
    public static void main(String[] args) throws IOException, InterruptedException {
        var myGroveBoard = new FirmataDevice("COM4");
        myGroveBoard.start();
        System.out.println("Board Started");
        myGroveBoard.ensureInitializationIsDone();

        I2CDevice i2cObject = myGroveBoard.getI2CDevice((byte) 0x3C);
        SSD1306 theOledObject = new SSD1306(i2cObject, SSD1306.Size.SSD1306_128_64);

        theOledObject.init();
        var moistureSensor = myGroveBoard.getPin(15);
        var pumpPin = myGroveBoard.getPin(2);
        var button = myGroveBoard.getPin(6);
        moistureSensor.setMode(Pin.Mode.ANALOG);
        button.setMode(Pin.Mode.INPUT);

        var mainTimer = new Timer();

        // Add a shutdown hook to ensure the pump is turned off when the program exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pumpPin.setValue(0);
                System.out.println("Pump turned off");
            } catch (IOException e) {
                e.printStackTrace();
            }
            mainTimer.cancel(); // Cancel the timer to stop the tasks
        }));

        mainTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                theOledObject.clear();

                // Updated calibration values
                int dryValue = 300;
                int wetValue = 800;

                // Calculate moisture percentage
                double moistureDecimal = ((moistureSensor.getValue() - dryValue) / (double)(wetValue - dryValue)) * 100;
                int moisturePercentage = 100 - (int) moistureDecimal;
                String moistureValue = "Moisture: " + moisturePercentage + "%";

                // Calculate actual voltage
                double voltage = moistureSensor.getValue() * (5.0 / 1023);
                String voltageValue = String.format("Voltage: %.2f V", voltage);

                System.out.println("Moisture: " + moisturePercentage + "%, Voltage: " + voltage + " V");

                var overrideState = button.getValue();

                if (moisturePercentage < 50 || overrideState == 1) {
                    try {
                        theOledObject.getCanvas().drawString(0, 0, moistureValue);
                        theOledObject.getCanvas().drawString(0, 10, voltageValue);

                        if (overrideState == 1) {
                            System.out.println("Manual Button Override is Active!");
                            theOledObject.getCanvas().drawString(0, 20, "Override: True");
                        }

                        theOledObject.getCanvas().drawString(0, 30, "Soil is dry");
                        theOledObject.getCanvas().drawString(0, 40, "Pumping: True");
                        theOledObject.display();
                        pumpPin.setValue(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        pumpPin.setValue(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    theOledObject.getCanvas().drawString(0, 0, moistureValue);
                    theOledObject.getCanvas().drawString(0, 10, voltageValue);
                    theOledObject.getCanvas().drawString(0, 20, "Soil is wet");
                    theOledObject.getCanvas().drawString(0, 30, "Pumping: False");
                    theOledObject.display();
                }
            }
        }, 0, 380);
    }
}
