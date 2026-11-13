import Foundation
import Testing
@testable import Pawrise

@MainActor
@Suite("Race : recherche dans le référentiel de l'API-CORE")
struct BreedSearchTests {
    @Test func sansSaisieLaListeDeLaMaquetteRestePropose() {
        let model = AddDogFlowModel()
        model.breedSearch = { _ in ["Ne doit pas servir"] }
        #expect(model.breedSuggestions == DogCatalog.breeds)
    }

    @Test func laSaisieAfficheLesRacesDuServeur() async throws {
        let model = AddDogFlowModel()
        model.breedSearch = { query in query == "malinois" ? ["Berger belge Malinois"] : [] }
        model.breedQuery = "malinois"
        model.breedQueryDidChange()
        try await Task.sleep(for: .milliseconds(600))
        #expect(model.breedSuggestions == ["Berger belge Malinois"])
    }

    @Test func sansReponseDuServeurLaListeEmbarqueeSert() async throws {
        let model = AddDogFlowModel()
        model.breedSearch = { _ in nil }
        model.breedQuery = "golden"
        model.breedQueryDidChange()
        try await Task.sleep(for: .milliseconds(600))
        #expect(model.breedSuggestions == ["Golden Retriever"])
    }
}
