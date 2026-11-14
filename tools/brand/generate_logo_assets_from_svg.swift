// Generates the derived AnaMuslim logo assets from the vector master (docs/brand/anamuslim-logo-vector-new.svg).
//
// usage (from the repo root):
//   /Applications/Inkscape.app/Contents/MacOS/inkscape docs/brand/anamuslim-logo-vector-new.svg \
//       --export-type=png --export-width=2000 --export-background-opacity=0 --export-filename=/tmp/logo-2000.png
//   swift tools/brand/generate_logo_assets_from_svg.swift /tmp/logo-2000.png .
//
// Needs only macOS (CoreGraphics/ImageIO) — no pillow/numpy. Supersedes generate_logo_assets.py, which
// cut everything from the old raster master (docs/brand/anamuslim-logo-master.jpg).
//
// NOT written here (made in Android Studio → Image Asset from the same SVG, keep them as they are):
//   app/src/main/res/mipmap-*/ic_launcher*.webp, app/src/main/ic_launcher-playstore.png.
// The notification icon (drawable-*/ic_notification.png) is the cube silhouette; the shape did not
// change, so it is left alone.
//
// Writes:
//   iosApp/.../AppIcon.appiconset/AppIcon-1024.png   white, opaque (App Store rejects alpha), ink height 80 %
//   app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png      circular badge 432 px (splash layer-lists, Glance widgets)
//   shared/src/commonMain/composeResources/drawable/ic_launcher_foreground.png  badge 512 px (About, greeting, share cards)
//   iosApp/.../LaunchLogo.imageset/LaunchLogo{,@2x,@3x}.png          badge 256/512/768 px
//   docs/brand/anamuslim-logo-transparent.png                         the transparent 2000 px render itself

import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let args = CommandLine.arguments
guard args.count == 3 else { fatalError("usage: swift generate_logo_assets_from_svg.swift <logo-2000.png> <repo root>") }
let srcURL = URL(fileURLWithPath: args[1])
let root = URL(fileURLWithPath: args[2])

guard let isrc = CGImageSourceCreateWithURL(srcURL as CFURL, nil),
      let logo = CGImageSourceCreateImageAtIndex(isrc, 0, nil) else { fatalError("cannot read \(srcURL.path)") }
let W = logo.width, H = logo.height
let rgb = CGColorSpace(name: CGColorSpace.sRGB)!

// ---- ink geometry from the alpha channel ----------------------------------------------------------
let alpha: [UInt8] = {
    var buf = [UInt8](repeating: 0, count: W * H)
    let ctx = CGContext(data: &buf, width: W, height: H, bitsPerComponent: 8, bytesPerRow: W,
                        space: CGColorSpaceCreateDeviceGray(), bitmapInfo: CGImageAlphaInfo.alphaOnly.rawValue)!
    ctx.draw(logo, in: CGRect(x: 0, y: 0, width: W, height: H))
    return buf  // row 0 = top (alphaOnly bitmap is stored top-down like the source)
}()
var minX = W, minY = H, maxX = 0, maxY = 0
var pts: [(Double, Double)] = []
for y in 0..<H { for x in 0..<W where alpha[y * W + x] > 128 {
    minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y)
    if (x + y) % 5 == 0 { pts.append((Double(x), Double(y))) }
} }
let boxCX = Double(minX + maxX + 1) / 2, boxCY = Double(minY + maxY + 1) / 2
let inkH = Double(maxY - minY + 1)
// Round masks: centred horizontally, slid vertically halfway to where the farthest ink is closest
// (same rule as the old Python generator, so the badge keeps its proportions).
func farthest(_ cy: Double) -> Double { pts.reduce(0) { max($0, (($1.0 - boxCX) * ($1.0 - boxCX) + ($1.1 - cy) * ($1.1 - cy)).squareRoot()) } }
var bestY = boxCY, bestR = Double.infinity
for dy in stride(from: -160.0, through: 160.0, by: 2.0) { let r = farthest(boxCY + dy); if r < bestR { bestR = r; bestY = boxCY + dy } }
let roundCY = (bestY + boxCY) / 2
let inkR = farthest(roundCY)
print("ink box", minX, minY, maxX, maxY, "round centre y", roundCY, "ink radius", Int(inkR))

// ---- helpers ------------------------------------------------------------------------------------
func context(_ px: Int, opaque: Bool) -> CGContext {
    let info = opaque ? CGImageAlphaInfo.noneSkipLast.rawValue : CGImageAlphaInfo.premultipliedLast.rawValue
    let c = CGContext(data: nil, width: px, height: px, bitsPerComponent: 8, bytesPerRow: 0, space: rgb, bitmapInfo: info)!
    c.interpolationQuality = .high
    c.setShouldAntialias(true)
    // flip to top-left origin so the maths matches image coordinates
    c.translateBy(x: 0, y: CGFloat(px)); c.scaleBy(x: 1, y: -1)
    return c
}

/// Draws the logo so that source point (cx, cy) lands on (dx, dy) at `scale` (dest px per source px).
func drawLogo(_ c: CGContext, cx: Double, cy: Double, dx: Double, dy: Double, scale: Double) {
    c.saveGState()
    let rect = CGRect(x: dx - cx * scale, y: dy - cy * scale, width: Double(W) * scale, height: Double(H) * scale)
    // CGContext.draw expects bottom-up; undo the flip locally for the image
    c.translateBy(x: 0, y: rect.midY * 2); c.scaleBy(x: 1, y: -1)
    c.draw(logo, in: rect)
    c.restoreGState()
}

func write(_ c: CGContext, _ rel: String) {
    let url = root.appendingPathComponent(rel)
    try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
    let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(dest, c.makeImage()!, nil)
    guard CGImageDestinationFinalize(dest) else { fatalError("write failed \(rel)") }
    print("wrote", rel)
}

let white = CGColor(srgbRed: 1, green: 1, blue: 1, alpha: 1)
let gold = CGColor(srgbRed: 196 / 255, green: 152 / 255, blue: 70 / 255, alpha: 1)

// ---- 1. iOS App Store icon: white, opaque, ink height 80 % ---------------------------------------
do {
    let px = 1024
    let c = context(px, opaque: true)
    c.setFillColor(white); c.fill(CGRect(x: 0, y: 0, width: px, height: px))
    drawLogo(c, cx: boxCX, cy: boxCY, dx: Double(px) / 2, dy: Double(px) / 2, scale: Double(px) * 0.80 / inkH)
    write(c, "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png")
}

// ---- 2. circular badge: white disc + thin gold ring, transparent outside ---------------------------
func badge(_ px: Int, circleFrac: Double, _ rel: String) {
    let c = context(px, opaque: false)
    let d = Double(px) * circleFrac
    let o = (Double(px) - d) / 2
    let disc = CGRect(x: o, y: o, width: d, height: d)
    c.setFillColor(white); c.fillEllipse(in: disc)
    c.saveGState(); c.addEllipse(in: disc); c.clip()
    drawLogo(c, cx: boxCX, cy: roundCY, dx: Double(px) / 2, dy: Double(px) / 2, scale: d / 2 * 0.88 / inkR)
    c.restoreGState()
    let lw = max(1, d * 0.018)
    c.setStrokeColor(gold); c.setLineWidth(lw)
    c.strokeEllipse(in: disc.insetBy(dx: lw / 2, dy: lw / 2))
    write(c, rel)
}
badge(432, circleFrac: 0.94, "app/src/main/res/drawable-xxxhdpi/ic_launcher_foreground.png")
badge(512, circleFrac: 0.94, "shared/src/commonMain/composeResources/drawable/ic_launcher_foreground.png")
for (suf, px) in [("", 256), ("@2x", 512), ("@3x", 768)] {
    badge(px, circleFrac: 0.94, "iosApp/iosApp/Assets.xcassets/LaunchLogo.imageset/LaunchLogo\(suf).png")
}

// ---- 3. transparent master for docs ---------------------------------------------------------------
do {
    let dst = root.appendingPathComponent("docs/brand/anamuslim-logo-transparent.png")
    try? FileManager.default.removeItem(at: dst)
    try! FileManager.default.copyItem(at: srcURL, to: dst)
    print("wrote docs/brand/anamuslim-logo-transparent.png")
}
