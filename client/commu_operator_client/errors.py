class CommUClientError(Exception):
    pass


class CommUConnectionError(CommUClientError):
    pass


class CommUServerError(CommUClientError):
    pass


class InvalidCommandError(CommUClientError):
    pass


class AudioConversionError(CommUClientError):
    pass
