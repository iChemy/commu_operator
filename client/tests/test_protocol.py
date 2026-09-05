import struct
import unittest

from commu_operator_client.protocol import build_get_pose_payload


class GetPoseProtocolTests(unittest.TestCase):
    def test_build_get_pose_payload_uses_length_prefixed_metadata(self) -> None:
        metadata = b"request=get_pose\n"

        payload = build_get_pose_payload()

        self.assertEqual(payload[:4], struct.pack(">I", len(metadata)))
        self.assertEqual(payload[4:], metadata)


if __name__ == "__main__":
    unittest.main()
