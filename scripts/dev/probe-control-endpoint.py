#!/usr/bin/env python3
import argparse
import json
import socket
import struct
import urllib.error
import urllib.request


def http_probe(base_url: str, path: str) -> dict:
    url = f"{base_url}{path}"
    request = urllib.request.Request(url, headers={"Connection": "close"})
    try:
        with urllib.request.urlopen(request, timeout=2) as response:
            body = response.read(200).decode("utf-8", errors="replace")
            return {
                "ok": True,
                "status": response.status,
                "contentType": response.headers.get("Content-Type", ""),
                "bodyPreview": body,
            }
    except urllib.error.HTTPError as error:
        return {
            "ok": False,
            "status": error.code,
            "reason": str(error.reason),
        }
    except Exception as error:  # noqa: BLE001
        return {"ok": False, "error": str(error)}


def adb_probe(host: str, port: int) -> dict:
    payload = b"host::flowy-probe\x00"
    command = struct.unpack("<I", b"CNXN")[0]
    checksum = sum(payload) & 0xFFFFFFFF
    magic = command ^ 0xFFFFFFFF
    packet = struct.pack(
        "<6I",
        command,
        0x01000001,
        4096,
        len(payload),
        checksum,
        magic,
    ) + payload

    with socket.create_connection((host, port), timeout=2) as conn:
        conn.settimeout(2)
        conn.sendall(packet)
        header = conn.recv(24)
        if len(header) != 24:
            return {"ok": False, "error": f"short-header:{len(header)}"}
        cmd, arg0, arg1, length, body_checksum, body_magic = struct.unpack("<6I", header)
        body = b""
        while len(body) < length:
            chunk = conn.recv(length - len(body))
            if not chunk:
                break
            body += chunk
        return {
            "ok": True,
            "command": struct.pack("<I", cmd).decode("ascii", errors="replace"),
            "arg0": arg0,
            "arg1": arg1,
            "length": length,
            "checksum": body_checksum,
            "magic": body_magic,
            "bodyPreview": body[:200].decode("utf-8", errors="replace"),
        }


def raw_probe(host: str, port: int, payload: bytes = b"") -> dict:
    with socket.create_connection((host, port), timeout=2) as conn:
        conn.settimeout(2)
        if payload:
            conn.sendall(payload)
        try:
            data = conn.recv(200)
            return {"ok": True, "recvLen": len(data), "dataPreview": data.hex()}
        except socket.timeout:
            return {"ok": True, "recvTimeout": True}


def classify(result: dict) -> str:
    adb = result.get("adb", {})
    health = result.get("httpHealth", {})
    clients = result.get("httpClients", {})
    if health.get("ok") or clients.get("ok"):
        return "flowy-exp01-http"
    if adb.get("ok") and adb.get("command") == "STLS":
        return "adb-wireless-debugging-tls"
    if adb.get("ok") and adb.get("command") in {"AUTH", "CNXN"}:
        return "adb-transport"
    return "unknown"


def main() -> None:
    parser = argparse.ArgumentParser(description="Probe a control endpoint and classify its protocol.")
    parser.add_argument("target", help="host:port")
    args = parser.parse_args()

    host, port_text = args.target.rsplit(":", 1)
    port = int(port_text)
    base_url = f"http://{host}:{port}"

    result = {
        "target": args.target,
        "rawConnect": None,
        "httpHealth": None,
        "httpClients": None,
        "adb": None,
        "classification": None,
    }

    for key, fn in {
        "rawConnect": lambda: raw_probe(host, port),
        "httpHealth": lambda: http_probe(base_url, "/health"),
        "httpClients": lambda: http_probe(base_url, "/exp01/clients"),
        "adb": lambda: adb_probe(host, port),
    }.items():
        try:
            result[key] = fn()
        except Exception as error:  # noqa: BLE001
            result[key] = {"ok": False, "error": str(error)}

    result["classification"] = classify(result)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
