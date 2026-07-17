from enum import Enum
from typing import Dict, List, Literal, Optional

from pydantic import BaseModel, Field, model_validator


class ServoName(str, Enum):
    BODY_P = "BODY_P"
    BODY_Y = "BODY_Y"
    L_SHOULDER_P = "L_SHOULDER_P"
    L_SHOULDER_R = "L_SHOULDER_R"
    R_SHOULDER_P = "R_SHOULDER_P"
    R_SHOULDER_R = "R_SHOULDER_R"
    HEAD_P = "HEAD_P"
    HEAD_R = "HEAD_R"
    HEAD_Y = "HEAD_Y"
    EYE_P = "EYE_P"
    L_EYE_Y = "L_EYE_Y"
    R_EYE_Y = "R_EYE_Y"
    EYELIDS = "EYELIDS"


class LEDSettings(BaseModel):
    body: Optional[str] = None
    power_button: Optional[str] = None
    left_cheek: Optional[int] = Field(default=None, ge=0, le=255)
    right_cheek: Optional[int] = Field(default=None, ge=0, le=255)

    def is_empty(self) -> bool:
        return (
            self.body is None
            and self.power_button is None
            and self.left_cheek is None
            and self.right_cheek is None
        )


class Action(BaseModel):
    type: Literal["audio", "pose", "wait"]
    duration_ms: int = Field(default=0, ge=0)
    audio: Optional[str] = None
    pose: Dict[ServoName, float] = Field(default_factory=dict)
    led: Optional[LEDSettings] = None

    @model_validator(mode="after")
    def validate_payload(self) -> "Action":
        if self.type == "audio":
            if self.audio is None or not self.audio.strip():
                raise ValueError("audio action requires audio")
            if "duration_ms" in self.model_fields_set:
                raise ValueError("audio action does not accept duration_ms")
        elif self.type == "pose":
            if not self.pose and (self.led is None or self.led.is_empty()):
                raise ValueError("pose action requires pose or led")
            if "duration_ms" not in self.model_fields_set:
                raise ValueError("pose action requires duration_ms")
        return self


class Command(BaseModel):
    actions: List[Action] = Field(default_factory=list)
