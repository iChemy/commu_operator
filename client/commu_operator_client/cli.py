import argparse
from pathlib import Path

from pydantic import ValidationError

from .client import CommUClient
from .commands import load_command_from_args
from .config import DEFAULT_HOST, DEFAULT_PORT, DEFAULT_TIMEOUT
from .errors import CommUClientError, InvalidCommandError
from .models import Command
from .protocol import audio_payload_length
from .servos import SERVO_SPECS


def main() -> None:
    try:
        run_cli()
    except CommUClientError as error:
        raise SystemExit(f"error: {error}") from error


def run_cli() -> None:
    parser = build_parser()
    args = parser.parse_args()

    try:
        args.func(args)
    except ValidationError as error:
        raise InvalidCommandError(error) from error
    except OSError as error:
        raise CommUClientError(error) from error


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Send generic operation commands to CommU.")
    subparsers = parser.add_subparsers(dest="command_name", required=True)

    send_parser = subparsers.add_parser("send", help="send one command")
    add_connection_arguments(send_parser)
    send_parser.add_argument("--audio", type=Path, help="audio file to play on CommU")
    send_parser.add_argument("--command", type=Path, help="command JSON file")
    send_parser.add_argument(
        "--pose",
        action="append",
        default=[],
        metavar="SERVO=ANGLE",
        help="servo target in degrees, e.g. HEAD_Y=20; can be repeated",
    )
    send_parser.add_argument(
        "--duration-ms",
        type=int,
        default=1000,
        help="duration for direct pose action in milliseconds",
    )
    send_parser.add_argument("--wait-ms", type=int, help="append a wait action in milliseconds")
    add_led_arguments(send_parser)
    send_parser.add_argument("--dry-run", action="store_true")
    send_parser.set_defaults(func=send_command)

    list_parser = subparsers.add_parser("list-servos", help="show supported servo names")
    list_parser.set_defaults(func=list_servos)
    return parser


def add_connection_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--host", default=DEFAULT_HOST)
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--timeout", type=float, default=DEFAULT_TIMEOUT)


def add_led_arguments(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--led-body", help="body LED color as #RRGGBB")
    parser.add_argument("--led-power-button", help="power button LED color as #RRGGBB")
    parser.add_argument("--led-left-cheek", type=int, help="left cheek brightness, 0-255")
    parser.add_argument("--led-right-cheek", type=int, help="right cheek brightness, 0-255")


def list_servos(_args: argparse.Namespace) -> None:
    print("name            range_deg    ratio")
    print("--------------  -----------  -------")
    for spec in SERVO_SPECS:
        print(
            f"{spec.name.value:<14}  "
            f"{spec.range_text:<11}  "
            f"{spec.reduction_ratio:<7g}"
        )
    print()
    print("Angles in command JSON and --pose are degrees. Values outside range are clamped by backend.")


def send_command(args: argparse.Namespace) -> None:
    command = load_command_from_args(args)
    base_path = args.command.parent if args.command is not None else Path.cwd()
    client = CommUClient(args.host, args.port, args.timeout)

    if args.dry_run:
        print_metadata(client, command, base_path)
        return

    client.send(command, base_path)
    print("OK")


def print_metadata(client: CommUClient, command: Command, base_path: Path) -> None:
    metadata = client.build_metadata(command, base_path)
    print(metadata.decode("utf-8"), end="")
    print(f"# audio.bytes={audio_payload_length(command, base_path)}")
