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

`--audio` は `audio` action を 1 つ作り、音声再生を開始します。
音声の終了は待ちません。

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

batch JSON を 1 command にまとめて送る:

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
      "type": "audio",
      "audio": "greeting.mp3"
    },
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

`audio`、`pose`、`led`、`wait` は独立した操作です。
`audio` action は音声再生を開始するだけで、`duration_ms` は指定できません。
音声再生中だけ動作を止めたい場合は、音声長に相当する `wait` action を `audio` の後に置きます。
`audio` が相対パスの場合は、command JSON ファイルからの相対パスとして解決されます。
姿勢を保つ時間は `wait` action として表現します。
`led` action は CommU の LED 一式を更新します。
未指定の `body` / `power_button` は `#000000`、未指定の `left_cheek` / `right_cheek` は `0` として扱われます。

`command` は、backend に 1 回送る操作のまとまりです。
`send --command command.json` では、この 1 つの command を 1 TCP 接続で送信します。
音声を含めたい場合は、command JSON 内に `audio` action を書きます。
`--audio` は JSON を使わずに音声再生だけを直接送るための簡易オプションです。

テスト用の command JSON:

```sh
uv run python main.py send --command examples/command_pose_led_wait.json --dry-run
```

実機へ送る場合:

```sh
uv run python main.py send --host COMMU_IP_ADDRESS --command examples/command_pose_led_wait.json
```

## Batch JSON

`batch` は command を複数並べたものですが、送信時には全 command の `actions` を 1 つに結合し、1 TCP 接続で 1 command として送ります。
command 間で `OK` 待ちをしないため、通信往復による余分な遅延は入りません。
時間制御は `pose.duration_ms` と `wait.duration_ms` で明示します。
音声付きで実行したい場合は、`audio` action を置きます。
backend は `audio` action に到達した時点で音声再生を開始し、待たずに次の action へ進みます。
`audio` が相対パスの場合は、batch JSON ファイルからの相対パスとして解決されます。

```json
{
  "commands": [
    {
      "actions": [
        {
          "type": "audio",
          "audio": "audio/part_001.wav"
        },
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

音声付きテスト用の batch JSON:

```sh
uv run python main.py batch examples/batch_greeting_with_gesture.json --dry-run
```

実機へ送る場合:

```sh
uv run python main.py batch --host COMMU_IP_ADDRESS examples/batch_test.json
uv run python main.py batch --host COMMU_IP_ADDRESS examples/batch_greeting_with_gesture.json
```

## サーボ詳細

```
> uv run python main.py list-servos
name            range_deg    ratio
--------------  -----------  -------
BODY_P          -15..15      3.833  
BODY_Y          -67..67      1      
L_SHOULDER_P    -108..108    1.364  
L_SHOULDER_R    -45..30      1      
R_SHOULDER_P    -108..108    1.364  
R_SHOULDER_R    -30..45      1      
HEAD_P          -20..25      1      
HEAD_R          -15..15      4.333  
HEAD_Y          -85..85      1      
EYE_P           -22..22      1      
L_EYE_Y         -35..20      1      
R_EYE_Y         -20..35      1      
EYELIDS         -65..3       1      

Angles in command JSON and --pose are degrees. Values outside range are clamped by backend.
```
