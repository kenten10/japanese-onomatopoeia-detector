import Foundation
import CoreData

// MARK: - PersistenceController

final class PersistenceController {

    static let shared = PersistenceController()

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
        if ctx.hasChanges {
            try? ctx.save()
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
        try? viewContext.execute(batch)
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
