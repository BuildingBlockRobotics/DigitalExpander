// Bench test for the digital outputs: expander output 0 (GP7) goes high
// while the colour sensor on port 2 sees taught class 1. Configure once,
// then probe GP7 — 3.3 V on the taught target, 0 V off it.
//
// The everyday one-liner for this is exp.triggerOnColor(0, 2, 1), which
// also saves to flash. This bench version uses the advanced OutputConfig
// path instead so the config stays in RAM only — press Y to persist.
package org.firstinspires.ftc.teamcode;

import com.buildingblockrobotics.expander.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DigitalChannel;

@TeleOp(name = "BBR: Color Trigger Setup", group = "BBR Examples")
public class BBRColorTriggerSetup extends LinearOpMode {

    static final int SENSOR_PORT = 2;
    static final int CLASS_SLOT = 1;  // taught via "BBR: Teach Colors"
    static final int OUTPUT = 0;      // DOUT0 = GP7 (DOUT0/1 and DOUT2/3 are swapped on the board)

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        // GP7 jumpered to a Hub digital pin, configured as "trigger". Left
        // null if not in the config so the expander-side test still runs.
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

        // Only the non-defaults need setting: LEVEL mode and a 2-cycle
        // debounce are already the OutputConfig defaults.
        BBRDigitalExpander.OutputConfig out = new BBRDigitalExpander.OutputConfig();
        out.source = BBRDigitalExpander.OutputSource.SENSOR_CLASS;
        out.srcIndex = SENSOR_PORT;
        out.classIndex = CLASS_SLOT;
        exp.configureOutput(OUTPUT, out);

        boolean prevY = false;
        String lastAction = "configured (RAM)";

        while (opModeIsActive()) {
            if (gamepad1.y && !prevY) {
                exp.saveConfigToFlash();
                lastAction = "saved to flash";
            }
            prevY = gamepad1.y;

            telemetry.addLine("GP7: high while class 1 is seen; Y = save to flash");
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
