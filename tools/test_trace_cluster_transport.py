import unittest

from trace_cluster_transport import catalog, derive


class ClusterTransportTest(unittest.TestCase):
    def test_tachometer_matches_independently_saved_native_trace(self):
        frame = derive(0x5C, 1, 0x50, 0x54)
        self.assertEqual(frame['oem_syscon_ipc'],
                         '02 00 15 FF EA 51 07 44 00 00 00 00 03 '
                         '16 30 50 54 40 2E 00 00 00 00 00 00 03')
        self.assertEqual(derive(0x5C, 2, 0x50, 0x55)['oem_payload'], '40 2E 01')

    def test_panel_variants_use_oem_value_translation(self):
        self.assertEqual([derive(0x5B, v, 0x50, 0x54)['oem_payload']
                          for v in (1, 2, 3)], ['40 15 00', '40 15 01', '40 15 02'])
        self.assertEqual(derive(0x2A, 1, 0x50, 0x54)['oem_payload'], '40 33 07')
        self.assertEqual(derive(0x2A, 2, 0x50, 0x54)['oem_payload'], '40 33 01')

    def test_no_missing_route_or_shared_state_is_filled_in(self):
        for args in [(0x41, 1, 0x50, 0x54), (0x5B, 1, 0x51, 0x54),
                     (0x01, 1, 0x50, 0x54), (0x5B, 4, 0x50, 0x54),
                     (0x5B, 1, 0x50, 0)]:
            with self.subTest(args=args), self.assertRaises(ValueError):
                derive(*args)

    def test_catalog_never_claims_an_xp_packet_or_hardware_result(self):
        features = catalog()['features']
        self.assertEqual(6, len(features))
        for feature in features:
            for case in feature['cases']:
                self.assertIsNone(case['xp_passthrough_packet'])
                self.assertFalse(case['hardware_verified'])


if __name__ == '__main__':
    unittest.main()
