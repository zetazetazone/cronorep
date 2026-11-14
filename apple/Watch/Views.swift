import SwiftUI

struct RootView: View {
    @EnvironmentObject private var timer: RestTimer
    @EnvironmentObject private var store: Store
    @Environment(\.scenePhase) private var scenePhase

    @State private var showSettings = false
    @State private var showPaywall = false

    var body: some View {
        TimerView()
            .contentShape(Rectangle())
            .onTapGesture {
                timer.flushTrial()
                if timer.trialExpired && !store.unlocked {
                    showPaywall = true
                } else {
                    timer.restart()
                }
            }
            .onLongPressGesture(minimumDuration: 0.6) {
                timer.flushTrial()
                showSettings = true
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
            }
            .sheet(isPresented: $showPaywall) {
                PaywallView()
            }
            .onChange(of: scenePhase) { _, phase in
                timer.sceneChanged(isActive: phase == .active)
            }
            .onChange(of: store.unlocked) { _, unlocked in
                if unlocked { showPaywall = false }
            }
    }
}

struct TimerView: View {
    @EnvironmentObject private var timer: RestTimer

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if let startedAt = timer.startedAt {
                TimelineView(.periodic(from: startedAt, by: 0.1)) { context in
                    RunningView(
                        elapsed: max(context.date.timeIntervalSince(startedAt), 0),
                        total: TimeInterval(timer.runSeconds)
                    )
                }
            } else {
                VStack(spacing: 4) {
                    Text("TAP TO START")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color.restGreen)
                    Text(formatTime(timer.restSeconds))
                        .font(.system(size: 40))
                    Text("Hold to set time")
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dim)
                }
            }
        }
    }
}

/// Counts down to the end of the rest. Then it counts up.
struct RunningView: View {
    let elapsed: TimeInterval
    let total: TimeInterval

    private var resting: Bool { elapsed < total }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.white.opacity(0.12), lineWidth: 6)
            Circle()
                .trim(from: 0, to: resting ? 1 - elapsed / total : 1)
                .stroke(
                    resting ? Color.restGreen : Color.goOrange,
                    style: StrokeStyle(lineWidth: 6, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))

            VStack(spacing: 0) {
                if resting {
                    Text("REST")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color.restGreen)
                    Text(formatTime(Int((total - elapsed).rounded(.up))))
                        .font(.system(size: 50))
                        .monospacedDigit()
                } else {
                    Text("GO")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color.goOrange)
                    Text("+" + formatTime(Int(elapsed - total)))
                        .font(.system(size: 44))
                        .monospacedDigit()
                        .foregroundStyle(Color.goOrange)
                }
                Text("total " + formatTime(Int(elapsed)))
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dim)
            }
        }
        .padding(4)
    }
}

struct SettingsView: View {
    @EnvironmentObject private var timer: RestTimer
    @EnvironmentObject private var store: Store
    @Environment(\.dismiss) private var dismiss

    private let columns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                Text("Rest time")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dim)

                HStack {
                    Button("-") { timer.changeRest(by: -RestTimer.step) }
                        .frame(width: 48)
                    Text(formatTime(timer.restSeconds))
                        .font(.system(size: 30))
                        .monospacedDigit()
                        .frame(maxWidth: .infinity)
                    Button("+") { timer.changeRest(by: RestTimer.step) }
                        .frame(width: 48)
                }

                Button("OK") { dismiss() }
                    .tint(Color.restGreen)

                Text(trialText)
                    .font(.system(size: 11))
                    .foregroundStyle(Color.dim)

                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(RestTimer.presets, id: \.self) { seconds in
                        Button(formatTime(seconds)) {
                            timer.setRest(seconds)
                            dismiss()
                        }
                        .tint(seconds == timer.restSeconds ? Color.restGreen : nil)
                    }
                }
            }
            .padding(.horizontal, 4)
        }
    }

    private var trialText: String {
        if store.unlocked { return "Unlocked. Thank you!" }
        if timer.trialExpired { return "Free trial ended" }
        return "Free trial: \(timer.trialRemainingText) left"
    }
}

struct PaywallView: View {
    @EnvironmentObject private var store: Store

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                Text("Free trial ended")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Color.goOrange)
                Text("Pay one time.\nUse it forever.")
                    .font(.system(size: 13))
                    .multilineTextAlignment(.center)

                Button(unlockTitle) {
                    Task { await store.buy() }
                }
                .tint(Color.restGreen)

                Button("Restore purchase") {
                    Task { await store.restore() }
                }
                .font(.system(size: 12))

                if let message = store.message {
                    Text(message)
                        .font(.system(size: 11))
                        .foregroundStyle(Color.dim)
                        .multilineTextAlignment(.center)
                }

                Text("Privacy: enautomatico.online/cronorep/privacy")
                    .font(.system(size: 9))
                    .foregroundStyle(Color.dim)
                    .multilineTextAlignment(.center)
            }
        }
    }

    private var unlockTitle: String {
        if let product = store.product { return "Unlock \(product.displayPrice)" }
        return "Unlock"
    }
}
