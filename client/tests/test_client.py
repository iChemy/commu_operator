import json
import unittest
from unittest.mock import patch

from commu_operator_client.client import CommUClient
from commu_operator_client.errors import CommUConnectionError, CommUServerError
from commu_operator_client.models import Action, Command, ServoName
from commu_operator_client.protocol import build_get_pose_payload, build_payload


def complete_pose_json() -> dict[str, int | float]:
    return {
        servo.value: 0 if index % 2 == 0 else 0.25
        for index, servo in enumerate(ServoName)
    }


class ChunkedSocket:
    def __init__(self, chunks: list[bytes]):
        self._chunks = list(chunks)
        self.sent = b""
        self.recv_calls = 0

    def __enter__(self) -> "ChunkedSocket":
        return self

    def __exit__(self, _exc_type, _exc_value, _traceback) -> None:
        return None

    def sendall(self, payload: bytes) -> None:
        self.sent += payload

    def recv(self, _size: int) -> bytes:
        self.recv_calls += 1
        if not self._chunks:
            return b""
        return self._chunks.pop(0)


class GetPoseClientTests(unittest.TestCase):
    def test_get_pose_reads_fragmented_response_until_eof(self) -> None:
        response = (json.dumps(complete_pose_json()) + "\n").encode("utf-8")
        chunks = [response[:7], response[7:31], response[31:-1], response[-1:], b""]
        fake_socket = ChunkedSocket(chunks)

        with patch(
            "commu_operator_client.client.socket.create_connection",
            return_value=fake_socket,
        ) as create_connection:
            pose = CommUClient("commu.local", 5000, timeout=2.5).get_pose()

        create_connection.assert_called_once_with(("commu.local", 5000), timeout=2.5)
        self.assertEqual(fake_socket.sent, build_get_pose_payload())
        self.assertEqual(fake_socket.recv_calls, len(chunks))
        self.assertEqual(set(pose), set(ServoName))
        self.assertEqual(
            pose,
            {ServoName(name): float(value) for name, value in complete_pose_json().items()},
        )
        self.assertTrue(all(type(value) is float for value in pose.values()))

    def test_get_pose_rejects_server_error(self) -> None:
        with self.assertRaises(CommUServerError):
            self._get_pose_from_chunks([b"ERR pose read failed\n", b""])

    def test_get_pose_reports_unsupported_legacy_backend(self) -> None:
        with self.assertRaisesRegex(CommUServerError, "does not support get_pose"):
            self._get_pose_from_chunks([b"OK\n", b""])

    def test_get_pose_rejects_invalid_json(self) -> None:
        with self.assertRaises(CommUServerError):
            self._get_pose_from_chunks([b'{"BODY_P":', b"not-json}\n", b""])

    def test_get_pose_rejects_non_object_json(self) -> None:
        with self.assertRaises(CommUServerError):
            self._get_pose_from_chunks([b"[]\n", b""])

    def test_get_pose_rejects_unknown_servo(self) -> None:
        response = complete_pose_json()
        response["MOUTH"] = 0

        with self.assertRaises(CommUServerError):
            self._get_pose_from_json(response)

    def test_get_pose_rejects_missing_servo(self) -> None:
        response = complete_pose_json()
        del response[ServoName.EYELIDS.value]

        with self.assertRaises(CommUServerError):
            self._get_pose_from_json(response)

    def test_get_pose_rejects_non_numeric_angle(self) -> None:
        response = complete_pose_json()
        response[ServoName.HEAD_Y.value] = "left"  # type: ignore[assignment]

        with self.assertRaises(CommUServerError):
            self._get_pose_from_json(response)

    def test_get_pose_rejects_non_finite_and_boolean_angles(self) -> None:
        for invalid_value in (float("nan"), float("inf"), 10**400, True):
            with self.subTest(invalid_value=invalid_value):
                response = complete_pose_json()
                response[ServoName.HEAD_Y.value] = invalid_value

                with self.assertRaises(CommUServerError):
                    self._get_pose_from_json(response)

    def test_get_pose_wraps_connection_errors(self) -> None:
        with patch(
            "commu_operator_client.client.socket.create_connection",
            side_effect=OSError("offline"),
        ):
            with self.assertRaises(CommUConnectionError):
                CommUClient("commu.local", 5000).get_pose()

    def _get_pose_from_json(self, response: dict[str, object]) -> dict[ServoName, float]:
        data = (json.dumps(response) + "\n").encode("utf-8")
        return self._get_pose_from_chunks([data, b""])

    def _get_pose_from_chunks(self, chunks: list[bytes]) -> dict[ServoName, float]:
        fake_socket = ChunkedSocket(chunks)
        with patch(
            "commu_operator_client.client.socket.create_connection",
            return_value=fake_socket,
        ):
            return CommUClient("commu.local", 5000).get_pose()


class ExistingSendRegressionTests(unittest.TestCase):
    def test_send_keeps_existing_payload_and_accepts_fragmented_ok(self) -> None:
        command = Command(
            actions=[
                Action(
                    type="pose",
                    duration_ms=800,
                    pose={ServoName.HEAD_Y: 12.5},
                )
            ]
        )
        chunks = [b"O", b"K", b"\n", b""]
        fake_socket = ChunkedSocket(chunks)

        with patch(
            "commu_operator_client.client.socket.create_connection",
            return_value=fake_socket,
        ):
            CommUClient("commu.local", 5000).send(command)

        self.assertEqual(fake_socket.sent, build_payload(command))
        self.assertEqual(fake_socket.recv_calls, len(chunks))


if __name__ == "__main__":
    unittest.main()
