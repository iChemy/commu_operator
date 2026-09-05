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

## 現在姿勢の取得

client の `get-pose` リクエストを受けると、同じサーバープロセスが CommU のサーボから現在位置を
読み取り、client で送信可能な 13 関節を degree の JSON 数値として返します。
読取値を送信値へ戻す変換には、姿勢 command の受信時と同じサーボ定義・減速比を使います。
サーボ値を読み取れない場合は、不正な角度を返さず `ERR` 応答にします。

別の Java プロセスからロボットを再初期化する必要はありません。起動中のサーバーが保持している
接続を使うため、通常どおり backend を起動した状態で client から実行してください。

## テスト

実機接続を使わない変換・プロトコルテストは、PowerShell では次のように実行できます。

```powershell
$sources = Get-ChildItem -Recurse src/main/java,src/test/java -Filter *.java | ForEach-Object { $_.FullName }
javac -cp "lib/*" -d build/test-classes $sources
java -cp "build/test-classes;lib/*" commu.BackendTests
```
