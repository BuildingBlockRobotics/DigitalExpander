// Four extra quadrature encoders with firmware-computed velocities. One
// call per question — getEncoderCount / getEncoderVelocity — with no masks
// or telemetry blocks; the driver batches everything into one I2C snapshot
// behind the scenes, so readings can never tear or mix samples.
package org.firstinspires.ftc.teamcode;

import com.buildingblockrobotics.expander.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BBR: Encoders", group = "BBR Examples")
public class BBREncoderExample extends LinearOpMode {

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        waitForStart();
        // Zero all four channels at start. Idempotency tokens make this safe
        // against transport retries — it can never zero twice.
        exp.resetAllEncoders();

        boolean prevX = false;
        while (opModeIsActive()) {
            if (gamepad1.x && !prevX) {
                exp.resetEncoder(0);  // re-zero channel 0 only
            }
            prevX = gamepad1.x;

            for (int ch = 0; ch < 4; ch++) {
                telemetry.addData("enc " + ch, "%d counts, %d counts/s",
                        exp.getEncoderCount(ch), exp.getEncoderVelocity(ch));
            }
            telemetry.addLine("X = re-zero channel 0");
            telemetry.update();
        }
    }
}
