import argparse
from pathlib import Path
from typing import List, Optional

from .errors import InvalidCommandError
from .models import Action, Command, CommandBatch, LEDSettings, ServoName


def load_command_from_args(args: argparse.Namespace) -> Command:
    direct_actions = build_direct_actions(args)
    if args.command is not None and direct_actions:
        raise InvalidCommandError("Use either --command or direct action arguments, not both.")
    if args.command is not None:
        return Command.model_validate_json(args.command.read_text(encoding="utf-8"))
    return Command(actions=direct_actions)


def build_direct_actions(args: argparse.Namespace) -> List[Action]:
    actions = []

    if args.pose:
        pose = {}
        for item in args.pose:
            name, angle = parse_pose_argument(item)
            pose[name] = angle
        actions.append(Action(type="pose", duration_ms=args.duration_ms, pose=pose))

    led = LEDSettings(
        body=args.led_body,
        power_button=args.led_power_button,
        left_cheek=args.led_left_cheek,
        right_cheek=args.led_right_cheek,
    )
    if not led.is_empty():
        actions.append(Action(type="led", duration_ms=args.duration_ms, led=led))

    if args.wait_ms is not None:
        actions.append(Action(type="wait", duration_ms=args.wait_ms))

    return actions


def load_batch(path: Path) -> CommandBatch:
    return CommandBatch.model_validate_json(path.read_text(encoding="utf-8"))


def resolve_audio_path(batch_path: Path, audio: Optional[str]) -> Optional[Path]:
    if audio is None:
        return None

    path = Path(audio)
    if path.is_absolute():
        return path
    return batch_path.parent / path


def parse_pose_argument(value: str) -> tuple[ServoName, float]:
    if "=" not in value:
        raise InvalidCommandError(f"invalid --pose value: {value}")

    name, angle = value.split("=", 1)
    try:
        return ServoName(name.strip().upper()), float(angle)
    except ValueError as error:
        raise InvalidCommandError(f"invalid --pose value: {value}") from error
