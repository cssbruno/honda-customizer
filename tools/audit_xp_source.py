"""Audit exact XP 0x4012a branches offline; external vendor source is not app code."""
import argparse
import hashlib
import json
import re
from pathlib import Path
from audit_bnr_source import evaluate


def audit(source_path, contracts_path, inventory_path):
    source = source_path.read_text()
    contracts = json.loads(contracts_path.read_text())
    inventory = json.loads(inventory_path.read_text())
    _, visibility, _ = evaluate(source, 'onResume', 0x4012a)
    visibility = {0x7f0b00b8: 8, 0x7f0b0121: 8, **visibility}
    onclick = re.search(r'\.method public onClick.*?\.end method', source, re.S)[0]
    clickable = {int(x, 0) for x in re.findall(r'(0x[0-9a-f]+) -> :sswitch_', onclick)}
    visible = {}
    for entry in inventory:
        fields = [x['field'] for x in entry['feedback']]
        ids = [int(x[0], 0) for x in entry['ids']]
        if len(fields) == 1 and not any(visibility.get(i, 0) for i in ids):
            visible[fields[0]] = sorted(clickable.intersection(ids))
    assert set(visible) == {c['field'] for c in contracts}, 'Visible scalar rows differ'
    rows = []
    for contract in contracts:
        count, key = len(contract['values']), contract['key']
        paths, outputs = [], set()
        assert visible[contract['field']], 'No source writer'
        for view in visible[contract['field']]:
            cases = []
            for current in range(count):
                _, _, writes = evaluate(source, 'onClick', 0x4012a, current, view)
                assert len(writes) == 1
                command, actual_key, value = writes[0]
                assert command == 105 and actual_key == key and 0 <= value < count
                outputs.add(value)
                cases.append({'current': current, 'write': writes[0]})
            paths.append({'view': hex(view), 'cases': cases})
        assert outputs == set(range(count)), 'Writer does not cover every advertised option'
        rows.append({'field': contract['field'], 'paths': paths})
    return {'profile': '0x4012a', 'source_sha256': hashlib.sha256(source_path.read_bytes()).hexdigest(),
            'visibility': {hex(k): v for k, v in visibility.items()}, 'rows': rows}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = audit(args.source, Path('documents/research/xp-4012a-contracts.json'),
                   Path('documents/research/honda-cluster-analysis/fyt-additional/doors-lights/siyu-inventory.json'))
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print('Verified', len(result['rows']), 'XP rows and every legal writer value.')
