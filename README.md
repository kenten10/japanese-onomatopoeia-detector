# オノマトペ判定アプリ / Onomatopoeia Detector

iOSアプリとPWA。音声入力された日本語をひらがな表記に整え、オノマトペらしさを5段階で評価します。

## PWA（Web版）

React、TypeScript、Viteで実装したWeb版は `web/` にあります。既存iOS版と同じ3タブ、判定アルゴリズム、最大100件の端末内履歴、日本語・英語UIを備えています。

### 必要環境

- Node.js 22+
- npm 10+
- マイクを使う場合はHTTPSまたはlocalhost

### 開発とビルド

```sh
cd web
npm install
npm run dev
```

プロダクション用の静的ファイルは次のコマンドで `web/dist/` に生成されます。

```sh
npm run build
npm run preview
```

`dist/` を任意のHTTPS対応静的ホスティングへ配置できます。Web App ManifestとService Workerが含まれ、初回読み込み後は画面、辞書、判定、履歴をオフラインで利用できます。

### Web版のテスト

```sh
npm test
npx playwright install chromium webkit  # 初回のみ
npm run test:e2e
```

### iPhoneへのインストール

1. Safariで配信URLを開く
2. 共有ボタンをタップする
3. 「ホーム画面に追加」を選ぶ

Web版は利用可能な場合にオンデバイス音声認識を優先します。利用できない場合はブラウザの音声認識サービスへフォールバックするため、その場合の音声認識にはネットワーク接続が必要です。履歴と表示言語はサーバーではなく、現在の端末・ブラウザ・サイト単位のIndexedDBとWeb Storageに保存され、別のユーザーや端末とは共有されません。

> [!IMPORTANT]
> WebKitの制約により、iPhoneでホーム画面に追加したPWAでは音声認識サービスを利用できない場合があります。その場合、アプリ内の「Safariで開く」から同じURLをSafariタブで開いて録音してください。また、iPhoneの「設定」でSiriと音声入力が有効になっている必要があります。リリース前には対象iOS実機のSafariタブとホーム画面版の両方で録音を確認してください。

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
