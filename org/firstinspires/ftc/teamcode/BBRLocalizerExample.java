// On-device absolute localizer (odometry variant only): two dead-wheel pods
// + onboard IMU, pose integrated at 1 kHz on the expander. One I2C read
// returns X/Y (mm), heading (rad), and velocities — no host-side math.
//
// One-time setup (persisted with saveConfigToFlash):
//   1. Wire the X (forward) pod and Y (left) pod to two encoder channels.
//   2. Set ENC_INVERT_MASK so forward/left travel counts positive.
//   3. Push the robot a measured 2 m along each axis; ticks / distance-mm
//      gives ticksPerMm for that pod.
//   4. Optionally calibrate imuScalar (spin 10 turns, correct the error) and
//      the TCP offsets — see the register map's Localizer section.
package org.firstinspires.ftc.teamcode;

import com.buildingblockrobotics.expander.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BBR: Localizer", group = "BBR Examples")
public class BBRLocalizerExample extends LinearOpMode {

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        // Example numbers for a goBILDA 4-bar pod (19.89 ticks/mm); replace
        // with values measured on YOUR robot (push-and-divide, not gear math).
        BBRDigitalExpander.LocalizerParams p = new BBRDigitalExpander.LocalizerParams();
        p.ticksPerMmX = 19.89;
        p.ticksPerMmY = 19.89;
        p.portX = 0;
        p.portY = 1;
        exp.setLocalizerParams(p);

        telemetry.addLine("Keep the robot STILL — calibrating on start");
        telemetry.addLine("A = re-zero pose, B = teleport to (500, -200, 90 deg)");
        telemetry.update();

        // Latch the parameters, zero the pose, calibrate the gyro, and wait
        // for RUNNING (~1-2 s total, robot still). Throws on a base board or
        // with unconfigured parameters — by design.
        exp.resetLocalizerAndCalibrateImu();
        if (!exp.waitForLocalizerReady(5000)) {
            telemetry.addLine("Localizer never reached RUNNING — is the robot moving?");
            telemetry.update();
           
        }

        waitForStart();

        

        boolean prevA = false, prevB = false;
        while (opModeIsActive()) {
            if (gamepad1.a && !prevA) {
                exp.setPose(new BBRDigitalExpander.Pose2D(0, 0, 0));
            }
            if (gamepad1.b && !prevB) {
                exp.setPose(BBRDigitalExpander.Pose2D.fromDegrees(500, -200, 90));
            }
            prevA = gamepad1.a;
            prevB = gamepad1.b;

            // A read torn down by STOP serves the last good pose instead of
            // throwing (isDataFresh() reports it), so no try/catch is needed.
            BBRDigitalExpander.LocalizerState s = exp.readLocalizerRaw();
            telemetry.addData("status", s.status);
            if (s.status == BBRDigitalExpander.LocalizerStatus.RUNNING) {
                telemetry.addData("pose", "x %.0f mm  y %.0f mm  h %.1f deg",
                        s.xMm, s.yMm, s.pose().headingDeg());
                telemetry.addData("vel", "%.0f / %.0f mm/s, %.2f rad/s",
                        s.velXMmPerSec, s.velYMmPerSec, s.headingVelRadPerSec);
            }
            if (s.hasGyroEverSaturated()) {
                // A collision clipped the gyro: heading is suspect from here.
                telemetry.addLine("WARNING: gyro saturated — heading may have drifted");
            }
            if (s.isPoseClipped()) {
                telemetry.addLine("WARNING: pose outside +/-32.7 m — reading clamped");
            }
            telemetry.update();
        }
    }
}
