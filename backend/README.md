# commu_operator backend

CommU 上で起動する TCP サーバーです。
client から受け取ったコマンドに従って、音声再生、姿勢遷移、LED 制御を実行します。

## 準備

`aplay` コマンドが使える状態にしてください。
受信した WAV は `sound/current_command.wav` に保存され、`aplay sound/current_command.wav` で再生されます。

以下の JAR を `lib` 配下に配置してください。

- `core-2.2.jar`
- `javase-2.2.jar`
- `jna-4.1.0.jar`
- `opencv-310.jar`
- `sotalib.jar`
- `gson-2.8.9.jar`

## 起動

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
