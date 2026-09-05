import json
import math
import socket
from pathlib import Path

from .config import DEFAULT_TIMEOUT
from .errors import CommUConnectionError, CommUServerError
from .models import Command, ServoName
from .protocol import build_get_pose_payload, build_metadata, build_payload


RESPONSE_CHUNK_SIZE = 4096
MAX_RESPONSE_BYTES = 64 * 1024


class CommUClient:
    def __init__(self, host: str, port: int, timeout: float = DEFAULT_TIMEOUT):
        self.host = host
        self.port = port
        self.timeout = timeout

    def send(self, command: Command, base_path: Path | None = None) -> None:
        payload = build_payload(command, base_path)
        response = self._exchange(payload)

        if response != "OK":
            raise CommUServerError(response or "empty response from server")

    def get_pose(self) -> dict[ServoName, float]:
        response = self._exchange(build_get_pose_payload())
        if response.startswith("ERR "):
            raise CommUServerError(response)
        if response == "OK":
            raise CommUServerError(
                "backend does not support get_pose; update and restart the backend"
            )

        try:
            decoded = json.loads(response)
        except (ValueError, TypeError, RecursionError) as error:
            raise CommUServerError("invalid get_pose response from server") from error

        if not isinstance(decoded, dict):
            raise CommUServerError("invalid get_pose response: expected a JSON object")

        expected_names = {servo.value for servo in ServoName}
        actual_names = set(decoded)
        if actual_names != expected_names:
            missing = sorted(expected_names - actual_names)
            unknown = sorted(actual_names - expected_names)
            details = []
            if missing:
                details.append("missing " + ", ".join(missing))
            if unknown:
                details.append("unknown " + ", ".join(unknown))
            raise CommUServerError("invalid get_pose response: " + "; ".join(details))

        pose = {}
        for name, value in decoded.items():
            if type(value) not in (int, float):
                raise CommUServerError("invalid angle for " + name)
            try:
                angle = float(value)
            except (ValueError, TypeError, OverflowError) as error:
                raise CommUServerError("invalid angle for " + name) from error
            if not math.isfinite(angle):
                raise CommUServerError("invalid angle for " + name)
            pose[ServoName(name)] = angle
        return pose

    def _exchange(self, payload: bytes) -> str:
        try:
            with socket.create_connection((self.host, self.port), timeout=self.timeout) as sock:
                sock.sendall(payload)
                chunks = []
                total_bytes = 0
                while True:
                    chunk = sock.recv(RESPONSE_CHUNK_SIZE)
                    if not chunk:
                        break
                    total_bytes += len(chunk)
                    if total_bytes > MAX_RESPONSE_BYTES:
                        raise CommUServerError("response from server is too large")
                    chunks.append(chunk)
        except OSError as error:
            raise CommUConnectionError(f"failed to connect to {self.host}:{self.port}: {error}") from error

        return b"".join(chunks).decode("utf-8", errors="replace").strip()

    def build_metadata(self, command: Command, base_path: Path | None = None) -> bytes:
        return build_metadata(command, base_path)
