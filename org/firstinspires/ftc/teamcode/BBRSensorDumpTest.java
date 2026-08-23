// Bring-up diagnostic: dumps every live value the board publishes — all four
// sensor ports (raw channels, not just the classified answer), the encoders,
// the device STATUS bits, and the IMU when fitted. Use it to confirm a sensor
// is actually wired to the port you think it is, and that its numbers move
// when you wave a hand at it.
//
// Each pass is ONE 96-byte telemetry read, so every number on the screen came
// from the same firmware sample — a port reading zero is really zero, not a
// torn read. Raw channels are shown deliberately: a colour sensor that is
// present but unclassified (class 0) looks identical to a dead one until you
// can see r/g/b/ir moving.
//
// Passive: nothing is written to the device, no modes or classes are changed.
package org.firstinspires.ftc.teamcode;

import com.botbuilders.bbr.BBRDigitalExpander;
import com.botbuilders.bbr.BBRRegMap;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BBR: Sensor Dump", group = "BBR Diagnostics")
public class BBRSensorDumpTest extends LinearOpMode {

    @Override
    public void runOpMode() {
        BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

        // Identity is read once at init — worth showing, because "wrong
        // variant" explains a missing IMU block before you go hunting.
        telemetry.addData("variant", exp.isOdometryVariant() ? "odometry" : "base");
        telemetry.addData("protocol", "1.%d", exp.getProtocolMinor());
        telemetry.addData("capabilities", capsName(exp.getCapabilities()));
        telemetry.addLine();
        telemetry.addLine("Press START to stream. Wave a hand at each sensor:");
        telemetry.addLine("the raw numbers must MOVE, or that port is not wired.");
        telemetry.update();
        waitForStart();

        boolean hasImu = (exp.getCapabilities() & BBRRegMap.CAP_IMU) != 0;

        while (opModeIsActive()) {
            BBRDigitalExpander.TelemetryBlock t = exp.readTelemetry();

            int status = exp.readStatus();
            telemetry.addData("STATUS", "0x%02X %s", status, statusName(status));
            // false = the last read served stale data (teardown or bus trouble)
            telemetry.addData("data fresh", exp.isDataFresh());
            telemetry.addData("present mask", "0x%02X", t.presentMask);
            telemetry.addData("sample age", "%d us stamp", t.timestampMicros);
            telemetry.addLine();

            for (int port = 0; port < 4; port++) {
                boolean present = (t.presentMask & (1 << port)) != 0;
                telemetry.addData("PORT " + port, "%s%s",
                        t.sensorType[port],
                        present ? "" : "  (not present)");

                if (!present) continue;

                switch (t.sensorType[port]) {
                    case COLOR:
                        int[] c = t.rawColor[port];
                        telemetry.addData("  raw", "r=%5d g=%5d b=%5d ir=%5d prox=%4d",
                                c[0], c[1], c[2], c[3], c[4]);
                        break;
                    case DISTANCE:
                        telemetry.addData("  range", "%s  signal=%d ambient=%d rstat=%d",
                                t.isDistanceValid(port) ? t.distanceMm[port] + " mm" : "INVALID",
                                t.signalRate[port], t.ambientRate[port], t.rangeStatus[port]);
                        break;
                    default:
                        // Present but unrecognised: the ID register answered
                        // with something the firmware does not know.
                        telemetry.addLine("  no decoder for this sensor type");
                        break;
                }
                telemetry.addData("  class", "%d (confidence %d)",
                        t.sensorClass[port], t.classConfidence[port]);
                telemetry.addData("  sstat", "0x%02X %s",
                        t.sensorStatus[port], sensorStatusName(t.sensorStatus[port]));
            }

            telemetry.addLine();
            for (int ch = 0; ch < 4; ch++) {
                // In PULSE_WIDTH mode these are microseconds and us/s, not
                // counts — the mode is per-channel config, so label loosely.
                telemetry.addData("ENC" + ch, "%d  (vel %d /s)",
                        t.encoderCounts[ch], t.encoderVelocities[ch]);
            }

            if (hasImu) {
                telemetry.addLine();
                try {
                    BBRDigitalExpander.ImuState imu = exp.readImu();
                    telemetry.addData("IMU ypr", "%.2f / %.2f / %.2f deg",
                            imu.yawDeg, imu.pitchDeg, imu.rollDeg);
                    telemetry.addData("IMU gyro", "%.1f %.1f %.1f dps",
                            imu.gyroDps[0], imu.gyroDps[1], imu.gyroDps[2]);
                    telemetry.addData("IMU accel", "%.2f %.2f %.2f m/s^2",
                            imu.linearAccel[0], imu.linearAccel[1], imu.linearAccel[2]);
                    telemetry.addData("IMU istat", "0x%02X %s  calib=%s",
                            imu.imuStatus, imuStatusName(imu.imuStatus),
                            calibName(imu.calibGrade));
                } catch (BBRDigitalExpander.BBRException e) {
                    // A fitted-but-faulty IMU throws rather than reporting a
                    // heading of 0.00 forever — show it instead of dying.
                    telemetry.addData("IMU", e.getMessage());
                }
            }

            telemetry.update();
        }
    }

    private static String capsName(int caps) {
        StringBuilder s = new StringBuilder();
        if ((caps & BBRRegMap.CAP_ENCODERS) != 0) s.append("ENCODERS ");
        if ((caps & BBRRegMap.CAP_COLOR) != 0) s.append("COLOR ");
        if ((caps & BBRRegMap.CAP_IMU) != 0) s.append("IMU ");
        if ((caps & BBRRegMap.CAP_DOUT) != 0) s.append("DOUT ");
        return s.length() == 0 ? "(none)" : s.toString().trim();
    }

    private static String statusName(int st) {
        StringBuilder s = new StringBuilder();
        if ((st & BBRRegMap.STATUS_READY) != 0) s.append("READY ");
        if ((st & BBRRegMap.STATUS_CFG_DIRTY) != 0) s.append("CFG_DIRTY ");
        if ((st & BBRRegMap.STATUS_FLASH_ERROR) != 0) s.append("FLASH_ERROR ");
        if ((st & BBRRegMap.STATUS_SENSOR_BUS_ERROR) != 0) s.append("SENSOR_BUS_ERROR ");
        if ((st & BBRRegMap.STATUS_IMU_PRESENT) != 0) s.append("IMU_PRESENT ");
        if ((st & BBRRegMap.STATUS_IMU_CALIBRATED) != 0) s.append("IMU_CALIBRATED ");
        if ((st & BBRRegMap.STATUS_CMD_OVERRUN) != 0) s.append("CMD_OVERRUN ");
        if ((st & BBRRegMap.STATUS_CFG_DEFAULTED) != 0) s.append("CFG_DEFAULTED ");
        return s.length() == 0 ? "(clear)" : s.toString().trim();
    }

    private static String sensorStatusName(int ss) {
        StringBuilder s = new StringBuilder();
        if ((ss & BBRRegMap.SSTAT_VALID) != 0) s.append("VALID ");
        if ((ss & BBRRegMap.SSTAT_STALE) != 0) s.append("STALE ");
        if ((ss & BBRRegMap.SSTAT_QUALITY_FAIL) != 0) s.append("QUALITY_FAIL ");
        if ((ss & BBRRegMap.SSTAT_OVERFLOW) != 0) s.append("OVERFLOW ");
        if ((ss & BBRRegMap.SSTAT_BUS_ERROR) != 0) s.append("BUS_ERROR ");
        if ((ss & BBRRegMap.SSTAT_INSUFFICIENT_SIGNAL) != 0) s.append("INSUFFICIENT_SIGNAL ");
        return s.length() == 0 ? "(clear)" : s.toString().trim();
    }

    private static String imuStatusName(int is) {
        StringBuilder s = new StringBuilder();
        if ((is & BBRRegMap.ISTAT_PRESENT) != 0) s.append("PRESENT ");
        if ((is & BBRRegMap.ISTAT_FUSION_VALID) != 0) s.append("FUSION_VALID ");
        if ((is & BBRRegMap.ISTAT_SATURATED) != 0) s.append("SATURATED ");
        if ((is & BBRRegMap.ISTAT_BIAS_VALID) != 0) s.append("BIAS_VALID ");
        if ((is & BBRRegMap.ISTAT_STATIONARY) != 0) s.append("STATIONARY ");
        if ((is & BBRRegMap.ISTAT_VIBRATION) != 0) s.append("VIBRATION ");
        if ((is & BBRRegMap.ISTAT_FAULT) != 0) s.append("FAULT ");
        if ((is & BBRRegMap.ISTAT_UNEXPECTED) != 0) s.append("UNEXPECTED ");
        return s.length() == 0 ? "(clear)" : s.toString().trim();
    }

    private static String calibName(int grade) {
        switch (grade) {
            case BBRRegMap.CALIB_NONE: return "NONE";
            case BBRRegMap.CALIB_COARSE: return "COARSE";
            case BBRRegMap.CALIB_GOOD: return "GOOD";
            case BBRRegMap.CALIB_EXCELLENT: return "EXCELLENT";
            default: return "?" + grade;
        }
    }
}
