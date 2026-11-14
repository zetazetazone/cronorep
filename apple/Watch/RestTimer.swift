import SwiftUI
import UserNotifications
import WatchKit

/// Timer state, haptics and the free trial clock.
@MainActor
final class RestTimer: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    static let presets = [30, 60, 90, 120]
    static let step = 5
    static let minRest = 10
    static let maxRest = 600
    static let trialLimit: TimeInterval = 10 * 60 * 60

    private enum Keys {
        static let rest = "rest_seconds"
        static let trialUsed = "trial_used_seconds"
    }

    private static let notificationID = "rest-end"

    /// Configured rest time.
    @Published private(set) var restSeconds: Int
    /// Time of the last tap. Nil = not started.
    @Published private(set) var startedAt: Date?
    /// Rest time of the current run. A settings change applies on the next tap.
    @Published private(set) var runSeconds: Int
    /// Free trial time used. It counts only with the app on screen and a started timer.
    @Published private(set) var trialUsed: TimeInterval

    private let defaults = UserDefaults.standard
    private var hapticTasks: [Task<Void, Never>] = []
    private var session: WKExtendedRuntimeSession?
    private var countingSince: Date?
    private var askedForNotifications = false

    override init() {
        let saved = UserDefaults.standard.integer(forKey: Keys.rest)
        let rest = saved == 0 ? 90 : min(max(saved, Self.minRest), Self.maxRest)
        restSeconds = rest
        runSeconds = rest
        trialUsed = UserDefaults.standard.double(forKey: Keys.trialUsed)
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }

    var trialExpired: Bool { trialUsed >= Self.trialLimit }

    /// Example: "9 h 58 min" or "42 min".
    var trialRemainingText: String {
        let minutes = Int((max(Self.trialLimit - trialUsed, 0) / 60).rounded(.up))
        return minutes >= 60 ? "\(minutes / 60) h \(minutes % 60) min" : "\(minutes) min"
    }

    // MARK: Settings

    func setRest(_ seconds: Int) {
        restSeconds = min(max(seconds, Self.minRest), Self.maxRest)
        defaults.set(restSeconds, forKey: Keys.rest)
    }

    func changeRest(by delta: Int) {
        setRest(restSeconds + delta)
    }

    // MARK: Timer

    func restart() {
        let now = Date()
        runSeconds = restSeconds
        startedAt = now
        startCounting()

        // Short tick: the tap worked.
        WKInterfaceDevice.current().play(.click)

        let total = TimeInterval(runSeconds)
        scheduleHaptics(end: now.addingTimeInterval(total))
        scheduleNotification(after: total)
        startSession()
    }

    /// Soft ticks at 3, 2 and 1 seconds before the end. Then the strong haptic.
    private func scheduleHaptics(end: Date) {
        hapticTasks.forEach { $0.cancel() }
        hapticTasks = []
        for secondsLeft in 0...3 {
            let delay = end.addingTimeInterval(-TimeInterval(secondsLeft)).timeIntervalSinceNow
            guard delay > 0 else { continue }
            let task = Task { [weak self] in
                try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                guard !Task.isCancelled else { return }
                self?.playHaptic(isEnd: secondsLeft == 0)
            }
            hapticTasks.append(task)
        }
    }

    private func playHaptic(isEnd: Bool) {
        WKInterfaceDevice.current().play(isEnd ? .notification : .click)
        if isEnd {
            session?.invalidate()
            session = nil
        }
    }

    /// The session keeps the app running when the wrist is down.
    private func startSession() {
        session?.invalidate()
        let newSession = WKExtendedRuntimeSession()
        newSession.start()
        session = newSession
    }

    /// Backup for the end haptic: the system delivers it when the app is not running.
    private func scheduleNotification(after seconds: TimeInterval) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [Self.notificationID])

        let content = UNMutableNotificationContent()
        content.title = "Rest is over"
        content.body = "Start your next set."
        content.sound = .default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(seconds, 1), repeats: false)
        let request = UNNotificationRequest(identifier: Self.notificationID, content: content, trigger: trigger)

        if askedForNotifications {
            center.add(request)
        } else {
            askedForNotifications = true
            center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
                if granted { center.add(request) }
            }
        }
    }

    /// App on screen: the app plays its own haptic, so the banner stays hidden.
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([])
    }

    // MARK: Free trial clock

    func sceneChanged(isActive: Bool) {
        if isActive {
            if startedAt != nil { startCounting() }
        } else {
            flushTrial()
            countingSince = nil
        }
    }

    private func startCounting() {
        if countingSince == nil { countingSince = Date() }
    }

    /// Adds the time since the last flush to the total and saves it.
    func flushTrial() {
        guard let since = countingSince else { return }
        let now = Date()
        trialUsed += now.timeIntervalSince(since)
        countingSince = now
        defaults.set(trialUsed, forKey: Keys.trialUsed)
    }
}
