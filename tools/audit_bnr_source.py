"""Offline evidence check of extracted BNR activity branches; never contacts Android.

This small interpreter accepts only the instructions needed by the inspected
onResume and onClick paths. Unknown instructions abort instead of being guessed.
Its inputs are analysis cases, not app data. Vendor source remains external.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path


def method(source, name):
    match = re.search(r'\.method[^\n]* ' + re.escape(name) + r'\([^\n]*\n(.*?)\.end method', source, re.S)
    if not match:
        raise ValueError('Missing source method: ' + name)
    return [line.strip() for line in match[1].splitlines()
            if line.strip() and not line.strip().startswith(('.', '#'))]


def evaluate(source, name, profile, current=0, view=0):
    lines = method(source, name)
    labels = {line: i for i, line in enumerate(lines) if line.startswith(':')}
    registers = {'p0': 'activity', 'p1': view}
    visibility, writes = {}, []
    result = None
    pc = 0
    for _ in range(3000):
        line = lines[pc]
        pc += 1
        if line.startswith(':') or line == 'nop':
            continue
        op, _, args = line.partition(' ')
        if op.startswith('const') and not op.startswith(('const-string', 'const-class')):
            register, number = args.split(', ')
            registers[register] = int(number, 0)
        elif op in ('move-result', 'move-result-object'):
            registers[args] = result
        elif op.startswith('move'):
            target, origin = args.split(', ')
            registers[target] = registers[origin]
        elif op == 'sget-object' and 'DataCanbus;->DATA:' in args:
            registers[args.split(',')[0]] = 'canbus-data'
        elif op == 'sget' and 'DataCanbus;->sCanbusId:I' in args:
            registers[args.split(',')[0]] = profile
        elif op == 'aget':
            target, array, index = args.split(', ')
            assert registers[array] == 'canbus-data'
            registers[target] = profile if registers[index] == 1000 else current
        elif op in ('add-int/lit8', 'rem-int/lit8'):
            target, origin, literal = args.split(', ')
            value, literal = registers[origin], int(literal, 0)
            registers[target] = value + literal if op.startswith('add') else value - int(value / literal) * literal
        elif op.startswith('goto'):
            pc = labels[args]
        elif op.startswith('if-'):
            parts = args.split(', ')
            left = registers[parts[0]]
            right = 0 if len(parts) == 2 else registers[parts[1]]
            condition = op[3:].removesuffix('z')
            yes = {'eq': left == right, 'ne': left != right, 'ge': left >= right,
                   'gt': left > right, 'le': left <= right, 'lt': left < right}[condition]
            if yes:
                pc = labels[parts[-1]]
        elif op == 'sparse-switch':
            register, label = args.split(', ')
            body = re.search(r'\.method[^\n]* ' + re.escape(name) + r'\([^\n]*\n(.*?)\.end method', source, re.S)[1]
            table = re.search(re.escape(label) + r'\s+\.sparse-switch(.*?)\.end sparse-switch', body, re.S)[1]
            choices = {int(value, 0): target for value, target in re.findall(r'(0x[0-9a-f]+) -> (:\w+)', table)}
            if registers[register] in choices:
                pc = labels[choices[registers[register]]]
        elif op.startswith('invoke-'):
            register_list, call = re.match(r'\{(.*?)\}, (.*)', args).groups()
            values = [registers[r] for r in register_list.split(', ')]
            if '->getId()' in call:
                result = view
            elif '->findViewById(I)' in call:
                result = values[1]
            elif '->isGuanDao()' in call or '->isBNRSiYuOrGuanDao()' in call:
                target = call.split('->')[1].split('(')[0]
                result, _, _ = evaluate(source, target, profile)
            elif '->setViewState(' in call:
                visibility[values[1]] = values[2]
            elif '->setViewVisible(' in call:
                visibility[values[1]] = 0 if values[2] else 8
            elif '->setCarInfo(II)' in call:
                writes.append([105, values[1], values[2]])
            elif '->onResume()' in call or '->addNotify()' in call:
                pass
            else:
                raise ValueError('Unsupported call: ' + call)
        elif op == 'check-cast':
            pass
        elif op.startswith('return'):
            return (registers.get(args) if args else None), visibility, writes
        else:
            raise ValueError('Unsupported instruction: ' + line)
    raise ValueError('Source path did not finish')


def audit(source_path, inventory_path):
    source = source_path.read_text()
    inventory = json.loads(inventory_path.read_text())
    profiles = [0x6012a, 0x7012a, 0x8012a, 0x9012a, 0xa012a, 0xb012a, 0xf012a, 0x28012a]
    onclick = re.search(r'\.method public onClick.*?\.end method', source, re.S)[0]
    clickable = {int(x, 0) for x in re.findall(r'(0x[0-9a-f]+) -> :sswitch_', onclick)}
    output = {'source': str(source_path), 'sha256': hashlib.sha256(source_path.read_bytes()).hexdigest(), 'profiles': {}}
    for profile in profiles:
        _, visibility, _ = evaluate(source, 'onResume', profile)
        # Actual layout defaults: key/remote-unlock and beep-volume rows are GONE.
        layout_visibility = {0x7f0b00b8: 8, 0x7f0b0121: 8}
        layout_visibility.update(visibility)
        rows = []
        for entry in inventory:
            fields = [x['field'] for x in entry['feedback']]
            if len(fields) != 1:
                continue
            ids = [int(x[0], 0) for x in entry['ids']]
            hidden = any(layout_visibility.get(x, 0) != 0 for x in ids)
            targets = sorted(clickable.intersection(ids))
            paths = []
            for target in targets:
                cases = [evaluate(source, 'onClick', profile, current, target)[2] for current in range(3)]
                paths.append({'view': hex(target), 'writes_for_current_0_1_2': cases})
            rows.append({'field': fields[0], 'label': entry['label'], 'visible': not hidden, 'paths': paths})
        output['profiles'][hex(profile)] = rows
    return output


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('--inventory', type=Path, default=Path('documents/research/honda-cluster-analysis/fyt-additional/doors-lights/siyu-inventory.json'))
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    output = audit(args.source, args.inventory)
    args.output.write_text(json.dumps(output, indent=2) + '\n')
    for profile, rows in output['profiles'].items():
        print(profile, 'visible fields:', ','.join(str(r['field']) for r in rows if r['visible']))
