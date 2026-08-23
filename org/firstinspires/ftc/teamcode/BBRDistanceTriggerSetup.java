// The canonical use case, end to end: a REV 2m Distance Sensor on port 2
// drives expander digital output 0 high when an object comes within 300 mm.
// Run this ONCE as setup; after it saves, the expander does the work forever
// with no custom driver at runtime — see BBRDistanceTriggerRuntime.
//
// The whole configuration is one everyday call: triggerWhenNear checks the
// arguments, builds the range window with working defaults (hysteresis,
// signal-rate floor), points the output at it, and saves to flash. The
// advanced path (DistanceClass + OutputConfig) is still there for custom
// hysteresis or two-sided windows — see the register map's worked example.
package org.firstinspires.ftc.teamcode;

import com.botbuilders.bbr.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DigitalChannel;

@Autonomous(name = "BBR: Distance Trigger Setup", group = "BBR Examples")
public class BBRDistanceTriggerSetup extends LinearOpMode {

    static final int SENSOR_PORT = 2;
    static final int OUTPUT = 0;
    static final int TRIGGER_MM = 300;

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        waitForStart();

        // Confirm what is actually plugged in before configuring. A colour
        // sensor on this port would make triggerWhenNear's window meaningless
        // — and getDistanceMm below would throw the same complaint anyway.
        if (exp.getSensorType(SENSOR_PORT) != BBRDigitalExpander.SensorType.DISTANCE) {
            telemetry.addData("ABORT", "port %d is %s, expected DISTANCE",
                    SENSOR_PORT, exp.getSensorType(SENSOR_PORT));
            telemetry.update();
            while (opModeIsActive()) { sleep(50); }
            return;
        }

        // Range window + output + save-to-flash, one call.
        exp.triggerWhenNear(OUTPUT, SENSOR_PORT, TRIGGER_MM);

        // Live readings, so the trigger can be confirmed against the pin it
        // drives: wire OUT0 to a Hub digital input configured as "trigger".
        DigitalChannel trigger = hardwareMap.tryGet(DigitalChannel.class, "trigger");
        if (trigger != null) trigger.setMode(DigitalChannel.Mode.INPUT);

        while (opModeIsActive()) {
            telemetry.addLine("Configured and saved. GP7 high within " + TRIGGER_MM + " mm.");
            double mm = exp.getDistanceMm(SENSOR_PORT);
            if (mm == Double.POSITIVE_INFINITY) {
                telemetry.addData("distance", "no valid return");
            } else {
                telemetry.addData("distance", "%.0f mm", mm);
            }
            if (trigger != null) {
                telemetry.addData("hub pin", trigger.getState() ? "HIGH" : "low");
            }
            telemetry.update();
        }
    }
}
