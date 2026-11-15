// Hazır reklam bannerini App Store məhsul səhifəsi başlığına (Product Page Header) yerləşdirir.
//
// İşlətmə:  swift tools/app-store/make_banner_header.swift
// Giriş:    fastlane/screenshots/ios/raw/banner-2000x968.jpg
// Çıxış:    docs/app-store/header/banner-header-3840x1646.png
//           docs/app-store/header/banner-universal-5244x2950.png
//
// Banner kətanın BÜTÜN enini tutur (loqo solda, telefonlar sağda qalır):
//   - 3840×1646 (21:9) bannerdən enlidir → yuxarıdan/aşağıdan ~106 px naxış kəsilir;
//   - 5244×2950 (16:9) bannerdən hündürdür → yuxarıda/aşağıda ~206 px zolaq qalır, o da bannerin
//     öz bulanıq nüsxəsi ilə doldurulur və yumşaq keçidlə birləşir.
// Qeyd: Apple yalnız mərkəzdəki «ART SAFE AREA»-nı hər cihazda göstərməyə zəmanət verir
// (`make_header.swift`), yəni dar iPhone ekranında sol/sağ kənarlar kəsilə bilər. Banneri tam həmin
// zonaya sığdırmaq sınandı (2026-11-15) — iPad/Mac-də çox kiçik qaldı və rədd edildi.
// Mənbə 2000 px endir, ona görə Lanczos ilə böyüdülür. Çıxışda alfa kanal yoxdur.

import CoreImage
import Foundation
import ImageIO
import UniformTypeIdentifiers

let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
    .deletingLastPathComponent().deletingLastPathComponent()
let input = root.appendingPathComponent("fastlane/screenshots/ios/raw/banner-2000x968.jpg")
let outDir = root.appendingPathComponent("docs/app-store/header")

struct Spec {
    let name: String
    let width: CGFloat
    let height: CGFloat
}

let specs = [
    Spec(name: "banner-header-3840x1646", width: 3840, height: 1646),
    Spec(name: "banner-universal-5244x2950", width: 5244, height: 2950),
]

guard let banner = CIImage(contentsOf: input) else { fatalError("açılmadı: \(input.path)") }
let bw = banner.extent.width, bh = banner.extent.height
let sRGB = CGColorSpace(name: CGColorSpace.sRGB)!
let ci = CIContext(options: [.workingColorSpace: sRGB, .outputColorSpace: sRGB])

/// Lanczos ilə miqyaslayıb `center`-ə yerləşdirir (adi affine-dən kəskin böyüdür).
func placed(scale: CGFloat, center: CGPoint) -> CIImage {
    let scaled = banner.applyingFilter("CILanczosScaleTransform", parameters: [
        kCIInputScaleKey: scale, kCIInputAspectRatioKey: 1.0,
    ])
    let e = scaled.extent
    return scaled.transformed(by: CGAffineTransform(translationX: center.x - e.midX, y: center.y - e.midY))
}

func render(_ spec: Spec) {
    let canvas = CGRect(x: 0, y: 0, width: spec.width, height: spec.height)
    let center = CGPoint(x: canvas.midX, y: canvas.midY)

    // Ön plan: bütün en.
    let front = placed(scale: spec.width / bw, center: center)

    var out = front
    if front.extent.height < spec.height {
        // Boş qalan üst/alt zolaqlar: bannerin özü, kətanı örtəcək qədər böyüdülüb güclü bulanıqlaşdırılmış.
        let cover = max(spec.width / bw, spec.height / bh) * 1.04
        let backdrop = placed(scale: cover, center: center)
            .clampedToExtent()
            .applyingGaussianBlur(sigma: Double(spec.width * 0.02))
            .applyingFilter("CIColorControls", parameters: [kCIInputBrightnessKey: -0.06])
            .cropped(to: canvas)

        // Yalnız üst/alt kənarı əridən maska (sol/sağ kənar kətanın kənarıdır).
        let feather = front.extent.height * 0.05
        let maskRect = CGRect(x: canvas.minX - 100, y: front.extent.minY + feather * 0.7,
                              width: canvas.width + 200, height: front.extent.height - feather * 1.4)
        let mask = CIImage(color: .white).cropped(to: maskRect)
            .composited(over: CIImage(color: .black).cropped(to: canvas.insetBy(dx: -200, dy: -200)))
            .applyingGaussianBlur(sigma: Double(feather / 2.5))
            .cropped(to: canvas)
        out = front.applyingFilter("CIBlendWithMask", parameters: [
            kCIInputBackgroundImageKey: backdrop, kCIInputMaskImageKey: mask,
        ])
    }
    out = out.cropped(to: canvas)

    // Alfa-sız PNG.
    guard let cg = ci.createCGImage(out, from: canvas, format: .RGBA8, colorSpace: sRGB),
          let ctx = CGContext(data: nil, width: Int(spec.width), height: Int(spec.height),
                              bitsPerComponent: 8, bytesPerRow: 0, space: sRGB,
                              bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)
    else { fatalError("render") }
    ctx.draw(cg, in: canvas)
    let url = outDir.appendingPathComponent(spec.name + ".png")
    guard let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)
    else { fatalError("yazıla bilmədi: \(url.path)") }
    CGImageDestinationAddImage(dest, ctx.makeImage()!, nil)
    CGImageDestinationFinalize(dest)
    let crop = max(0, (front.extent.height - spec.height) / 2)
    let pad = max(0, (spec.height - front.extent.height) / 2)
    print("✓ \(url.lastPathComponent)  banner \(Int(front.extent.width))×\(Int(front.extent.height)), " +
          "kəsilən \(Int(crop)) px, zolaq \(Int(pad)) px (üst/alt)")
}

specs.forEach(render)
