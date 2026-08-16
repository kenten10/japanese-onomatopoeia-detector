# 共有リソース

3プラットフォームが同じものを参照します。ここを変更すると、iOS・Android・PWAすべてのCIが動きます。

| ファイル | 使いみち |
|---|---|
| `onomatopoeia_dict.json` | オノマトペ辞書（51語、日英の意味と例文つき） |
| `feedback-form-url.txt` | ご意見フォームのURL。空なら設定画面に導線を出さない |

## ご意見フォームのURL

`feedback-form-url.txt` にURLを1行で書きます。空のままなら、3実装とも設定画面に導線が出ません。

```
https://docs.google.com/forms/d/e/xxxxxxxx/viewform
```

iOS・Androidはビルド時に読み込むため、変更後は再ビルドが必要です。
