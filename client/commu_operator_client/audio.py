import io
from pathlib import Path
from typing import Optional

from pydub import AudioSegment

from .config import TARGET_AUDIO_CHANNELS, TARGET_AUDIO_FRAME_RATE, TARGET_AUDIO_SAMPLE_WIDTH
from .errors import AudioConversionError


def load_audio(path: Optional[Path]) -> bytes:
    if path is None:
        return b""
    return convert_audio_to_wav(path)


def convert_audio_to_wav(path: Path) -> bytes:
    try:
        audio = AudioSegment.from_file(path)
    except Exception as error:
        raise AudioConversionError(
            f"failed to read audio file {path}. MP3 input requires ffmpeg on the client machine."
        ) from error

    audio = (
        audio.set_channels(TARGET_AUDIO_CHANNELS)
        .set_frame_rate(TARGET_AUDIO_FRAME_RATE)
        .set_sample_width(TARGET_AUDIO_SAMPLE_WIDTH)
    )

    output = io.BytesIO()
    audio.export(output, format="wav")
    return output.getvalue()
