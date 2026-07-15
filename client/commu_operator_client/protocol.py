import struct
from typing import Dict, List

from .models import Command, LEDSettings, ServoName


def build_payload(command: Command, audio_bytes: bytes = b"") -> bytes:
    metadata = build_metadata(command, len(audio_bytes))
    return struct.pack(">I", len(metadata)) + metadata + audio_bytes


def build_metadata(command: Command, audio_length: int = 0) -> bytes:
    lines = [
        f"audio.length={audio_length}",
        f"actions={len(command.actions)}",
    ]

    for index, action in enumerate(command.actions):
        prefix = f"action.{index}."
        lines.append(f"{prefix}type={action.type}")
        lines.append(f"{prefix}duration_ms={action.duration_ms}")

        if action.type == "pose":
            lines.append(f"{prefix}pose={format_pose(action.pose)}")
        elif action.type == "led" and action.led is not None:
            append_led_metadata(lines, prefix, action.led)

    return ("\n".join(lines) + "\n").encode("utf-8")


def append_led_metadata(lines: List[str], prefix: str, led: LEDSettings) -> None:
    if led.body is not None:
        lines.append(f"{prefix}led.body={escape_value(led.body)}")
    if led.power_button is not None:
        lines.append(f"{prefix}led.power_button={escape_value(led.power_button)}")
    if led.left_cheek is not None:
        lines.append(f"{prefix}led.left_cheek={led.left_cheek}")
    if led.right_cheek is not None:
        lines.append(f"{prefix}led.right_cheek={led.right_cheek}")


def format_pose(pose: Dict[ServoName, float]) -> str:
    return ",".join(f"{servo.value}={format_angle(angle)}" for servo, angle in pose.items())


def format_angle(angle: float) -> str:
    value = float(angle)
    if value.is_integer():
        return str(int(value))
    return str(value)


def escape_value(value: object) -> str:
    return str(value).replace("\\", "\\\\").replace("\n", "\\n")
