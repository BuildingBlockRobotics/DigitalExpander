// Encoder-threshold digital output: DOUT0 (GP7) goes high while encoder
// channel 0 reads THRESHOLD counts or more — "the arm is past the line" as
// a plain digital signal, no driver needed at runtime. The one-call setup
// saves to flash automatically. Spin the encoder by hand to cross the line;
// A resets the count back to zero.
// Optional hub-side readback on a digital input named "trigger".
package org.firstinspires.ftc.teamcode;

import com.buildingblockrobotics.expander.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DigitalChannel;

@TeleOp(name = "BBR: Encoder Trigger", group = "BBR Examples")
public class BBREncoderTriggerExample extends LinearOpMode {

    static final int CHANNEL = 0;
    static final int OUTPUT = 0;        // DOUT0 = GP7 (DOUT0/1 and DOUT2/3 are swapped on the board)
    static final int THRESHOLD = 1000;  // counts

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        DigitalChannel trigger = hardwareMap.tryGet(DigitalChannel.class, "trigger");
        if (trigger != null) trigger.setMode(DigitalChannel.Mode.INPUT);

        waitForStart();

        exp.triggerWhenEncoderPast(OUTPUT, CHANNEL, THRESHOLD);

        boolean prevA = false;
        while (opModeIsActive()) {
            if (gamepad1.a && !prevA) exp.resetEncoder(CHANNEL);
            prevA = gamepad1.a;

            telemetry.addLine("GP7 high at >= " + THRESHOLD + " counts; A = zero encoder");
            telemetry.addData("channel " + CHANNEL, "%d counts",
                    exp.getEncoderCount(CHANNEL));
            if (trigger != null) {
                telemetry.addData("hub pin", trigger.getState() ? "HIGH" : "low");
            }
            telemetry.update();
        }
    }
}
