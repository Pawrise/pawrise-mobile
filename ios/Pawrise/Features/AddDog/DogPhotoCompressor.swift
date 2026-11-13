import UIKit

/// Réduit une photo de la photothèque en JPEG léger pour le profil du chien.
enum DogPhotoCompressor {
    static let maxBytes = 300_000
    static let maxDimension: CGFloat = 1024

    /// Redimensionne (côté max 1024 px) puis baisse la qualité jusqu’à passer sous ~300 Ko.
    static func jpeg(from data: Data, maxBytes: Int = maxBytes) -> Data? {
        guard let image = UIImage(data: data) else { return nil }
        var dimension = maxDimension
        while dimension >= 256 {
            let resized = resize(image, maxDimension: dimension)
            for quality in stride(from: 0.8, through: 0.4, by: -0.1) {
                if let jpeg = resized.jpegData(compressionQuality: quality), jpeg.count <= maxBytes {
                    return jpeg
                }
            }
            dimension *= 0.75
        }
        return resize(image, maxDimension: 256).jpegData(compressionQuality: 0.4)
    }

    private static func resize(_ image: UIImage, maxDimension: CGFloat) -> UIImage {
        let size = image.size
        let scale = min(1, maxDimension / max(size.width, size.height))
        let target = CGSize(width: (size.width * scale).rounded(), height: (size.height * scale).rounded())
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = true
        return UIGraphicsImageRenderer(size: target, format: format).image { _ in
            image.draw(in: CGRect(origin: .zero, size: target))
        }
    }
}
