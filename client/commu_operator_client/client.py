import socket
from pathlib import Path

from .config import DEFAULT_TIMEOUT
from .errors import CommUConnectionError, CommUServerError
from .models import Command
from .protocol import build_metadata, build_payload


class CommUClient:
    def __init__(self, host: str, port: int, timeout: float = DEFAULT_TIMEOUT):
        self.host = host
        self.port = port
        self.timeout = timeout

    def send(self, command: Command, base_path: Path | None = None) -> None:
        payload = build_payload(command, base_path)
        try:
            with socket.create_connection((self.host, self.port), timeout=self.timeout) as sock:
                sock.sendall(payload)
                response = sock.recv(4096).decode("utf-8", errors="replace").strip()
        except OSError as error:
            raise CommUConnectionError(f"failed to connect to {self.host}:{self.port}: {error}") from error

        if response != "OK":
            raise CommUServerError(response or "empty response from server")

    def build_metadata(self, command: Command, base_path: Path | None = None) -> bytes:
        return build_metadata(command, base_path)
