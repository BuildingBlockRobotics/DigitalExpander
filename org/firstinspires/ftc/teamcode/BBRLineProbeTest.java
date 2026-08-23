// Bring-up diagnostic: watches the raw digital level of all eight encoder
// inputs via ENC_PIN_STATE (0x9C, protocol 1.1 firmware). Spin each encoder
// SLOWLY by hand: a healthy line shows both LOW and HIGH; a line that only
// ever shows one level is the dead one — exact wire identified, no meter.
//
// Passive: no modes are changed and nothing is written to the device.
package org.firstinspires.ftc.teamcode;

import com.botbuilders.bbr.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BBR: Line Probe", group = "BBR Diagnostics")
public class BBRLineProbeTest extends LinearOpMode {

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        telemetry.addLine("Press START, then spin each encoder SLOWLY by hand.");
        telemetry.addLine("A healthy line shows \"LO+HI\"; a dead line stays at one level.");
        telemetry.update();
        waitForStart();

        // Which levels each of the 8 lines has ever shown: bit set once seen.
        int seenLow = 0, seenHigh = 0;

        while (opModeIsActive()) {
            int pins = exp.readEncoderPinState();
            seenHigh |= pins;
            seenLow |= ~pins & 0xFF;

            for (int ch = 0; ch < 4; ch++) {
                int aBit = 1 << (2 * ch), bBit = 1 << (2 * ch + 1);
                telemetry.addData("ENC" + ch,
                        "A=%d %-6s  B=%d %-6s",
                        (pins & aBit) != 0 ? 1 : 0, verdict(seenLow, seenHigh, aBit),
                        (pins & bBit) != 0 ? 1 : 0, verdict(seenLow, seenHigh, bBit));
            }
            telemetry.addLine();
            telemetry.addLine("LO+HI = line alive.  Stuck HI = open/unwired (pull-up).");
            telemetry.addLine("Stuck LO = shorted to ground.");
            telemetry.update();
        }
    }

    private static String verdict(int seenLow, int seenHigh, int bit) {
        boolean lo = (seenLow & bit) != 0, hi = (seenHigh & bit) != 0;
        if (lo && hi) return "LO+HI";
        return hi ? "hi..." : "lo...";
    }
}
