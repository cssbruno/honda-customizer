# Direct transport inspection

The 2026-09-16 access check successfully ran `adb devices -l`, but returned no devices. No head-unit interfaces or permissions have been inspected yet.

The read-only collector prints its report to the terminal:

```sh
python3 tools/inspect_direct_transport.py --adb /home/bruno/Android/Sdk/platform-tools/adb
```

Use `--serial SERIAL` when multiple devices are connected. The tool does not connect to guessed network addresses or select an unauthorized device. Use `--print-script` to review the exact device-side shell script without invoking ADB.

It reads:

- Basic platform identity, inspection UID, and SELinux enforcement status.
- Network interface type, state, and device/driver symlinks. Type 280 identifies a CAN-type interface; virtual interfaces must not be treated as physical controllers.
- Candidate serial/CAN/MCU device node metadata, including access modes and SELinux labels.
- Kernel TTY driver inventory.
- Visible `/dev/` file descriptor targets of the exact `com.syu.ms` and `com.syu.canbus` processes.

It does not open the listed devices, transmit frames, acquire root, change permissions, stop services, or write files on the head unit. Access errors remain visible. ADB shell privileges do not establish APK privileges. Serial node names and open descriptors alone do not establish physical wiring, baud rate, or a decoder command contract.

## Decision after collection

1. If a physical CAN interface is present, verify its driver, hardware route, and application access before implementing a matching transport.
2. If only serial/MCU endpoints are present, correlate their paths with the installed SYU service configuration. Opening that endpoint would still require its packet protocol and coordinated ownership; bypassing Binder alone does not bypass the decoder.
3. If metadata is denied, report the limitation. Do not interpret denied access as absent hardware or disable SELinux to make the inspection succeed.

Local validation: CLI help and generated shell syntax checked. Physical-device behavior remains untested because no central is connected.
