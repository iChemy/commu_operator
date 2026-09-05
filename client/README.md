# commu_operator client

CommU 側で起動している `commu_operator/backend` に、PC から音声・姿勢・LED・待機 action を送る CLI です。
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
uv run python main.py send --host COMMU_IP_ADDRESS --led-body "#00aaff" --led-left-cheek 128 --duration-ms 800
```

LED も `pose` action の一部として送られるため、`--duration-ms` を指定できます。
姿勢と LED を同時に変化させたい場合は、`--pose` と `--led-*` を同じ `send` に指定します。

command JSON を送る:

```sh
uv run python main.py send --host COMMU_IP_ADDRESS --command examples/command_pose_led_wait.json
```

送信内容だけ確認する:

```sh
uv run python main.py send --command examples/command_greeting_with_gesture.json --dry-run
```

現在の姿勢を取得する:

```sh
uv run python main.py get-pose --host COMMU_IP_ADDRESS
```

出力は、取得した 13 関節の実測角度を `pose` に含む完全な command JSON です。
値の単位は `send --pose` と同じ degree なので、ファイルへ保存して無加工で再送できます。

```sh
uv run python main.py get-pose --host COMMU_IP_ADDRESS --duration-ms 800 --output captured_pose.json
uv run python main.py send --host COMMU_IP_ADDRESS --command captured_pose.json
```

`--duration-ms` の既定値は `1000` で、取得処理の待ち時間ではなく、生成した JSON を再送するときの
姿勢遷移時間です。`--output` は OS のシェルに依存せず UTF-8 で保存します。省略時は標準出力へ表示します。
口のサーボ (`MOUTH`) は client から姿勢値を送れないため、取得結果には含みません。
サーバーが別の command を実行中の場合、`get-pose` はその command の全 action が完了してから処理されます。
この機能を使う前に、client と backend の両方を更新して backend を再起動してください。

## Command JSON

`command` は、backend に 1 回送る操作のまとまりです。
`send --command command.json` では、この 1 つの command を 1 TCP 接続で送信します。
複数の動作を続けて実行したい場合は、`actions` に順番に並べます。

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
      },
      "led": {
        "body": "#00aaff",
        "left_cheek": 128
      }
    },
    {
      "type": "wait",
      "duration_ms": 500
    }
  ]
}
```

`audio` action は音声再生を開始するだけで、`duration_ms` は指定できません。
音声再生中だけ動作を止めたい場合は、音声長に相当する `wait` action を `audio` の後に置きます。
`audio` が相対パスの場合は、command JSON ファイルからの相対パスとして解決されます。

`pose` action は指定された姿勢へ `duration_ms` ミリ秒で移動します。
LED は `pose` action の `led` フィールドとして指定します。
`pose` と `led` を同じ action に書くと、同じ `duration_ms` で姿勢遷移と LED 変化を実行します。
LED だけを変えたい場合も、`pose` action に `led` だけを書きます。
未指定の `body` / `power_button` は `#000000`、未指定の `left_cheek` / `right_cheek` は `0` として扱われます。

`wait` action は指定されたミリ秒だけ待機します。
姿勢を保つ時間は `pose.duration_ms` を長くするか、必要に応じて `wait` action として表現します。

## Examples

姿勢・LED・待機のテスト:

```sh
uv run python main.py send --command examples/command_pose_led_wait.json --dry-run
uv run python main.py send --host COMMU_IP_ADDRESS --command examples/command_pose_led_wait.json
```

`examples/greeting.mp3` を再生しながら姿勢と LED を動かすテスト:

```sh
uv run python main.py send --command examples/command_greeting_with_gesture.json --dry-run
uv run python main.py send --host COMMU_IP_ADDRESS --command examples/command_greeting_with_gesture.json
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

## テスト

```sh
uv run python -m unittest discover -s tests -v
```
