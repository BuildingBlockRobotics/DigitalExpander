# BBR Digital Expander — FTC Java driver

The FTC SDK driver and example OpModes for the **BBR Digital Expander**: an
expansion board for the REV Control Hub and Expansion Hub that adds four
quadrature encoder inputs, four colour/distance sensor ports, four digital
outputs and an onboard IMU, all through a single I2C port.

**Documentation: <https://expander.buildingblockrobotics.com/>**

The board can watch a condition for you — a taught colour, a distance, an
encoder threshold, a heading — and drive a digital output when it is met, with
no I2C traffic and no code in your loop. Your match OpMode reads a stock
`DigitalChannel`.

## Install

Copy both folders into your FTC project's TeamCode source root
(`TeamCode/src/main/java/`):

- `com/buildingblockrobotics/expander/` — the driver (`BBRDigitalExpander`)
  and the register map constants (`BBRRegMap`, a generated file — do not
  edit it).
- `org/firstinspires/ftc/teamcode/` — example OpModes; copy the ones you want.

Then add the device in the robot configuration: on the I2C bus it is wired to,
add **"BBR Digital Expander"** (xmlTag `BBRDigitalExpander`) and give it a name
(the examples use `expander`).

```java
BBRDigitalExpander exp = hardwareMap.get(BBRDigitalExpander.class, "expander");

int counts     = exp.getEncoderCount(0);
boolean isRed  = exp.seesColor(1, 1);
double mm      = exp.getDistanceMm(2);
double heading = exp.getHeading();
```

## Examples

Start with `BBRColorClassExample` if you are new to the board, and read
`BBRDistanceTriggerSetup` and `BBRDistanceTriggerRuntime` together — the pair
shows the whole point of the product.

| OpMode | Shows |
|---|---|
| `BBRColorClassExample` | Reading classes/confidence — the primary interface |
| `BBRTeachColorExample` | Teach-by-example calibration + saving to flash |
| `BBRThreeColorExample` | The three-colour season pattern, one pin per colour |
| `BBRColorTriggerSetup` | Setting up a colour-driven digital output |
| `BBRColorLatchExample` | Latched outputs — catching an event too brief for your loop |
| `BBRDistanceTriggerSetup` | The canonical distance-to-digital-output setup, run once |
| `BBRDistanceTriggerRuntime` | The runtime half: a stock `DigitalChannel`, zero I2C |
| `BBREncoderExample` | Counts + firmware-computed velocities, idempotent reset |
| `BBREncoderTriggerExample` | Driving an output from an encoder threshold |
| `BBRPwmEncoderExample` | Absolute pulse-width encoders, wrap tracking |
| `BBRImuHeadingExample` | Heading; fail-loud when the gyro cannot be trusted |
| `BBRHeadingTriggerExample` | A heading-driven output, wrap-safe at ±180 |
| `BBRLocalizerExample` | Full pose tracking: params, calibration, live pose, re-zeroing |
| `BBRSensorDumpTest` | One-transaction dump of every live value — bring-up diagnostic |
| `BBRLineProbeTest` | Bench-testing a sensor against a line |

## Two tiers

The **everyday tier** is one call per question — no bitmasks, no register
numbers, and the tuning values that matter are preset to ones that work. The
**advanced tier** underneath exposes the full register map for teams that
outgrow the helpers. You can mix them.

Setup helpers (`teachColor`, `triggerOnColor`, `triggerWhenNear`,
`triggerWhenEncoderPast`, `triggerWhenFacing`) save to flash automatically, so
run them **once** in a setup OpMode — never in your match loop. The advanced
tier never auto-saves; call `saveConfigToFlash()` yourself.

## Driver behaviour teams should know

- **Init refuses loudly** on a wrong `DEVICE_ID`, an unsupported
  `PROTOCOL_MAJOR`, or an unrecognised `HW_VARIANT` — a clear exception at init
  beats corrupt data mid-match.
- **Commands are idempotent**: every command carries a token, so an I2C
  transport retry can never execute one twice (`resetEncoders` cannot
  double-zero).
- **`readTelemetry()` is one bus transaction** (96 bytes) and the block is
  snapshotted by the firmware — values in one `TelemetryBlock` are mutually
  consistent by construction.
- **IMU access throws** (never returns zeros) on a missing or faulty
  IMU, with a message saying which. A heading of 0.00 that never changes
  would look exactly like working software.
- **`BBRTransportException` means the read never reached the device**, not
  that anything is broken. The SDK tears the I2C device down when an OpMode
  stops, so a read already in flight comes back as all zeros (or all `0xFF`
  on a released bus). It is a subclass of `BBRException`, so existing
  `catch` blocks still work — but a polling loop should catch it and
  `break`, as the IMU and localizer examples do. Without this the shutdown
  looked like CRC corruption on the localizer and like a dead IMU
  ("request a replacement") on the IMU path, both on a perfectly good board.
- **Config writes are verified by readback** — the bus layer never NAKs, so
  an out-of-range value would otherwise be silently ignored.
- After calibrating (`captureAndStore`, `calibrateGyro`) call
  `saveConfigToFlash()` or the calibration is lost at power-off;
  `isConfigDirty()` tells you.

## Licence

MIT — see [LICENSE](LICENSE). Copy it into your TeamCode, change it, ship it.
