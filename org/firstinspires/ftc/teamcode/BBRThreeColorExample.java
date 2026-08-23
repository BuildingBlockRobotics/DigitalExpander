// Three game colours, one digital output each — the typical season setup in
// four everyday calls: teachColor once per colour, triggerOnColor once per
// output. Both save to flash automatically, so after this runs once the
// expander does the job standalone forever — runtime reads are stock
// DigitalChannels (configure hub inputs as "trigger0".."trigger2"), no BBR
// driver needed.
//
// Teach: point at a target, A = colour 1, B = colour 2, X = colour 3.
// Outputs: DOUT0/1/2 land on GP7/GP6/GP9 — the outputs are swapped
// within each pair on the real board. They go high
// while their colour is seen.
package org.firstinspires.ftc.teamcode;

import com.buildingblockrobotics.expander.BBRDigitalExpander;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DigitalChannel;

@TeleOp(name = "BBR: Three Color Trigger", group = "BBR Examples")
public class BBRThreeColorExample extends LinearOpMode {

    static final int SENSOR_PORT = 2;
    static final int NUM_COLORS = 3;   // class slots 1..3 -> DOUT0..2

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        // Optional hub-side readback: digital inputs named trigger0..trigger2,
        // jumpered from GP7/GP6/GP9. Skipped if not in the robot config.
        DigitalChannel[] pins = new DigitalChannel[NUM_COLORS];
        for (int i = 0; i < NUM_COLORS; i++) {
            pins[i] = hardwareMap.tryGet(DigitalChannel.class, "trigger" + i);
            if (pins[i] != null) pins[i].setMode(DigitalChannel.Mode.INPUT);
        }

        telemetry.addLine("A/B/X = teach colour 1/2/3 (saved automatically)");
        telemetry.update();
        waitForStart();

        // One output per colour: DOUTn follows colour n+1 on the sensor port.
        for (int i = 0; i < NUM_COLORS; i++) {
            exp.triggerOnColor(i, SENSOR_PORT, i + 1);
        }

        boolean prevA = false, prevB = false, prevX = false;
        String lastAction = "-";

        while (opModeIsActive()) {
            if (gamepad1.a && !prevA) lastAction = teach(exp, 1);
            if (gamepad1.b && !prevB) lastAction = teach(exp, 2);
            if (gamepad1.x && !prevX) lastAction = teach(exp, 3);
            prevA = gamepad1.a;
            prevB = gamepad1.b;
            prevX = gamepad1.x;

            telemetry.addData("port " + SENSOR_PORT, "sees colour %d",
                    exp.getColorClass(SENSOR_PORT));
            for (int i = 0; i < NUM_COLORS; i++) {
                if (pins[i] != null) {
                    telemetry.addData("colour " + (i + 1) + " pin",
                            pins[i].getState() ? "HIGH" : "low");
                }
            }
            telemetry.addData("last action", lastAction);
            telemetry.update();
        }
    }

    private String teach(BBRDigitalExpander exp, int slot) {
        try {
            exp.teachColor(SENSOR_PORT, slot);
            return "taught colour " + slot + " (saved)";
        } catch (BBRDigitalExpander.BBRException e) {
            return "colour " + slot + " NOT taught: " + e.getMessage();
        }
    }
}
