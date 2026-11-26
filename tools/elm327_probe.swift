// A tiny ELM327 probe for macOS, over classic Bluetooth (SPP/RFCOMM).
//
// The adapter has to be paired with this Mac first (System Settings → Bluetooth, PIN 1234 or
// 0000), and nothing else may be connected to it: an ELM327 takes one connection at a time.
// macOS asks for Bluetooth permission the first time; allow it.
//
// Build and run:
//   xcrun swiftc -O tools/elm327_probe.swift -o /tmp/elm327_probe -framework IOBluetooth
//   /tmp/elm327_probe list
//   /tmp/elm327_probe probe OBD            # part of the paired device's name
//   /tmp/elm327_probe probe OBD --scan     # + walk the engine module's 0x1100…0x11FF
//
// Read-only: AT commands, mode 01 and UDS service 22. Nothing is written, coded or cleared.

import Foundation
import IOBluetooth

final class Channel: NSObject, IOBluetoothRFCOMMChannelDelegate {
    var channel: IOBluetoothRFCOMMChannel?
    var opened = false
    var closed = false
    var buffer = Data()

    func open(_ device: IOBluetoothDevice) -> Bool {
        for id in UInt8(1)...UInt8(8) {
            var ch: IOBluetoothRFCOMMChannel?
            if device.openRFCOMMChannelSync(&ch, withChannelID: id, delegate: self) == kIOReturnSuccess, let ch {
                channel = ch
                return true
            }
        }
        return false
    }

    func rfcommChannelOpenComplete(_ rfcommChannel: IOBluetoothRFCOMMChannel!, status: IOReturn) {
        opened = status == kIOReturnSuccess
    }

    func rfcommChannelData(_ rfcommChannel: IOBluetoothRFCOMMChannel!, data dataPointer: UnsafeMutableRawPointer!, length dataLength: Int) {
        buffer.append(Data(bytes: dataPointer, count: dataLength))
    }

    func rfcommChannelClosed(_ rfcommChannel: IOBluetoothRFCOMMChannel!) {
        closed = true
    }

    func send(_ command: String, timeout: TimeInterval) -> String {
        buffer.removeAll()
        let data = Data((command + "\r").utf8)
        data.withUnsafeBytes { raw in
            _ = channel?.writeSync(UnsafeMutableRawPointer(mutating: raw.baseAddress), length: UInt16(data.count))
        }
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.02))
            if let text = String(data: buffer, encoding: .ascii), text.contains(">") { return text }
        }
        return String(data: buffer, encoding: .ascii) ?? ""
    }

    func close() {
        channel?.close()
        channel = nil
    }
}

func pairedDevices() -> [IOBluetoothDevice] {
    (IOBluetoothDevice.pairedDevices() as? [IOBluetoothDevice]) ?? []
}

func tidy(_ answer: String) -> String {
    answer
        .replacingOccurrences(of: ">", with: "")
        .split(whereSeparator: { $0 == "\r" || $0 == "\n" })
        .map { $0.trimmingCharacters(in: .whitespaces) }
        .filter { !$0.isEmpty && !$0.hasPrefix("SEARCHING") }
        .joined(separator: " | ")
}

func lines(_ answer: String) -> [String] {
    answer
        .replacingOccurrences(of: ">", with: "")
        .split(whereSeparator: { $0 == "\r" || $0 == "\n" })
        .map { $0.trimmingCharacters(in: .whitespaces) }
        .filter { !$0.isEmpty && !$0.hasPrefix("SEARCHING") }
}

/// The bytes of a `62 <did> …` answer, multi-frame "0:"/"1:" lines joined.
func udsBytes(_ answer: String, did: UInt16) -> [UInt8]? {
    let hex = lines(answer)
        .map { $0.replacingOccurrences(of: " ", with: "").replacingOccurrences(of: #"^\d+:"#, with: "", options: .regularExpression) }
        .filter { !$0.isEmpty && $0.allSatisfy { $0.isHexDigit } }
        .joined()
        .uppercased()
    let tag = String(format: "62%04X", did)
    guard let range = hex.range(of: tag) else { return nil }
    let rest = String(hex[range.upperBound...])
    let even = rest.count - rest.count % 2
    guard even > 0 else { return [] }
    var out = [UInt8]()
    var i = rest.startIndex
    while i < rest.index(rest.startIndex, offsetBy: even) {
        let next = rest.index(i, offsetBy: 2)
        if let b = UInt8(rest[i..<next], radix: 16) { out.append(b) }
        i = next
    }
    return out
}

func hexText(_ bytes: [UInt8]) -> String { bytes.map { String(format: "%02X", $0) }.joined(separator: " ") }

func choose(_ wanted: String) -> IOBluetoothDevice? {
    let all = pairedDevices()
    let match = all.first { d in
        let name = (d.name ?? "").lowercased()
        let address = (d.addressString ?? "").lowercased()
        let want = wanted.lowercased()
        return name.contains(want) || address == want || address.replacingOccurrences(of: "-", with: ":") == want
    }
    return match
}

let args = CommandLine.arguments
let command = args.count > 1 ? args[1] : "list"

switch command {
case "list":
    let all = pairedDevices()
    if all.isEmpty { print("no paired devices") }
    for d in all {
        print("\(d.name ?? "?")  \(d.addressString ?? "?")  connected=\(d.isConnected())")
    }

case "probe":
    guard args.count > 2 else {
        print("usage: elm327_probe probe <part of the adapter's name or its address> [--scan]")
        exit(2)
    }
    guard let device = choose(args[2]) else {
        print("no paired device matching \"\(args[2])\"; run `list`")
        exit(1)
    }
    let scan = args.contains("--scan")
    print("connecting to \(device.name ?? "?") \(device.addressString ?? "")…")
    _ = device.openConnection()
    let link = Channel()
    guard link.open(device) else {
        print("could not open a serial (SPP) channel: is the adapter classic Bluetooth and paired?")
        exit(1)
    }
    let deadline = Date().addingTimeInterval(5)
    while !link.opened && Date() < deadline { RunLoop.current.run(mode: .default, before: Date().addingTimeInterval(0.05)) }
    print("channel open=\(link.opened)")

    func ask(_ cmd: String, _ timeout: TimeInterval = 1.5) -> String {
        let answer = link.send(cmd, timeout: timeout)
        print("> \(cmd)")
        print("  \(tidy(answer))")
        return answer
    }

    _ = ask("ATZ", 4)
    for cmd in ["ATE0", "ATL0", "ATS0", "ATH0", "ATSP0"] { _ = ask(cmd) }
    for pid in [0x00, 0x20, 0x40, 0x60] { _ = ask(String(format: "01%02X", pid), 2) }

    // The VAG identifiers that tools use for measured values, on the engine and gearbox modules.
    let known: [(String, UInt16)] = [("7E0", 0x1100), ("7E1", 0x2104)]
    for (header, did) in known {
        _ = ask("ATSH\(header)")
        let answer = ask(String(format: "22%04X", did), 2)
        if let bytes = udsBytes(answer, did: did) {
            print("  = \(header) \(String(format: "%04X", did)): \(hexText(bytes))")
        }
    }

    if scan {
        let ranges: [(String, UInt16, UInt16)] = [("7E0", 0x1100, 0x11FF), ("7E1", 0x2100, 0x21FF)]
        var hits = 0
        for (header, from, to) in ranges {
            print("scanning \(header) \(String(format: "%04X", from))…\(String(format: "%04X", to))…")
            _ = ask("ATSH\(header)")
            for did in from...to {
                let answer = link.send(String(format: "22%04X", did), timeout: 0.35)
                guard let bytes = udsBytes(answer, did: did) else { continue }
                hits += 1
                print("  \(header) \(String(format: "%04X", did)): \(hexText(bytes))")
            }
        }
        print("scan done: \(hits) identifiers answered")
    }

    _ = ask("ATSH7DF")
    link.close()
    _ = device.closeConnection()

default:
    print("usage: elm327_probe list | probe <name> [--scan]")
    exit(2)
}
