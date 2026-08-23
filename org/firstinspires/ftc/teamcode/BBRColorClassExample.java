// "Am I over red?" — the simplest way to consume the expander: one call per
// question. Classes must have been taught first (see BBRTeachColorExample)
// and the answers fail safe — a stale, saturated, dark, or disconnected
// sensor reads "class 0 / nothing", never a confident wrong answer.
package org.firstinspires.ftc.teamcode;

import com.botbuilders.bbr.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BBR: Color Classes", group = "BBR Examples")
public class BBRColorClassExample extends LinearOpMode {

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        waitForStart();
        while (opModeIsActive()) {
            for (int port = 0; port < 4; port++) {
                if (!exp.isSensorConnected(port)) {
                    telemetry.addData("port " + port, "no sensor");
                    continue;
                }
                switch (exp.getSensorType(port)) {
                    case COLOR:
                        // 0 = nothing taught in view; 1-7 = that colour.
                        telemetry.addData("port " + port, "sees colour %d",
                                exp.getColorClass(port));
                        break;
                    case DISTANCE:
                        // Infinity when nothing is in range, so this
                        // prints "Infinity" rather than a fake number.
                        telemetry.addData("port " + port, "%.0f mm",
                                exp.getDistanceMm(port));
                        break;
                    default:
                        telemetry.addData("port " + port, "unrecognised sensor");
                        break;
                }
            }
            telemetry.update();
        }
    }
}
