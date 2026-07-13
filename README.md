# オノマトペ判定アプリ / Onomatopoeia Detector

iOSアプリ。音声入力された日本語をひらがな表記に整え、オノマトペらしさを5段階で評価します。

## 必要環境

- Xcode 15.0+
- iOS 16.0+（実機推奨 — シミュレータではマイク・音声認識が制限されます）

## セットアップ

1. **プロジェクトを開く**
   ```
   open OnomatopoeiaDetector.xcodeproj
   ```

2. **Bundle Identifierを変更**（署名エラー回避）
   - Target → Signing & Capabilities
   - Bundle Identifier を `com.yourname.OnomatopoeiaDetector` に変更
   - Team を自分のApple IDに設定

3. **ビルド & 実行**
   - 実機を接続してRunボタン
   - 初回起動時にマイク・音声認識の許可を求めるダイアログが表示されます

## プロジェクト構成

```
OnomatopoeiaDetector/
├── OnomatopoeiaDetectorApp.swift     # エントリーポイント
├── ContentView.swift                  # タブナビゲーション
├── Views/
│   ├── HomeView.swift                 # メイン画面（録音ボタン・波形）
│   ├── ResultView.swift               # 評価結果＋類似オノマトペカード
│   ├── HistoryView.swift              # 判定履歴
│   └── SettingsView.swift             # 設定（言語・履歴クリア）
├── ViewModels/
│   └── AppViewModel.swift             # MVVM ViewModel
├── Models/
│   ├── Models.swift                   # データモデル
│   ├── PersistenceController.swift    # Core Data管理
│   ├── HistoryEntity.swift            # NSManagedObject
│   └── OnomatopoeiaDetector.xcdatamodeld/
├── Engine/
│   ├── OnoEngine.swift                # 評価エンジン（辞書照合・音韻解析）
│   └── SpeechManager.swift            # AVFoundation + Speech Framework・音声認識結果のひらがな変換
├── Resources/
│   └── onomatopoeia_dict.json         # オノマトペ辞書（51語、日英意味付き）
├── Localizable/
│   ├── ja.lproj/Localizable.strings   # 日本語UI文字列
│   └── en.lproj/Localizable.strings   # 英語UI文字列
└── Info.plist                          # マイク・音声認識権限設定

OnomatopoeiaDetectorTests/
├── AppLanguageTests.swift
├── OnoEngineTests.swift
├── PersistenceControllerTests.swift
└── SpeechTextConverterTests.swift

```

## テスト

Xcodeでは共有scheme `OnomatopoeiaDetector` を選び、⌘UでUnit Testを実行します。

CLIでは利用可能なiOS Simulatorを指定して実行します。

```sh
xcodebuild test \
  -project OnomatopoeiaDetector.xcodeproj \
  -scheme OnomatopoeiaDetector \
  -destination 'platform=iOS Simulator,name=iPhone 16'
```

## 主な機能

| 機能 | 詳細 |
|------|------|
| 音声入力 | タップで録音開始、最大10秒、自動停止 |
| 音声認識 | SFSpeechRecognizer（日本語・オンデバイス） |
| ひらがな表示 | 音声認識結果をひらがな化し、`しいん` → `しーん` など一部のオノマトペ長音表記を補正 |
| 5段階評価 | 辞書完全一致は評価5。その他は辞書照合40%・音韻パターン30%・音象徴20%・形態素10% |
| 類似表示 | スコア3以上で類似オノマトペの日英意味・例文表示 |
| 履歴 | Core Dataで最大100件保存 |
| 多言語UI | 日本語・英語（システム言語自動追従） |

## 技術スタック

- SwiftUI + MVVM
- AVFoundation（録音）
- Speech Framework（日本語音声認識・オンデバイス）
- NaturalLanguage（形態素解析）
- Core Data（履歴永続化）
- Localizable.strings（日英対応）

## 評価アルゴリズム

入力は評価前にカタカナをひらがなへ正規化し、空白・記号を除去します。

1. 辞書完全一致: 51語の辞書の `word` または `reading` と一致した場合は評価5
2. 辞書照合スコア（40%）: 完全一致以外の辞書との一致度・部分一致度
3. 音韻パターンスコア（30%）: ABAB反復・促音・長音・撥音などの検出
4. 音象徴スコア（20%）: 濁音率・半濁音・長音マーカーなどの割合
5. 形態素解析スコア（10%）: 助詞・動詞を含まない独立語らしさ

類似オノマトペはLevenshtein距離 + 文字重複率で算出。

## 音声認識結果の表記

音声認識結果は、画面表示・評価・履歴保存に渡す前にひらがなへ変換します。

- `SFTranscription` の segment 候補にかな表記がある場合は、その候補を優先します
- 漢字だけが返った場合は tokenizer による読み推定にフォールバックします
- カタカナはひらがなへ変換します
- オノマトペとして明確な長音誤表記は補正します（例: `しいん` → `しーん`）

## 注意事項

- 音声認識はインターネット不要（オンデバイス処理）
- 音声データは端末内でのみ処理され、外部送信されません
- シミュレータでは音声入力機能が制限されるため、音声入力の確認は実機推奨です
