import Foundation
import CoreData
import os

// MARK: - PersistenceController

final class PersistenceController {

    static let shared = PersistenceController()

    private let log = Logger(subsystem: "OnomatopoeiaDetector", category: "Persistence")
    let container: NSPersistentContainer

    init(inMemory: Bool = false) {
        container = NSPersistentContainer(name: "OnomatopoeiaDetector")
        if inMemory {
            container.persistentStoreDescriptions.first?.url = URL(fileURLWithPath: "/dev/null")
        }
        container.loadPersistentStores { _, error in
            if let error {
                fatalError("Core Data error: \(error)")
            }
        }
        container.viewContext.automaticallyMergesChangesFromParent = true
    }

    var viewContext: NSManagedObjectContext { container.viewContext }

    // MARK: - Save

    func save() {
        let ctx = viewContext
        guard ctx.hasChanges else { return }
        do {
            try ctx.save()
        } catch {
            log.error("Core Data save failed: \(error.localizedDescription, privacy: .public)")
        }
    }

    // MARK: - CRUD

    func addHistory(inputText: String, score: Int) {
        let item = HistoryEntity(context: viewContext)
        item.id = UUID()
        item.inputText = inputText
        item.score = Int16(score)
        item.date = Date()
        save()
        pruneIfNeeded()
    }

    func fetchHistory() -> [HistoryItem] {
        let request = HistoryEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(key: "date", ascending: false)]
        request.fetchLimit = 100
        let entities = (try? viewContext.fetch(request)) ?? []
        return entities.map {
            HistoryItem(
                id: $0.id ?? UUID(),
                inputText: $0.inputText ?? "",
                score: Int($0.score),
                date: $0.date ?? Date()
            )
        }
    }

    func delete(item: HistoryItem) {
        let request = HistoryEntity.fetchRequest()
        request.predicate = NSPredicate(format: "id == %@", item.id as CVarArg)
        if let entity = (try? viewContext.fetch(request))?.first {
            viewContext.delete(entity)
            save()
        }
    }

    func deleteAll() {
        let request: NSFetchRequest<NSFetchRequestResult> = HistoryEntity.fetchRequest()
        let batch = NSBatchDeleteRequest(fetchRequest: request)
        do {
            try viewContext.execute(batch)
        } catch {
            log.error("Core Data batch delete failed: \(error.localizedDescription, privacy: .public)")
        }
        save()
    }

    private func pruneIfNeeded() {
        let request = HistoryEntity.fetchRequest()
        request.sortDescriptors = [NSSortDescriptor(key: "date", ascending: false)]
        guard let all = try? viewContext.fetch(request), all.count > 100 else { return }
        all[100...].forEach { viewContext.delete($0) }
        save()
    }
}
