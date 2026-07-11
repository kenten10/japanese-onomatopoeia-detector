import SwiftUI
import UIKit

// MARK: - Manga sound-effect (描き文字) design system
//
// オノマトペが最も生きる世界＝マンガの効果音。
// 白い紙・黒インク・朱色のアクセント・網点（スクリーントーン）を基調に、
// 認識した言葉を効果音のように大きく弾けさせる。

// MARK: Palette

enum Ink {
    /// 紙。ライトは温かみのある白、ダークは墨色の紙。
    static let paper = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark
            ? UIColor(red: 0.090, green: 0.082, blue: 0.063, alpha: 1) // #17150F
            : UIColor(red: 0.984, green: 0.980, blue: 0.965, alpha: 1) // #FBFAF6
    })

    /// インク。線と文字の主色。
    static let ink = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark
            ? UIColor(red: 0.949, green: 0.933, blue: 0.894, alpha: 1) // #F2EEE4
            : UIColor(red: 0.078, green: 0.078, blue: 0.078, alpha: 1) // #141414
    })

    /// 朱。効果音のエネルギーを担う唯一のアクセント。
    static let vermilion = Color(red: 0.910, green: 0.255, blue: 0.180) // #E8412E

    /// パネルの下地（紙よりわずかに浮いた面）。
    static let panel = Color(uiColor: UIColor { tc in
        tc.userInterfaceStyle == .dark
            ? UIColor(red: 0.129, green: 0.118, blue: 0.098, alpha: 1)
            : UIColor(red: 1.0, green: 1.0, blue: 0.996, alpha: 1)
    })

    /// スコアに応じたインクの「熱量」。高いほど朱に近づき、低いほど淡い墨になる。
    static func score(_ s: Int) -> Color {
        switch s {
        case 5:  return vermilion
        case 4:  return ink
        case 3:  return ink.opacity(0.70)
        case 2:  return ink.opacity(0.48)
        default: return ink.opacity(0.32)
        }
    }
}

// MARK: Type roles

extension Font {
    /// 描き文字。認識した言葉を効果音として弾けさせる用。
    static func sfx(_ size: CGFloat) -> Font { .system(size: size, weight: .black, design: .rounded) }
    /// 見出し。太いラウンド。
    static func mangaHeading(_ size: CGFloat) -> Font { .system(size: size, weight: .heavy, design: .rounded) }
}

// MARK: Halftone (網点 / スクリーントーン)

struct Halftone: View {
    var color: Color = Ink.ink
    var dot: CGFloat = 2.4
    var spacing: CGFloat = 11

    var body: some View {
        Canvas { ctx, size in
            let cols = Int(size.width / spacing) + 2
            let rows = Int(size.height / spacing) + 2
            for r in 0..<rows {
                for c in 0..<cols {
                    let x = CGFloat(c) * spacing + (r % 2 == 0 ? 0 : spacing / 2)
                    let y = CGFloat(r) * spacing
                    let rect = CGRect(x: x, y: y, width: dot, height: dot)
                    ctx.fill(Path(ellipseIn: rect), with: .color(color))
                }
            }
        }
        .allowsHitTesting(false)
    }
}

// MARK: Speed lines (集中線)

struct SpeedLines: View {
    var color: Color = Ink.ink
    var count: Int = 56
    var innerRatio: CGFloat = 0.30

    var body: some View {
        Canvas { ctx, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let maxR = max(size.width, size.height)
            for i in 0..<count {
                let angle = (Double(i) / Double(count)) * 2 * .pi
                let inner = maxR * innerRatio
                var path = Path()
                path.move(to: CGPoint(x: center.x + CGFloat(cos(angle)) * inner,
                                      y: center.y + CGFloat(sin(angle)) * inner))
                path.addLine(to: CGPoint(x: center.x + CGFloat(cos(angle)) * maxR,
                                         y: center.y + CGFloat(sin(angle)) * maxR))
                ctx.stroke(path, with: .color(color), lineWidth: i % 2 == 0 ? 2 : 0.8)
            }
        }
        .allowsHitTesting(false)
    }
}

// MARK: Manga panel (コマ枠)

private struct MangaPanel: ViewModifier {
    var radius: CGFloat
    var border: CGFloat
    var offset: CGFloat

    func body(content: Content) -> some View {
        content
            .background(Ink.panel, in: RoundedRectangle(cornerRadius: radius))
            .background(
                // 印刷のズレを思わせるハードなオフセット影
                RoundedRectangle(cornerRadius: radius)
                    .fill(Ink.ink)
                    .offset(x: offset, y: offset)
            )
            .overlay(
                RoundedRectangle(cornerRadius: radius)
                    .stroke(Ink.ink, lineWidth: border)
            )
    }
}

extension View {
    /// マンガのコマ枠。太いインク線とハードなオフセット影で「印刷された紙」の質感を出す。
    func mangaPanel(radius: CGFloat = 6, border: CGFloat = 2.5, offset: CGFloat = 4) -> some View {
        modifier(MangaPanel(radius: radius, border: border, offset: offset))
    }
}
