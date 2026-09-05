# commu_operator

CommU を PC 側の client から操作するための最小構成です。

`commu_operator_mock` に含まれていたジェスチャー生成、LLM、TTS、生成済み実行データは含めていません。
このプロジェクトは、既に用意した音声ファイルや command JSON を CommU 側 backend に送り、
CommU に発話、姿勢変更、LED 制御、待機を実行させることだけを扱います。

## 構成

- `backend`: CommU 上で起動する Java TCP サーバー
- `client`: PC 側から backend にコマンドを送る Python CLI
- `backend/lib`: CommU/Vstone 操作用 JAR

## backend の起動

CommU 上で `backend` ディレクトリに移動し、コンパイルして起動します。

Windows/PowerShell:

```powershell
$sources = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -cp "lib/*" -d build/classes $sources
java -cp "build/classes;lib/*" commu.Main --port 5000
```

Linux:

```sh
find src/main/java -name "*.java" -print | xargs javac -cp "lib/*" -d build/classes
java -cp "build/classes:lib/*" commu.Main --port 5000
```

`--port` を省略した場合は `5000` で待ち受けます。

## client の使い方

PC 側で `client` ディレクトリに移動して実行します。

```sh
uv run python main.py list-servos
uv run python main.py send --host COMMU_IP_ADDRESS --audio path/to/speech.wav
uv run python main.py send --host COMMU_IP_ADDRESS --pose HEAD_Y=20 --duration-ms 800
uv run python main.py send --host COMMU_IP_ADDRESS --led-body "#00aaff" --duration-ms 800
uv run python main.py send --host COMMU_IP_ADDRESS --command examples/command_pose_led_wait.json
uv run python main.py send --host COMMU_IP_ADDRESS --command examples/command_greeting_with_gesture.json
uv run python main.py get-pose --host COMMU_IP_ADDRESS
```

現在の姿勢を JSON に保存し、そのまま再送する場合:

```sh
uv run python main.py get-pose --host COMMU_IP_ADDRESS --duration-ms 800 --output captured_pose.json
uv run python main.py send --host COMMU_IP_ADDRESS --command captured_pose.json
```

`get-pose` は CommU のサーボから読み取った現在角度を degree に変換し、1 個の `pose` action を持つ
完全な command JSON を標準出力へ出します。`--duration-ms` は、その JSON を再送するときの姿勢遷移時間です。
`--output` を指定すると、リダイレクトを使わず UTF-8 の JSON ファイルへ保存できます。
出力する 13 関節の名前と値は、command JSON の `pose` と同じ形式です。
client から制御できない口のサーボは含みません。

送信内容だけ確認する場合:

```sh
uv run python main.py send --command examples/command_greeting_with_gesture.json --dry-run
```

MP3 など WAV 以外の音声を渡す場合、client 側で `ffmpeg` が必要です。
client は送信前に音声を `Signed 16 bit Little Endian / 22050 Hz / Mono` の WAV に変換します。

## Command JSON

`command` は、backend に 1 回送る操作のまとまりです。
`actions` に `audio`、`pose`、`wait` を順番に並べます。

```json
{
  "actions": [
    {
      "type": "audio",
      "audio": "greeting.mp3"
    },
    {
      "type": "pose",
      "duration_ms": 400,
      "pose": {
        "HEAD_P": 15,
        "HEAD_Y": 0
      },
      "led": {
        "body": "#00aaff",
        "left_cheek": 128,
        "right_cheek": 128,
        "power_button": "#ff6688"
      }
    },
    {
      "type": "wait",
      "duration_ms": 300
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

テスト用 JSON は `client/examples/` 配下にあります。

## サポートするサーボ名

- `BODY_P`
- `BODY_Y`
- `L_SHOULDER_P`
- `L_SHOULDER_R`
- `R_SHOULDER_P`
- `R_SHOULDER_R`
- `HEAD_P`
- `HEAD_R`
- `HEAD_Y`
- `EYE_P`
- `L_EYE_Y`
- `R_EYE_Y`
- `EYELIDS`
