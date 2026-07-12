# オノマトペ判定アプリ / Onomatopoeia Detector

iOSアプリ。音声入力された言葉のオノマトペらしさを5段階で評価します。

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
│   └── onomatopoeia_dict.json         # オノマトペ辞書（50語、日英意味付き）
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
| 5段階評価 | 辞書照合40%・音韻パターン30%・音象徴20%・形態素10% |
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

1. 辞書照合スコア（40%）: 約50語のオノマトペ辞書との一致度
2. 音韻パターンスコア（30%）: ABAB反復・促音・長音などの検出
3. 音象徴スコア（20%）: 濁音率・半濁音・長音マーカーの割合
4. 形態素解析スコア（10%）: 助詞・助動詞を含まない独立語

類似オノマトペはLevenshtein距離 + 文字重複率で算出。

## 注意事項

- 音声認識はインターネット不要（オンデバイス処理）
- 音声データは端末内でのみ処理され、外部送信されません
- シミュレータでは音声入力機能は動作しません（実機必須）
