from dataclasses import dataclass
from typing import Tuple

from .models import ServoName


@dataclass(frozen=True)
class ServoSpec:
    name: ServoName
    min_degrees: float
    max_degrees: float
    reduction_ratio: float
    aliases: Tuple[str, ...] = ()

    @property
    def range_text(self) -> str:
        return f"{format_degrees(self.min_degrees)}..{format_degrees(self.max_degrees)}"


SERVO_SPECS = (
    ServoSpec(ServoName.BODY_P, -15, 15, 3.833),
    ServoSpec(ServoName.BODY_Y, -67, 67, 1.0),
    ServoSpec(ServoName.L_SHOULDER_P, -108, 108, 1.364, ("L_SHOU_P",)),
    ServoSpec(ServoName.L_SHOULDER_R, -45, 30, 1.0, ("L_SHOU_R",)),
    ServoSpec(ServoName.R_SHOULDER_P, -108, 108, 1.364, ("R_SHOU_P",)),
    ServoSpec(ServoName.R_SHOULDER_R, -30, 45, 1.0, ("R_SHOU_R",)),
    ServoSpec(ServoName.HEAD_P, -20, 25, 1.0),
    ServoSpec(ServoName.HEAD_R, -15, 15, 4.333),
    ServoSpec(ServoName.HEAD_Y, -85, 85, 1.0),
    ServoSpec(ServoName.EYE_P, -22, 22, 1.0, ("EYES_P",)),
    ServoSpec(ServoName.L_EYE_Y, -35, 20, 1.0),
    ServoSpec(ServoName.R_EYE_Y, -20, 35, 1.0),
    ServoSpec(ServoName.EYELIDS, -65, 3, 1.0, ("EYELID",)),
)


def format_degrees(value: float) -> str:
    if float(value).is_integer():
        return str(int(value))
    return str(value)
