import SwiftUI

struct RootView: View {
    @EnvironmentObject private var timer: RestTimer
    @EnvironmentObject private var store: Store
    @EnvironmentObject private var look: Look
    @Environment(\.scenePhase) private var scenePhase

    @State private var showSettings = false
    @State private var showPaywall = false
    @State private var showIntro = false
    /// The trial terms show one time, before the first rest starts.
    @AppStorage("trial_intro_seen") private var introSeen = false

    var body: some View {
        TimerView()
            .contentShape(Rectangle())
            .onTapGesture {
                timer.flushTrial()
                if timer.trialExpired && !store.unlocked {
                    showPaywall = true
                } else {
                    timer.restart()
                    look.timerStarted()
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
            .sheet(isPresented: $showIntro, onDismiss: { introSeen = true }) {
                TrialIntroView()
            }
            .onAppear {
                if !introSeen && !store.unlocked { showIntro = true }
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
    @EnvironmentObject private var look: Look

    var body: some View {
        ZStack {
            LookBackground()
            content
                .opacity(look.dim ? Look.dimOpacity : 1)
        }
    }

    @ViewBuilder
    private var content: some View {
        if let startedAt = timer.startedAt {
            // Dim mode: one screen update each second, on the whole second since the tap,
            // so "REST 0:01" never stays on screen after the end haptic.
            TimelineView(.periodic(from: startedAt, by: look.dim ? 1 : 0.1)) { context in
                RunningView(
                    elapsed: max(context.date.timeIntervalSince(startedAt), 0),
                    total: TimeInterval(timer.runSeconds),
                    minimal: look.minimal
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

/// Counts down to the end of the rest. Then it counts up.
struct RunningView: View {
    let elapsed: TimeInterval
    let total: TimeInterval
    /// No progress ring and no "total" line.
    var minimal = false

    private var resting: Bool { elapsed < total }

    var body: some View {
        ZStack {
            if !minimal {
                Circle()
                    .stroke(Color.white.opacity(0.12), lineWidth: 6)
                Circle()
                    .trim(from: 0, to: resting ? 1 - elapsed / total : 1)
                    .stroke(
                        resting ? Color.restGreen : Color.goOrange,
                        style: StrokeStyle(lineWidth: 6, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
            }

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
                if !minimal {
                    Text("total " + formatTime(Int(elapsed)))
                        .font(.system(size: 12))
                        .foregroundStyle(Color.dim)
                }
            }
        }
        .padding(4)
    }
}

struct SettingsView: View {
    @EnvironmentObject private var timer: RestTimer
    @EnvironmentObject private var store: Store
    @EnvironmentObject private var look: Look
    @Environment(\.dismiss) private var dismiss
    @State private var showPaywall = false

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

                if !store.unlocked {
                    Button("Unlock now") { showPaywall = true }
                        .font(.system(size: 12))
                }

                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(RestTimer.presets, id: \.self) { seconds in
                        Button(formatTime(seconds)) {
                            timer.setRest(seconds)
                            dismiss()
                        }
                        .tint(seconds == timer.restSeconds ? Color.restGreen : nil)
                    }
                }

                Text("Look")
                    .font(.system(size: 12))
                    .foregroundStyle(Color.dim)
                    .padding(.top, 6)

                LookButton(title: look.minimal ? "Minimal on" : "Minimal off", active: look.minimal) {
                    look.minimal.toggle()
                }
                LookButton(title: look.dim ? "Dim screen on" : "Dim screen off", active: look.dim) {
                    look.dim.toggle()
                }
                LookButton(title: "Back: \(look.background.label)", active: look.background != .black) {
                    look.background = look.background.next
                }
                if look.background == .color {
                    LookButton(title: "Colour: \(look.gradient.name)", active: true) {
                        look.nextGradient()
                    }
                }
                if look.dim && look.background != .black {
                    Text("Dim screen shows a black background.")
                        .font(.system(size: 10))
                        .foregroundStyle(Color.dim)
                        .multilineTextAlignment(.center)
                }
            }
            .padding(.horizontal, 4)
        }
        .sheet(isPresented: $showPaywall) {
            PaywallView()
        }
        .onChange(of: store.unlocked) { _, unlocked in
            if unlocked { showPaywall = false }
        }
    }

    private var trialText: String {
        if store.unlocked { return "Unlocked. Thank you!" }
        if timer.trialExpired { return "Free trial ended" }
        return "Free trial: \(timer.trialRemainingText) left"
    }
}

/// A look setting. Orange = on.
private struct LookButton: View {
    let title: String
    let active: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 13, weight: .bold))
                .foregroundStyle(active ? Color.goOrange : Color.white)
        }
        .tint(active ? Color.goOrange : nil)
    }
}

/// Shown one time before the first rest: the trial length and the price.
struct TrialIntroView: View {
    @EnvironmentObject private var store: Store
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                Text("10 hours free")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Color.restGreen)
                Text("The free trial counts only while a rest timer runs with the app on screen.")
                    .font(.system(size: 12))
                    .multilineTextAlignment(.center)
                Text(priceText)
                    .font(.system(size: 12))
                    .multilineTextAlignment(.center)
                Button("OK") { dismiss() }
                    .tint(Color.restGreen)
            }
        }
    }

    private var priceText: String {
        let price = store.product?.displayPrice ?? "a one-time price"
        return "After that, unlock CronoRep for \(price), paid one time. No subscription."
    }
}

struct PaywallView: View {
    @EnvironmentObject private var store: Store
    @EnvironmentObject private var timer: RestTimer

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                Text(timer.trialExpired ? "Free trial ended" : "Unlock CronoRep")
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
