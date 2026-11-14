import StoreKit
import SwiftUI

/// One-time "lifetime unlock" purchase through the App Store (StoreKit 2).
@MainActor
final class Store: ObservableObject {
    /// Must match the product ID in App Store Connect.
    static let productID = "lifetime_unlock"

    private static let unlockedKey = "unlocked"

    @Published private(set) var unlocked: Bool
    @Published private(set) var product: Product?
    /// Short status for the paywall. Nil = nothing to say.
    @Published private(set) var message: String?

    private var updates: Task<Void, Never>?

    init() {
        unlocked = UserDefaults.standard.bool(forKey: Self.unlockedKey)
        // Purchases, refunds and "Ask to Buy" approvals arrive here.
        updates = Task { [weak self] in
            for await result in Transaction.updates {
                if case .verified(let transaction) = result {
                    await transaction.finish()
                }
                await self?.refresh()
            }
        }
        Task { await refresh() }
    }

    deinit {
        updates?.cancel()
    }

    /// Asks the App Store again for the product and the purchase.
    func refresh() async {
        if product == nil {
            product = try? await Product.products(for: [Self.productID]).first
        }
        var owned = false
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result,
               transaction.productID == Self.productID,
               transaction.revocationDate == nil {
                owned = true
            }
        }
        saveUnlocked(owned)
    }

    func buy() async {
        guard let product else {
            message = "Store not ready. Try again."
            await refresh()
            return
        }
        message = nil
        do {
            switch try await product.purchase() {
            case .success(let verification):
                if case .verified(let transaction) = verification {
                    await transaction.finish()
                    saveUnlocked(true)
                } else {
                    message = "Payment not verified."
                }
            case .pending:
                message = "Payment pending."
            case .userCancelled:
                break
            @unknown default:
                break
            }
        } catch {
            message = "Payment failed. Try again."
        }
    }

    /// Apple requires a restore button for one-time purchases.
    func restore() async {
        message = nil
        try? await AppStore.sync()
        await refresh()
        if !unlocked { message = "No purchase found." }
    }

    private func saveUnlocked(_ value: Bool) {
        unlocked = value
        UserDefaults.standard.set(value, forKey: Self.unlockedKey)
    }
}
