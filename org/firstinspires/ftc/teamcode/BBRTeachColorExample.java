// Teach-by-example calibration: point the sensor at a target and press a
// button. The expander samples, builds a tolerance window around the
// reading, stores it, and the driver saves it to flash automatically — a
// taught colour survives power-off with no extra step to forget.
package org.firstinspires.ftc.teamcode;

import com.buildingblockrobotics.expander.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BBR: Teach Colors", group = "BBR Examples")
public class BBRTeachColorExample extends LinearOpMode {

    static final int PORT = 2;  // sensor to teach

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        telemetry.addLine("A = teach colour 1, B = teach colour 2");
        telemetry.addLine("(may take a few seconds; saved automatically)");
        telemetry.update();
        waitForStart();

        boolean prevA = false, prevB = false;
        String lastAction = "-";

        while (opModeIsActive()) {
            if (gamepad1.a && !prevA) {
                lastAction = teach(exp, 1);
            }
            if (gamepad1.b && !prevB) {
                lastAction = teach(exp, 2);
            }
            prevA = gamepad1.a;
            prevB = gamepad1.b;

            telemetry.addData("port " + PORT, "sees colour %d", exp.getColorClass(PORT));
            telemetry.addData("last action", lastAction);
            telemetry.update();
        }
    }

    private String teach(BBRDigitalExpander exp, int slot) {
        try {
            exp.teachColor(PORT, slot);
            return "taught colour " + slot + " (saved)";
        } catch (BBRDigitalExpander.BBRException e) {
            // Saturated / too dark / no sensor: refused rather than storing
            // a class that would silently never (or always) fire.
            return "colour " + slot + " NOT taught: " + e.getMessage();
        }
    }
}
