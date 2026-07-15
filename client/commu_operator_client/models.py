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
    type: Literal["pose", "led", "wait"]
    duration_ms: int = Field(default=0, ge=0)
    pose: Dict[ServoName, float] = Field(default_factory=dict)
    led: Optional[LEDSettings] = None

    @model_validator(mode="after")
    def validate_payload(self) -> "Action":
        if self.type == "pose" and not self.pose:
            raise ValueError("pose action requires pose")
        if self.type == "led" and (self.led is None or self.led.is_empty()):
            raise ValueError("led action requires at least one LED value")
        return self


class Command(BaseModel):
    actions: List[Action] = Field(default_factory=list)


class BatchCommand(Command):
    audio: Optional[str] = None


class CommandBatch(BaseModel):
    commands: List[BatchCommand] = Field(default_factory=list)
