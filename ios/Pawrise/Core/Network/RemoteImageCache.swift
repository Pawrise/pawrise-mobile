import SwiftUI
import UIKit

/// Images distantes (photo du chien `photoUrl`) : cache mémoire décodé, plus cache HTTP disque partagé.
/// Une photo déjà vue s'affiche tout de suite, sans repasser par le réseau ni clignoter.
final class RemoteImageCache: Sendable {
    static let shared = RemoteImageCache()

    // NSCache est sûr entre threads ; URLSession aussi.
    nonisolated(unsafe) private let images = NSCache<NSURL, UIImage>()
    private let session: URLSession

    init(session: URLSession? = nil) {
        images.countLimit = 64
        if let session {
            self.session = session
        } else {
            let configuration = URLSessionConfiguration.default
            configuration.urlCache = URLCache(memoryCapacity: 8 * 1024 * 1024, diskCapacity: 64 * 1024 * 1024)
            configuration.requestCachePolicy = .returnCacheDataElseLoad
            self.session = URLSession(configuration: configuration)
        }
    }

    func cachedImage(for url: URL) -> UIImage? {
        images.object(forKey: url as NSURL)
    }

    func image(for url: URL) async -> UIImage? {
        if let cached = cachedImage(for: url) { return cached }
        guard let (data, response) = try? await session.data(from: url),
              (response as? HTTPURLResponse).map({ (200..<300).contains($0.statusCode) }) ?? true,
              let image = UIImage(data: data) else { return nil }
        images.setObject(image, forKey: url as NSURL)
        return image
    }
}

/// Équivalent d'`AsyncImage` adossé à `RemoteImageCache`.
struct CachedRemoteImage<Content: View, Placeholder: View>: View {
    let url: URL
    @ViewBuilder let content: (Image) -> Content
    @ViewBuilder let placeholder: () -> Placeholder

    @State private var loaded: (url: URL, image: UIImage)?

    var body: some View {
        Group {
            if let image = currentImage {
                content(Image(uiImage: image))
            } else {
                placeholder()
            }
        }
        .task(id: url) {
            guard currentImage == nil, let image = await RemoteImageCache.shared.image(for: url) else { return }
            loaded = (url, image)
        }
    }

    private var currentImage: UIImage? {
        if let loaded, loaded.url == url { return loaded.image }
        return RemoteImageCache.shared.cachedImage(for: url)
    }
}
