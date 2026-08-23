// Latched digital output: GP7 goes high the first time the colour sensor on
// port 2 sees taught class 1 and STAYS high after the colour is gone — "did
// we ever see it", not "are we seeing it now". Press B to clear the latch
// (clearing while the colour is still in view re-asserts immediately).
// Optional hub-side readback on a digital input named "trigger".
//
// Uses the advanced OutputConfig path because of the LATCHED mode; note it
// does NOT auto-save — press-to-save is deliberate on a bench test.
package org.firstinspires.ftc.teamcode;

import com.buildingblockrobotics.expander.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DigitalChannel;

@TeleOp(name = "BBR: Color Latch", group = "BBR Examples")
public class BBRColorLatchExample extends LinearOpMode {

    static final int SENSOR_PORT = 2;
    static final int CLASS_SLOT = 1;  // taught via "BBR: Teach Colors"
    static final int OUTPUT = 0;      // DOUT0 = GP7 (DOUT0/1 and DOUT2/3 are swapped on the board)

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        DigitalChannel trigger = hardwareMap.tryGet(DigitalChannel.class, "trigger");
        if (trigger != null) trigger.setMode(DigitalChannel.Mode.INPUT);

        waitForStart();

        if (exp.getSensorType(SENSOR_PORT) != BBRDigitalExpander.SensorType.COLOR) {
            telemetry.addData("ABORT", "port %d is %s, expected COLOR",
                    SENSOR_PORT, exp.getSensorType(SENSOR_PORT));
            telemetry.update();
            while (opModeIsActive()) { sleep(50); }
            return;
        }

        BBRDigitalExpander.OutputConfig out = new BBRDigitalExpander.OutputConfig();
        out.source = BBRDigitalExpander.OutputSource.SENSOR_CLASS;
        out.srcIndex = SENSOR_PORT;
        out.classIndex = CLASS_SLOT;
        out.mode = BBRDigitalExpander.OutputMode.LATCHED;
        exp.configureOutput(OUTPUT, out);

        boolean prevB = false;
        String lastAction = "configured (RAM, latched)";

        while (opModeIsActive()) {
            if (gamepad1.b && !prevB) {
                exp.clearOutputLatch(OUTPUT);
                lastAction = "latch cleared";
            }
            prevB = gamepad1.b;

            telemetry.addLine("GP7 latches high on class 1; B = clear latch");
            telemetry.addData("port " + SENSOR_PORT, "sees colour %d",
                    exp.getColorClass(SENSOR_PORT));
            if (trigger != null) {
                telemetry.addData("hub pin", trigger.getState() ? "HIGH" : "low");
            }
            telemetry.addData("last action", lastAction);
            telemetry.update();
        }
    }
}
