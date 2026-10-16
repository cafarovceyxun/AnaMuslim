// App Store məhsul səhifəsi başlıq şəkli (Product Page Header / universal asset).
//
// İşlətmə:  swift tools/app-store/make_header.swift
// Çıxış:    docs/app-store/header/header-3840x1646.png   (21:9, yalnız başlıq)
//           docs/app-store/header/universal-5244x2950.png (16:9, başlıq + axtarış)
//
// Apple şablonlarındakı «ART SAFE AREA» düzbucaqlıları (Sketch şablonundan ölçülüb):
//   başlıq 3840×1646   → 1646×661, başlanğıc (1097, 493)  — mərkəzdə
//   universal 5244×2950 → 1402×962, başlanğıc (1921, 660) — üfüqi mərkəzdə, 334 px yuxarıda
// Əsas obyekt (Bismillah) həmin zonaya sığdırılır, naxış və halqa isə kəsilə bilən fondur.
// Şəkildə alfa kanal olmamalıdır — kontekst `noneSkipLast` ilə qurulur.

import CoreGraphics
import CoreText
import Foundation
import ImageIO
import UniformTypeIdentifiers

let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
    .deletingLastPathComponent().deletingLastPathComponent()
let fontURL = root.appendingPathComponent(
    "shared/src/commonMain/composeResources/font/kfgqpc_uthman_taha_naskh.ttf")
let outDir = root.appendingPathComponent("docs/app-store/header")

// Fatiha 1:1 — tools/tajweed/cache/cpfair-quran-uthmani.txt ilə eyni mətn.
let bismillah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"

// İkonun rəngləri (iosApp/AppIcon.svg).
func rgb(_ hex: UInt32, _ a: CGFloat = 1) -> CGColor {
    CGColor(srgbRed: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: a)
}
let greenDeep = rgb(0x0F3420)
let greenIcon = rgb(0x1D5333)
let greenGlow = rgb(0x2A6E45)
let gold = 0xFFB000 as UInt32
let goldLight = 0xFFD978 as UInt32

struct Spec {
    let name: String
    let width: Int
    let height: Int
    let safe: CGRect // yuxarı-sol koordinatlarla
}

let specs = [
    Spec(name: "header-3840x1646", width: 3840, height: 1646,
         safe: CGRect(x: 1097, y: 493, width: 1646, height: 661)),
    Spec(name: "universal-5244x2950", width: 5244, height: 2950,
         safe: CGRect(x: 1921, y: 660, width: 1402, height: 962)),
]

func loadFont(size: CGFloat) -> CTFont {
    guard let provider = CGDataProvider(url: fontURL as CFURL),
          let cgFont = CGFont(provider) else { fatalError("şrift açılmadı: \(fontURL.path)") }
    return CTFontCreateWithGraphicsFont(cgFont, size, nil, nil)
}

func line(_ text: String, size: CGFloat) -> CTLine {
    let attrs: [NSAttributedString.Key: Any] = [
        NSAttributedString.Key(kCTFontAttributeName as String): loadFont(size: size),
    ]
    return CTLineCreateWithAttributedString(NSAttributedString(string: text, attributes: attrs))
}

/// 8 guşəli ulduz (iki kvadratın birləşməsi) konturu.
func octagram(_ c: CGPoint, _ r: CGFloat) -> CGPath {
    let p = CGMutablePath()
    let inner = r * cos(.pi / 4) / cos(.pi / 8)
    for i in 0..<16 {
        let rad = i % 2 == 0 ? r : inner
        let a = CGFloat(i) * .pi / 8
        let pt = CGPoint(x: c.x + rad * cos(a), y: c.y + rad * sin(a))
        if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
    }
    p.closeSubpath()
    return p
}

/// Sətrin bütün qliflərini (formalaşdırılmış ərəb hərfləri + hərəkələr) bir konturda birləşdirir.
func glyphPath(_ line: CTLine, at origin: CGPoint) -> CGPath {
    let path = CGMutablePath()
    for run in CTLineGetGlyphRuns(line) as! [CTRun] {
        let attrs = CTRunGetAttributes(run) as NSDictionary
        let font = attrs[kCTFontAttributeName as String] as! CTFont
        let count = CTRunGetGlyphCount(run)
        var glyphs = [CGGlyph](repeating: 0, count: count)
        var positions = [CGPoint](repeating: .zero, count: count)
        CTRunGetGlyphs(run, CFRange(), &glyphs)
        CTRunGetPositions(run, CFRange(), &positions)
        for i in 0..<count {
            guard let g = CTFontCreatePathForGlyph(font, glyphs[i], nil) else { continue }
            let t = CGAffineTransform(translationX: origin.x + positions[i].x, y: origin.y + positions[i].y)
            path.addPath(g, transform: t)
        }
    }
    return path
}

func render(_ spec: Spec) {
    let w = CGFloat(spec.width), h = CGFloat(spec.height)
    guard let ctx = CGContext(
        data: nil, width: spec.width, height: spec.height, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!,
        bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue) else { fatalError("kontekst") }

    // Yuxarı-sol koordinatdan CG-yə (aşağı-sol).
    let focus = CGPoint(x: spec.safe.midX, y: h - spec.safe.midY)
    let unit = spec.safe.width // bütün ölçülər təhlükəsiz zonanın eninə nisbətlə

    // 1. Fon: kənarlarda tünd, mərkəzdə ikonun yaşılından açıq halo.
    ctx.setFillColor(greenDeep)
    ctx.fill(CGRect(x: 0, y: 0, width: w, height: h))
    let bg = CGGradient(colorsSpace: nil, colors: [greenGlow, greenIcon, greenDeep] as CFArray,
                        locations: [0, 0.45, 1])!
    ctx.drawRadialGradient(bg, startCenter: focus, startRadius: 0, endCenter: focus,
                           endRadius: max(w, h) * 0.62, options: [.drawsAfterEndLocation])

    // 2. Həndəsi naxış: ulduz şəbəkəsi, mətnin altında seyrəlir.
    let step = unit * 0.21
    let starR = step * 0.36
    let cols = Int(w / step) + 2, rows = Int(h / step) + 2
    let originX = focus.x.truncatingRemainder(dividingBy: step) - step
    let originY = focus.y.truncatingRemainder(dividingBy: step) - step
    ctx.setLineWidth(max(2, unit * 0.0022))
    ctx.setLineJoin(.round)
    for row in 0...rows {
        for col in 0...cols {
            let c = CGPoint(x: originX + CGFloat(col) * step, y: originY + CGFloat(row) * step)
            let d = hypot((c.x - focus.x) / (unit * 0.62), (c.y - focus.y) / (unit * 0.36))
            // mərkəzdə 0 → kənara doğru yığılır, çox uzaqda yenə zəifləyir
            let near = min(1, max(0, (d - 0.75) / 0.9))
            let far = min(1, max(0, 1 - (hypot(c.x - focus.x, c.y - focus.y) - unit * 1.1) / (unit * 1.6)))
            let alpha = 0.16 * near * (0.35 + 0.65 * far)
            if alpha < 0.005 { continue }
            ctx.setStrokeColor(rgb(gold, alpha))
            ctx.addPath(octagram(c, starR))
            // qonşu ulduzların uclarını birləşdirən xətlər + hücrə mərkəzində romb
            ctx.move(to: CGPoint(x: c.x + starR, y: c.y)); ctx.addLine(to: CGPoint(x: c.x + step - starR, y: c.y))
            ctx.move(to: CGPoint(x: c.x, y: c.y + starR)); ctx.addLine(to: CGPoint(x: c.x, y: c.y + step - starR))
            let m = CGPoint(x: c.x + step / 2, y: c.y + step / 2), dr = step * 0.17
            ctx.move(to: CGPoint(x: m.x, y: m.y - dr)); ctx.addLine(to: CGPoint(x: m.x + dr, y: m.y))
            ctx.addLine(to: CGPoint(x: m.x, y: m.y + dr)); ctx.addLine(to: CGPoint(x: m.x - dr, y: m.y))
            ctx.closePath()
            ctx.strokePath()
        }
    }

    // 3. İkondakı qoşa qızılı halqanın əks-sədası.
    let ringR = unit * 0.66
    ctx.setStrokeColor(rgb(gold, 0.55))
    ctx.setLineWidth(unit * 0.006)
    ctx.strokeEllipse(in: CGRect(x: focus.x - ringR, y: focus.y - ringR, width: ringR * 2, height: ringR * 2))
    let ring2 = ringR - unit * 0.018
    ctx.setStrokeColor(rgb(gold, 0.35))
    ctx.setLineWidth(unit * 0.0016)
    ctx.strokeEllipse(in: CGRect(x: focus.x - ring2, y: focus.y - ring2, width: ring2 * 2, height: ring2 * 2))

    // 4. Bismillah — təhlükəsiz zonanın ~88%-i enində.
    let probe = line(bismillah, size: 100)
    var ascent: CGFloat = 0, descent: CGFloat = 0, leading: CGFloat = 0
    let probeWidth = CGFloat(CTLineGetTypographicBounds(probe, &ascent, &descent, &leading))
    let size = 100 * spec.safe.width * 0.88 / probeWidth
    let text = line(bismillah, size: size)
    let ink = CTLineGetImageBounds(text, ctx)
    let textOrigin = CGPoint(x: focus.x - ink.midX, y: focus.y - ink.midY + unit * 0.03)

    // CTLineDraw rəngi atributdan alır və clip rejiminə tabe olmur — hərfləri kontur kimi yığırıq.
    let glyphs = glyphPath(text, at: textOrigin)
    let textBox = glyphs.boundingBoxOfPath

    // yumşaq parıltı
    ctx.saveGState()
    ctx.setShadow(offset: .zero, blur: unit * 0.05, color: rgb(gold, 0.45))
    ctx.setFillColor(rgb(gold))
    ctx.addPath(glyphs)
    ctx.fillPath()
    ctx.restoreGState()

    // qızılı qradiyent: hərf konturunu kəsik kimi işlət
    ctx.saveGState()
    ctx.addPath(glyphs)
    ctx.clip()
    let goldGrad = CGGradient(colorsSpace: nil,
                              colors: [rgb(goldLight), rgb(0xFFC23A), rgb(0xE89A00)] as CFArray,
                              locations: [0, 0.5, 1])!
    ctx.drawLinearGradient(goldGrad, start: CGPoint(x: textBox.midX, y: textBox.maxY),
                           end: CGPoint(x: textBox.midX, y: textBox.minY), options: [])
    ctx.restoreGState()

    // 5. Altında incə bəzək xətti: xətt — romb — xətt.
    let divY = textBox.minY - unit * 0.07
    let half = unit * 0.24, gem = unit * 0.014
    ctx.setStrokeColor(rgb(gold, 0.8))
    ctx.setLineWidth(unit * 0.0025)
    ctx.move(to: CGPoint(x: focus.x - half, y: divY)); ctx.addLine(to: CGPoint(x: focus.x - gem * 2.2, y: divY))
    ctx.move(to: CGPoint(x: focus.x + gem * 2.2, y: divY)); ctx.addLine(to: CGPoint(x: focus.x + half, y: divY))
    ctx.strokePath()
    ctx.setFillColor(rgb(gold))
    ctx.move(to: CGPoint(x: focus.x, y: divY - gem)); ctx.addLine(to: CGPoint(x: focus.x + gem, y: divY))
    ctx.addLine(to: CGPoint(x: focus.x, y: divY + gem)); ctx.addLine(to: CGPoint(x: focus.x - gem, y: divY))
    ctx.closePath()
    ctx.fillPath()

    // 6. Yaz.
    guard let image = ctx.makeImage() else { fatalError("şəkil") }
    let url = outDir.appendingPathComponent("\(spec.name).png")
    guard let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)
    else { fatalError("fayl") }
    CGImageDestinationAddImage(dest, image, nil)
    guard CGImageDestinationFinalize(dest) else { fatalError("yazılmadı") }
    print("✓ \(url.path)  (təhlükəsiz zonada mətn: \(Int(textBox.width))×\(Int(textBox.height)) px)")
}

try FileManager.default.createDirectory(at: outDir, withIntermediateDirectories: true)
specs.forEach(render)
