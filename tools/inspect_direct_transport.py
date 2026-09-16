"""Print a read-only ADB transport inventory; never open a serial/CAN device."""

import argparse
import subprocess
import sys


DEVICE_SCRIPT = r'''
section() { printf '\n## %s\n' "$1"; }
section 'Identity and inspection privileges'
id
getenforce
for prop in ro.product.manufacturer ro.product.model ro.board.platform ro.build.version.release; do
    printf '%s: ' "$prop"
    getprop "$prop"
done
section 'Network interface types (280 = CAN; type alone does not prove physical hardware)'
for iface in /sys/class/net/*; do
    [ -e "$iface" ] || continue
    printf '\n%s\n' "$iface"
    for attr in type operstate; do
        printf '%s: ' "$attr"
        cat "$iface/$attr" 2>&1
    done
    printf 'device: '
    readlink "$iface/device" 2>&1
    printf 'driver: '
    readlink "$iface/device/driver" 2>&1
done
section 'Candidate device metadata only (no device opened)'
found=0
for node in /dev/ttyS* /dev/ttyUSB* /dev/ttyACM* /dev/ttyMbx* /dev/*serial* /dev/*can* /dev/*CAN* /dev/*mcu* /dev/*MCU*; do
    [ -e "$node" ] || continue
    found=1
    ls -ldZ "$node" 2>&1
done
[ "$found" = 1 ] || printf 'No matching nodes visible to this ADB identity.\n'
section 'Kernel TTY driver inventory'
cat /proc/tty/drivers 2>&1
section 'SYU open device descriptors (visibility may be denied)'
for service in com.syu.ms com.syu.canbus; do
    printf '\n%s\n' "$service"
    pids=$(pidof "$service" 2>/dev/null)
    if [ -z "$pids" ]; then
        printf 'No PID visible for this exact process name.\n'
        continue
    fi
    for syu_pid in $pids; do
        printf 'PID %s\n' "$syu_pid"
        if ! ls "/proc/$syu_pid/fd" >/dev/null 2>&1; then
            printf 'Descriptor metadata unavailable to this ADB identity.\n'
            continue
        fi
        for fd in /proc/"$syu_pid"/fd/*; do
            target=$(readlink "$fd" 2>/dev/null)
            case "$target" in
                /dev/*) printf '%s -> %s\n' "$fd" "$target" ;;
            esac
        done
    done
done
section 'Limits'
printf '%s\n' 'ADB permissions are not application permissions.' \
 'Missing or denied metadata does not prove absence of hardware.' \
 'A serial node does not identify its wiring or establish an XP raw-CAN packet format.' \
 'No serial device was opened, no CAN frame sent, no service stopped, no permissions changed.'
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb', help='ADB executable path')
    parser.add_argument('--serial', help='Exact authorized ADB device serial')
    parser.add_argument('--print-script', action='store_true', help='Review device script without running ADB')
    args = parser.parse_args()
    if args.print_script:
        print(DEVICE_SCRIPT)
        return 0
    try:
        inventory = subprocess.run([args.adb, 'devices'], check=True,
                                   capture_output=True, text=True, timeout=15)
        devices = [line.split() for line in inventory.stdout.splitlines()[1:] if line.strip()]
        ready = [row[0] for row in devices if len(row) >= 2 and row[1] == 'device']
        if args.serial:
            if args.serial not in ready:
                parser.exit(2, 'Selected device is not connected and authorized.\n')
            selected = args.serial
        elif len(ready) == 1:
            selected = ready[0]
        else:
            parser.exit(2, 'Connect one authorized head unit or specify --serial. No inspection performed.\n')
        result = subprocess.run([args.adb, '-s', selected, 'shell', 'sh'],
                                input=DEVICE_SCRIPT, text=True, timeout=45)
        return result.returncode
    except (OSError, subprocess.SubprocessError) as exc:
        print(f'Inspection could not complete: {exc}', file=sys.stderr)
        return 1


if __name__ == '__main__':
    sys.exit(main())
