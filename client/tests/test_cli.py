import io
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path
from unittest.mock import patch

from commu_operator_client.cli import run_cli
from commu_operator_client.models import Command, ServoName


def complete_pose() -> dict[ServoName, float]:
    return {servo: index / 10.0 for index, servo in enumerate(ServoName)}


class GetPoseCliTests(unittest.TestCase):
    def test_get_pose_prints_replayable_command_json(self) -> None:
        pose = complete_pose()
        stdout = io.StringIO()

        with (
            patch("commu_operator_client.cli.CommUClient") as client_class,
            patch(
                "sys.argv",
                [
                    "main.py",
                    "get-pose",
                    "--host",
                    "commu.local",
                    "--port",
                    "5123",
                    "--timeout",
                    "2.5",
                    "--duration-ms",
                    "750",
                ],
            ),
            redirect_stdout(stdout),
        ):
            client_class.return_value.get_pose.return_value = pose
            run_cli()

        client_class.assert_called_once_with("commu.local", 5123, 2.5)
        client_class.return_value.get_pose.assert_called_once_with()

        command = Command.model_validate_json(stdout.getvalue())
        self.assertEqual(len(command.actions), 1)
        action = command.actions[0]
        self.assertEqual(action.type, "pose")
        self.assertEqual(action.duration_ms, 750)
        self.assertEqual(action.pose, pose)
        self.assertIsNone(action.led)

    def test_get_pose_writes_replayable_utf8_file(self) -> None:
        pose = complete_pose()
        with tempfile.TemporaryDirectory() as directory:
            output_path = Path(directory) / "captured_pose.json"
            with (
                patch("commu_operator_client.cli.CommUClient") as client_class,
                patch(
                    "sys.argv",
                    ["main.py", "get-pose", "--output", str(output_path)],
                ),
            ):
                client_class.return_value.get_pose.return_value = pose
                run_cli()

            command = Command.model_validate_json(output_path.read_text(encoding="utf-8"))

        self.assertEqual(command.actions[0].pose, pose)
        self.assertEqual(command.actions[0].duration_ms, 1000)


if __name__ == "__main__":
    unittest.main()
