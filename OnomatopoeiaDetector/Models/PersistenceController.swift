import Foundation
import CoreData
import os

// MARK: - PersistenceController

final class PersistenceController {

    static let shared = PersistenceController()

    private let log = Logger(subsystem: "OnomatopoeiaDetector", category: "Persistence")
    let container: NSPersistentContainer
    private var storeLoadError: Error?

    init(inMemory: Bool = false) {
        container = NSPersistentContainer(name: "OnomatopoeiaDetector")
        if inMemory {
            container.persistentStoreDescriptions.first?.url = URL(fileURLWithPath: "/dev/null")
        }
        container.persistentStoreDescriptions.forEach { description in
            description.shouldMigrateStoreAutomatically = true
            description.shouldInferMappingModelAutomatically = true
        }
        container.loadPersistentStores { _, error in
            if let error {
                self.storeLoadError = error
                self.log.fault("Core Data store load failed: \(error.localizedDescription, privacy: .public)")
            }
        }
        container.viewContext.automaticallyMergesChangesFromParent = true
    }

    var viewContext: NSManagedObjectContext { container.viewContext }

    // MARK: - Save

    func save() throws {
        try ensureStoreIsAvailable()
        let ctx = viewContext
        guard ctx.hasChanges else { return }
        do {
            try ctx.save()
        } catch {
            log.error("Core Data save failed: \(error.localizedDescription, privacy: .public)")
            throw PersistenceError.saveFailed(error)
        }
    }

    // MARK: - CRUD

    func addHistory(inputText: String, score: Int) throws {
        try ensureStoreIsAvailable()
        let item = HistoryEntity(context: viewContext)
        item.id = UUID()
        item.inputText = inputText
        item.score = Int16(score)
        item.date = Date()
        try save()
        try pruneIfNeeded()
    }

    func fetchHistory() throws -> [HistoryItem] {
        try ensureStoreIsAvailable()
        let request = HistoryEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(key: "date", ascending: false)]
        request.fetchLimit = 100
        let entities: [HistoryEntity]
        do {
            entities = try viewContext.fetch(request)
        } catch {
            log.error("Core Data fetch failed: \(error.localizedDescription, privacy: .public)")
            throw PersistenceError.loadFailed(error)
        }
        return entities.map {
            HistoryItem(
                id: $0.id ?? UUID(),
                inputText: $0.inputText ?? "",
                score: Int($0.score),
                date: $0.date ?? Date()
            )
        }
    }

    func delete(item: HistoryItem) throws {
        try ensureStoreIsAvailable()
        let request = HistoryEntity.fetchRequest()
        request.predicate = NSPredicate(format: "id == %@", item.id as CVarArg)
        do {
            guard let entity = try viewContext.fetch(request).first else { return }
            viewContext.delete(entity)
            try save()
        } catch let error as PersistenceError {
            throw error
        } catch {
            log.error("Core Data delete failed: \(error.localizedDescription, privacy: .public)")
            throw PersistenceError.deleteFailed(error)
        }
    }

    func deleteAll() throws {
        try ensureStoreIsAvailable()
        let request: NSFetchRequest<NSFetchRequestResult> = HistoryEntity.fetchRequest()
        let batch = NSBatchDeleteRequest(fetchRequest: request)
        do {
            try viewContext.execute(batch)
            viewContext.reset()
        } catch {
            log.error("Core Data batch delete failed: \(error.localizedDescription, privacy: .public)")
            throw PersistenceError.deleteFailed(error)
        }
    }

    private func pruneIfNeeded() throws {
        let request = HistoryEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(key: "date", ascending: false)]
        let all = try viewContext.fetch(request)
        guard all.count > 100 else { return }
        all[100...].forEach { viewContext.delete($0) }
        try save()
    }

    private func ensureStoreIsAvailable() throws {
        if let storeLoadError {
            throw PersistenceError.storeUnavailable(storeLoadError)
        }
    }
}

enum PersistenceError: LocalizedError {
    case storeUnavailable(Error)
    case loadFailed(Error)
    case saveFailed(Error)
    case deleteFailed(Error)

    var errorDescription: String? {
        switch self {
        case .storeUnavailable, .loadFailed:
            return String(localized: "error.persistence.load")
        case .saveFailed:
            return String(localized: "error.persistence.save")
        case .deleteFailed:
            return String(localized: "error.persistence.delete")
        }
    }
}
