// IMU heading for odometry (odometry variant only). The driver fails LOUDLY
// on a base board or a faulty IMU rather than returning a plausible heading
// of 0.00 forever — run this on a base board to see the message.
//
// Yaw is relative (six-axis, no magnetometer — deliberate: FTC robots are
// steel and motors): zero it at match start, expect slow drift over minutes.
package org.firstinspires.ftc.teamcode;

import com.botbuilders.bbr.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BBR: IMU Heading", group = "BBR Examples")
public class BBRImuHeadingExample extends LinearOpMode {

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        telemetry.addLine("A = re-zero heading, B = calibrate gyro (keep robot still)");
        telemetry.update();
        waitForStart();
        exp.resetHeading();  // throws on a base board — by design

        boolean prevA = false, prevB = false;
        String calMsg = "-";
        while (opModeIsActive()) {
            if (gamepad1.a && !prevA) {
                exp.resetHeading();
            }
            if (gamepad1.b && !prevB) {
                try {
                    exp.calibrateGyro();
                    calMsg = "calibrated (save to flash to keep)";
                } catch (BBRDigitalExpander.BBRException e) {
                    calMsg = e.getMessage();  // e.g. robot moved during sample
                }
            }
            prevA = gamepad1.a;
            prevB = gamepad1.b;

            // A read torn down by STOP serves the last good sample instead of
            // throwing (isDataFresh() reports it), so no try/catch is needed.
            BBRDigitalExpander.ImuState imu = exp.readImu();
            telemetry.addData("heading", "%.2f deg (left positive)", imu.yawDeg);
            telemetry.addData("pitch/roll", "%.2f / %.2f deg", imu.pitchDeg, imu.rollDeg);
            telemetry.addData("turn rate", "%.1f dps", imu.gyroDps[2]);
            telemetry.addData("calibration", "grade %d, %s", imu.calibGrade, calMsg);
            if (imu.isGyroSaturated()) {
                // A collision clipped the gyro: heading is suspect from here.
                telemetry.addLine("WARNING: gyro saturated — heading may be wrong");
            }
            telemetry.update();
        }
    }
}
