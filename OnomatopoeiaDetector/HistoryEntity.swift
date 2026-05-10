import Foundation
import CoreData

@objc(HistoryEntity)
public class HistoryEntity: NSManagedObject {}

extension HistoryEntity {
    @nonobjc public class func fetchRequest() -> NSFetchRequest<HistoryEntity> {
        return NSFetchRequest<HistoryEntity>(entityName: "HistoryEntity")
    }

    @NSManaged public var id: UUID?
    @NSManaged public var inputText: String?
    @NSManaged public var score: Int16
    @NSManaged public var date: Date?
}
