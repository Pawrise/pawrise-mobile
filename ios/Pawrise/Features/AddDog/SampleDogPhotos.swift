import UIKit

/// Photos d’exemple de la maquette (A08 « ou choisissez un exemple »), embarquées dans l’app :
/// elles s’affichent sans réseau et sont enregistrées comme une photo locale du chien.
enum SampleDogPhotos {
    static let all: [Data] = (1...4).compactMap { NSDataAsset(name: "Onboarding/dog_\($0)")?.data }
}
