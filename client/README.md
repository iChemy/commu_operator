# commu_operator client

CommU 側で起動している `commu_operator/backend` に、PC から音声・姿勢・LED・待機コマンドを送る CLI です。
ジェスチャー生成、LLM、TTS は含みません。

## セットアップ

```sh
uv sync
```

MP3 など WAV 以外の音声を入力する場合は、別途 `ffmpeg` が必要です。

## 構成

- `main.py`: CLI エントリーポイント
- `commu_operator_client/cli.py`: argparse とコマンド実行
- `commu_operator_client/models.py`: command/action JSON モデル
- `commu_operator_client/client.py`: TCP client
- `commu_operator_client/protocol.py`: backend へ送るメタデータ生成
- `commu_operator_client/audio.py`: 音声読み込みと WAV 変換
- `commu_operator_client/commands.py`: CLI 引数と JSON から command を組み立てる処理

## コマンド

サーボ名一覧:

```sh
uv run python main.py list-servos
```

音声ファイルを送る:

```sh
uv run python main.py send --host COMMU_IP_ADDRESS --audio path/to/speech.wav
```

姿勢を直接送る:

```sh
uv run python main.py send --host COMMU_IP_ADDRESS --pose HEAD_Y=20 --pose HEAD_P=-10 --duration-ms 800
```

LED を直接送る:

```sh
uv run python main.py send --host COMMU_IP_ADDRESS --led-body "#00aaff" --led-left-cheek 128
```

command JSON を送る:

```sh
uv run python main.py send --host COMMU_IP_ADDRESS --command command.json
```

batch JSON を順番に送る:

```sh
uv run python main.py batch --host COMMU_IP_ADDRESS batch.json
```

送信内容だけ確認する:

```sh
uv run python main.py send --pose HEAD_Y=20 --duration-ms 800 --dry-run
```

## Command JSON

```json
{
  "actions": [
    {
      "type": "pose",
      "duration_ms": 800,
      "pose": {
        "HEAD_Y": 20
      }
    },
    {
      "type": "wait",
      "duration_ms": 500
    },
    {
      "type": "led",
      "led": {
        "body": "#00aaff",
        "left_cheek": 128
      }
    }
  ]
}
```

`pose`、`led`、`wait` は独立した操作です。
姿勢を保つ時間は `wait` action として表現します。
