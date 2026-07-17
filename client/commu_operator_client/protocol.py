import struct
from pathlib import Path
from typing import Dict, List

from .audio import load_audio
from .models import Command, LEDSettings, ServoName


def build_payload(command: Command, base_path: Path | None = None) -> bytes:
    metadata, audio_payload = build_metadata_and_audio(command, base_path)
    return struct.pack(">I", len(metadata)) + metadata + audio_payload


def build_metadata(command: Command, base_path: Path | None = None) -> bytes:
    metadata, _audio_payload = build_metadata_and_audio(command, base_path)
    return metadata


def audio_payload_length(command: Command, base_path: Path | None = None) -> int:
    _metadata, audio_payload = build_metadata_and_audio(command, base_path)
    return len(audio_payload)


def build_metadata_and_audio(command: Command, base_path: Path | None = None) -> tuple[bytes, bytes]:
    lines = [
        f"actions={len(command.actions)}",
    ]
    audio_chunks = []

    for index, action in enumerate(command.actions):
        prefix = f"action.{index}."
        lines.append(f"{prefix}type={action.type}")

        if action.type == "audio":
            audio_bytes = load_audio(resolve_audio_path(action.audio, base_path))
            audio_chunks.append(audio_bytes)
            lines.append(f"{prefix}audio.length={len(audio_bytes)}")
        elif action.type == "pose":
            lines.append(f"{prefix}duration_ms={action.duration_ms}")
            if action.pose:
                lines.append(f"{prefix}pose={format_pose(action.pose)}")
            if action.led is not None and not action.led.is_empty():
                append_led_metadata(lines, prefix, action.led)
        elif action.type == "wait":
            lines.append(f"{prefix}duration_ms={action.duration_ms}")

    return ("\n".join(lines) + "\n").encode("utf-8"), b"".join(audio_chunks)


def resolve_audio_path(audio: str | None, base_path: Path | None) -> Path:
    if audio is None:
        raise ValueError("audio action requires audio")

    path = Path(audio)
    if path.is_absolute() or base_path is None:
        return path
    return base_path / path


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
