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

        // WHICH ENCODER CHANNELS THE PODS ARE ON. Any two of 0-3, as long as
        // they differ and both are set — the localizer refuses to start
        // otherwise. Both must stay in QUADRATURE mode: a channel switched to
        // PULSE_WIDTH is measuring a pulse width, not counting ticks, and
        // cannot drive the pose.
        p.portX = 0;   // pod that rolls when the robot drives forward/back
        p.portY = 1;   // pod that rolls when the robot strafes left/right
        exp.setLocalizerParams(p);
        exp.saveConfigToFlash();   // parameters do NOT save themselves

        // DIRECTION. The localizer needs forward and left travel to BOTH
        // count positive. It reads the channels itself, so negating a number
        // in your OpMode never reaches it — fix it here instead.
        //
        // Push the robot to find out which way each pod counts: driving
        // forward should raise the X count, strafing left should raise Y. If
        // one goes the wrong way, change its FORWARD to REVERSE.
        // These two DO save themselves, so the direction survives a reboot.
        exp.setEncoderDirection(0, BBRDigitalExpander.Direction.FORWARD);  // X pod
        exp.setEncoderDirection(1, BBRDigitalExpander.Direction.FORWARD);  // Y pod

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
            return;
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
