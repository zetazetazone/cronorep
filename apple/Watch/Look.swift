import ImageIO
import SwiftUI

/// What is behind the timer.
enum BackgroundKind: String, CaseIterable {
    case black, color, gif

    var label: String {
        switch self {
        case .black: return "Black"
        case .color: return "Colour"
        case .gif: return "GIF"
        }
    }

    var next: BackgroundKind {
        switch self {
        case .black: return .color
        case .color: return .gif
        case .gif: return .black
        }
    }
}

/// A still background, from the top colour to the bottom colour.
struct LookGradient {
    let name: String
    let top: Color
    let bottom: Color

    /// The same list as the Wear OS app (shared/Gradients.kt).
    static let all = [
        LookGradient(name: "Ember", top: Color(hex: 0xF97316), bottom: Color(hex: 0x7F1D1D)),
        LookGradient(name: "Ocean", top: Color(hex: 0x06B6D4), bottom: Color(hex: 0x1E3A8A)),
        LookGradient(name: "Forest", top: Color(hex: 0x4ADE80), bottom: Color(hex: 0x064E3B)),
        LookGradient(name: "Violet", top: Color(hex: 0xA855F7), bottom: Color(hex: 0x312E81)),
        LookGradient(name: "Steel", top: Color(hex: 0x94A3B8), bottom: Color(hex: 0x0F172A)),
        LookGradient(name: "Sunset", top: Color(hex: 0xF472B6), bottom: Color(hex: 0x9A3412)),
    ]
}

/// How the timer looks: minimal mode, dim mode and the background.
@MainActor
final class Look: ObservableObject {
    private enum Keys {
        static let minimal = "look_minimal"
        static let dim = "look_dim"
        static let background = "look_background"
        static let gradient = "look_gradient"
    }

    /// The content shows at this opacity in dim mode. Dark pixels use less power on the OLED screen.
    static let dimOpacity = 0.45

    /// No progress ring and no "total" line.
    @Published var minimal: Bool { didSet { save() } }
    /// Dimmer text, a black background and one screen update each second.
    @Published var dim: Bool { didSet { save(); backgroundChanged() } }
    @Published var background: BackgroundKind { didSet { save(); backgroundChanged() } }
    /// Index in `LookGradient.all`.
    @Published var gradientIndex: Int { didSet { save() } }

    /// The GIF that shows now. Nil = no GIF (off, loading or no network).
    @Published private(set) var gif: AnimatedGif?

    private let defaults = UserDefaults.standard
    private var gifID: String?
    private var gifTask: Task<Void, Never>?

    init() {
        let defaults = UserDefaults.standard
        minimal = defaults.bool(forKey: Keys.minimal)
        dim = defaults.bool(forKey: Keys.dim)
        background = BackgroundKind(rawValue: defaults.string(forKey: Keys.background) ?? "") ?? .black
        let index = defaults.integer(forKey: Keys.gradient)
        gradientIndex = LookGradient.all.indices.contains(index) ? index : 0
        if effectiveBackground == .gif { showRandomGif() }
    }

    /// The background to draw. Dim mode always draws black.
    var effectiveBackground: BackgroundKind { dim ? .black : background }

    var gradient: LookGradient { LookGradient.all[gradientIndex] }

    func nextGradient() {
        gradientIndex = (gradientIndex + 1) % LookGradient.all.count
    }

    /// Call at each timer start. The GIF background then shows a different GIF.
    func timerStarted() {
        if effectiveBackground == .gif { showRandomGif() }
    }

    private func save() {
        defaults.set(minimal, forKey: Keys.minimal)
        defaults.set(dim, forKey: Keys.dim)
        defaults.set(background.rawValue, forKey: Keys.background)
        defaults.set(gradientIndex, forKey: Keys.gradient)
    }

    private func backgroundChanged() {
        if effectiveBackground == .gif {
            // Show a GIF at once. The user must not wait for the next tap.
            if gifID == nil { showRandomGif() }
        } else {
            gifTask?.cancel()
            gifTask = nil
            gifID = nil
            gif = nil
        }
    }

    /// Picks a random GIF that is not the current one and loads it.
    private func showRandomGif() {
        guard let id = GifCatalog.ids.filter({ $0 != gifID }).randomElement() else { return }
        gifID = id
        gifTask?.cancel()
        gifTask = Task { [weak self] in
            let loaded = await AnimatedGif.load(from: GifCatalog.url(id))
            guard !Task.isCancelled, let self, self.gifID == id else { return }
            // A failed load keeps the last GIF. No GIF at all shows black.
            if let loaded { self.gif = loaded }
        }
    }
}

/// The gym GIFs. The same list as the Wear OS app (shared/GifCatalog.kt).
enum GifCatalog {
    static let ids = [
        "HSBsApUGF4erUp7NHV", "hWddcO7xQWCqIew5rH", "6uPX6ZNoCiPNxVmAUB", "7kbEhOfZzxNPTerRRz",
        "lOqNS2HUyN8OJV0CuF", "H4TsgO30w3qZZeMmlL", "WbsQEnBiIWKIM", "cuXLiXhdgOuJi",
        "3oriNZNU6VDrDxM7sI", "gElZT8LKDPJeg", "zy5evPW7VPCKI", "TyuwvnAzEPVFC",
        "0py1N0feFG64kbG2r3", "kzqdxYKp2yy31ROpUM", "39lwENv9kD5nO", "ehsEjVlJeXiqBqEpGc",
        "oShObTfbg3S5G", "0YvEGKIctG6nNOwdLd", "FSO0qBQ9Dwno4", "XMJb1k3GuouMfXSPIJ",
        "VIMDNQUn0rQ0L4ojIr", "bdpIDWZFkI67S", "XDSJtVcyGNXXrcCDnr", "VRGeARH0HUgr6",
        "Nf9kT8LvdkkStAMGEp", "3oz8xDnHOUs1Zmgcyk", "Y7O3LHmhllEk", "KHhaSdqhakNxa884ea",
        "3o6Ztq2Ed2cSSwAg8M", "xUPGcKoAYCn5fHK0Zq", "7ueSsRAZsOvDST7Q8p", "nFyfPbGijyKMZrF65y",
        "shuybsYkLzyUomNI17", "IuZ47lDMjN9yXx4C41", "h6M6Dfp5eXCjoeBzJC", "HQ9dS2mcEiqYw",
        "pPZ0wbiNbwJMDEzFxm", "YpZh61QjwuXq15FhV7", "KxKU0W6iBy5M88MwnY", "tskVbKESeJPSpVgf57",
        "wVKWhHbI3afhx3MW1e", "VFDeGtRSHswfe", "NJQubFbs35Vug",
    ]

    /// 200 px high: sharp enough for the background and a small download.
    static func url(_ id: String) -> URL {
        URL(string: "https://media.giphy.com/media/\(id)/200.gif")!
    }
}

/// The frames of a GIF, ready to draw.
struct AnimatedGif {
    let frames: [CGImage]
    /// The time where each frame starts, in seconds from the loop start.
    let starts: [TimeInterval]
    let duration: TimeInterval

    /// Watch memory is small: a frame limit and a size limit keep the GIF small.
    private static let maxFrames = 60
    private static let maxPixelSize = 240

    func frame(at time: TimeInterval) -> CGImage {
        let t = time.truncatingRemainder(dividingBy: duration)
        let index = (starts.lastIndex { $0 <= t }) ?? 0
        return frames[index]
    }

    static func load(from url: URL) async -> AnimatedGif? {
        guard let result = try? await URLSession.shared.data(from: url),
              (result.1 as? HTTPURLResponse)?.statusCode == 200 else { return nil }
        let data = result.0
        return await Task.detached(priority: .utility) { decode(data) }.value
    }

    private static func decode(_ data: Data) -> AnimatedGif? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        let count = min(CGImageSourceGetCount(source), maxFrames)
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: maxPixelSize,
        ]
        var frames: [CGImage] = []
        var starts: [TimeInterval] = []
        var time: TimeInterval = 0
        for index in 0..<count {
            guard let image = CGImageSourceCreateThumbnailAtIndex(source, index, options as CFDictionary) else { continue }
            frames.append(image)
            starts.append(time)
            time += frameDelay(source, index)
        }
        guard !frames.isEmpty, time > 0 else { return nil }
        return AnimatedGif(frames: frames, starts: starts, duration: time)
    }

    /// Browsers show a delay under 0.02 s as 0.1 s. This app does the same.
    private static func frameDelay(_ source: CGImageSource, _ index: Int) -> TimeInterval {
        let properties = CGImageSourceCopyPropertiesAtIndex(source, index, nil) as? [CFString: Any]
        let gif = properties?[kCGImagePropertyGIFDictionary] as? [CFString: Any]
        let delay = (gif?[kCGImagePropertyGIFUnclampedDelayTime] as? Double)
            ?? (gif?[kCGImagePropertyGIFDelayTime] as? Double)
            ?? 0.1
        return delay < 0.02 ? 0.1 : delay
    }
}

/// Draws the chosen background behind the timer.
struct LookBackground: View {
    @EnvironmentObject private var look: Look

    var body: some View {
        ZStack {
            Color.black
            switch look.effectiveBackground {
            case .black:
                EmptyView()
            case .color:
                LinearGradient(colors: [look.gradient.top, look.gradient.bottom], startPoint: .top, endPoint: .bottom)
                DarkLayer()
            case .gif:
                if let gif = look.gif {
                    GifView(gif: gif)
                    DarkLayer()
                }
            }
        }
        .ignoresSafeArea()
    }
}

/// The timer text must stay easy to read on each background.
private struct DarkLayer: View {
    var body: some View {
        Color.black.opacity(0.55)
    }
}

private struct GifView: View {
    let gif: AnimatedGif
    @State private var start = Date()

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 15)) { context in
            Image(decorative: gif.frame(at: context.date.timeIntervalSince(start)), scale: 1)
                .resizable()
                .scaledToFill()
        }
        .clipped()
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}
