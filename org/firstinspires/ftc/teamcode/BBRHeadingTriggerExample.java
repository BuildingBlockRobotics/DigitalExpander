// Heading trigger (odometry variant only): expander output 0 (GP7) goes
// high while the robot is facing a set heading, within a tolerance. The
// board does the comparison itself at loop rate, so a match OpMode reads
// the answer as a plain DigitalChannel with no BBR driver involved.
//
// Run once with A to arm the current heading, then drive the robot around
// and watch the pin. Y saves it so it survives power-off.
//
// The window may straddle +/-180 — aim the robot backwards and press A to
// see that work; the board handles the wrap, not your code.
package org.firstinspires.ftc.teamcode;

import com.buildingblockrobotics.expander.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DigitalChannel;

@TeleOp(name = "BBR: Heading Trigger", group = "BBR Examples")
public class BBRHeadingTriggerExample extends LinearOpMode {

    static final int OUTPUT = 0;             // DOUT0 = GP7 (DOUT0/1 and DOUT2/3 are swapped on the board)
    static final double TOLERANCE_DEG = 15;  // +/- either side of the target

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        // GP7 jumpered to a Hub digital pin configured as "trigger". Left
        // null if it is not in the robot configuration, so the expander-side
        // half of the test still runs.
        DigitalChannel trigger = hardwareMap.tryGet(DigitalChannel.class, "trigger");
        if (trigger != null) trigger.setMode(DigitalChannel.Mode.INPUT);

        telemetry.addLine("A = arm trigger at current heading, Y = save to flash");
        telemetry.update();
        waitForStart();

        // Throws on a base board rather than reporting a heading of 0.00
        // forever — that failure looks like working software, which is worse.
        exp.resetHeading();

        boolean prevA = false, prevY = false;
        String armed = "not armed";

        while (opModeIsActive()) {
            BBRDigitalExpander.ImuState imu = exp.readImu();

            if (gamepad1.a && !prevA) {
                try {
                    exp.triggerWhenFacing(OUTPUT, imu.yawDeg, TOLERANCE_DEG);
                    armed = String.format("%.1f +/- %.0f deg", imu.yawDeg, TOLERANCE_DEG);
                } catch (BBRDigitalExpander.BBRException e) {
                    // Shown to the driver instead of a stack trace: a failed
                    // arm is something they can react to at the field.
                    armed = "failed: " + e.getMessage();
                }
            }
            if (gamepad1.y && !prevY) {
                exp.saveConfigToFlash();
                armed += " (saved)";
            }
            prevA = gamepad1.a;
            prevY = gamepad1.y;

            telemetry.addData("heading", "%.2f deg (left positive)", imu.yawDeg);
            telemetry.addData("armed at", armed);
            if (trigger != null) {
                telemetry.addData("hub pin", trigger.getState() ? "HIGH" : "low");
            } else {
                telemetry.addLine("probe GP7: 3.3 V inside the window, 0 V outside");
            }
            telemetry.update();
        }
    }
}
