// Runtime half of the distance-trigger example: after BBRDistanceTriggerSetup
// ran once, the expander needs NO custom driver, NO I2C traffic, and NO
// loop-rate cost — the trigger is a stock DigitalChannel. This opmode does
// not even import the BBR driver.
//
// Configuration: expander digital output 0 wired to a Hub digital input,
// configured here as a Digital Device named "objectNear".
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DigitalChannel;

@TeleOp(name = "BBR: Distance Trigger Runtime", group = "BBR Examples")
public class BBRDistanceTriggerRuntime extends LinearOpMode {

    @Override
    public void runOpMode() {
        DigitalChannel objectNear = hardwareMap.get(DigitalChannel.class, "trigger");
        objectNear.setMode(DigitalChannel.Mode.INPUT);

        waitForStart();
        while (opModeIsActive()) {
            telemetry.addData("object within 300 mm", objectNear.getState());
            telemetry.update();
        }
    }
}
