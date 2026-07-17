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

サーボ名と指定可能な角度範囲:

```sh
uv run python main.py list-servos
```

表示される `range_deg` は command JSON や `--pose` に degree 単位で指定する値の範囲です。
範囲外の値は backend 側で安全範囲に丸められます。

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
`led` action は CommU の LED 一式を更新します。
未指定の `body` / `power_button` は `#000000`、未指定の `left_cheek` / `right_cheek` は `0` として扱われます。

`command` は、backend に 1 回送る操作のまとまりです。
`send --command command.json` では、この 1 つの command を 1 TCP 接続で送信します。
音声を同時に再生したい場合は、command JSON に音声パスを書くのではなく、`--audio path/to/file.wav` を併用します。

テスト用の command JSON:

```sh
uv run python main.py send --command examples/command_pose_led_wait.json --dry-run
```

実機へ送る場合:

```sh
uv run python main.py send --host COMMU_IP_ADDRESS --command examples/command_pose_led_wait.json
```

## Batch JSON

`batch` は command を複数並べたものです。
client は `commands` の先頭から順に 1 command ずつ送信し、backend から `OK` が返ってから次を送ります。
各 command には必要に応じて `audio` を書けます。
`audio` が相対パスの場合は、batch JSON ファイルからの相対パスとして解決されます。

```json
{
  "commands": [
    {
      "audio": "audio/part_001.wav",
      "actions": [
        {
          "type": "pose",
          "duration_ms": 500,
          "pose": {
            "HEAD_Y": 20
          }
        },
        {
          "type": "wait",
          "duration_ms": 500
        }
      ]
    },
    {
      "actions": [
        {
          "type": "led",
          "led": {
            "body": "#00aaff"
          }
        }
      ]
    }
  ]
}
```

テスト用の batch JSON:

```sh
uv run python main.py batch examples/batch_test.json --dry-run
```

実機へ送る場合:

```sh
uv run python main.py batch --host COMMU_IP_ADDRESS examples/batch_test.json
```
