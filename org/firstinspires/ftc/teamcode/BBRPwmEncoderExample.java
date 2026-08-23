// Absolute PWM encoder on a pulse-width channel (e.g. the REV Through Bore's
// absolute output, 1-1024 us per revolution). The expander measures the
// pulse width on the channel's A pin at 1 us resolution and, with wrap
// tracking enabled, accumulates position across revolutions on-device.
//
// getPulseWidthUs returns 0 for NO SIGNAL (deliberately outside the valid
// 1-65535 us range) — check for it instead of treating it as a position.
// It also refuses (throws) on a channel still in QUADRATURE mode, so a
// missing setChannelMode line is a clear message, not a garbage reading.
package org.firstinspires.ftc.teamcode;

import com.botbuilders.bbr.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BBR: PWM Encoder", group = "BBR Examples")
public class BBRPwmEncoderExample extends LinearOpMode {

    static final int CHANNEL = 0;

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        // One-time setup (persists across power cycles after
        // exp.saveConfigToFlash()): channel 0 measures pulse width, with the
        // REV Through Bore absolute range and multi-turn wrap tracking.
        exp.setChannelMode(CHANNEL, BBRDigitalExpander.ChannelMode.PULSE_WIDTH);
        exp.setPwmChannelParams(CHANNEL, 1, 1024);
        exp.setPwmWrapEnabled(CHANNEL, true);

        telemetry.addLine("A = zero the wrap count (position stays absolute in-turn)");
        telemetry.update();
        waitForStart();

        boolean prevA = false;
        while (opModeIsActive()) {
            if (gamepad1.a && !prevA) {
                // Zeroes only the wrap accumulator - the reading returns to
                // the raw in-turn pulse width, staying absolute.
                exp.resetEncoder(CHANNEL);
            }
            prevA = gamepad1.a;

            int count = exp.getPulseWidthUs(CHANNEL);
            if (count == 0) {
                telemetry.addLine("NO SIGNAL - check the encoder cable");
            } else {
                telemetry.addData("position", "%d us (multi-turn)", count);
                telemetry.addData("in-turn", "%d us", ((count - 1) % 1024 + 1024) % 1024 + 1);
                telemetry.addData("turns", "%.2f", count / 1024.0);
                telemetry.addData("velocity", "%d us/s", exp.getEncoderVelocity(CHANNEL));
            }
            telemetry.update();
        }
    }
}
