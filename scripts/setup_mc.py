#!/usr/bin/env python3
"""Готовит всё для компиляции: клиент Minecraft 1.21.4 в intermediary-именах + библиотеки."""
import json
import os
import re
import subprocess
import sys
import urllib.request
import zipfile

MC = "1.21.4"
OUT = os.path.join("build", "mc")
LIBS = os.path.join(OUT, "libs")
UA = {"User-Agent": "applefarm-build/1.0"}


def fetch(url, dest=None, tries=3):
    last = None
    for _ in range(tries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=120) as r:
                data = r.read()
            if dest:
                os.makedirs(os.path.dirname(dest), exist_ok=True)
                with open(dest, "wb") as f:
                    f.write(data)
            return data
        except Exception as e:  # noqa: BLE001
            last = e
    raise SystemExit(f"Не удалось скачать {url}: {last}")


def main():
    os.makedirs(LIBS, exist_ok=True)

    # 1. Minecraft client + библиотеки
    manifest = json.loads(fetch("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
    entry = next(v for v in manifest["versions"] if v["id"] == MC)
    version = json.loads(fetch(entry["url"]))
    client = os.path.join(OUT, "client.jar")
    print("Скачиваю клиент", MC)
    fetch(version["downloads"]["client"]["url"], client)

    lib_files = []
    for lib in version["libraries"]:
        art = lib.get("downloads", {}).get("artifact")
        if not art:
            continue
        dest = os.path.join(LIBS, art["path"].replace("/", "_"))
        if not os.path.exists(dest):
            fetch(art["url"], dest)
        lib_files.append(dest)
    print("Библиотек:", len(lib_files))

    # 2. intermediary-маппинги
    inter_jar = os.path.join(OUT, f"intermediary-{MC}-v2.jar")
    fetch(f"https://maven.fabricmc.net/net/fabricmc/intermediary/{MC}/intermediary-{MC}-v2.jar", inter_jar)
    tiny = os.path.join(OUT, "mappings.tiny")
    with zipfile.ZipFile(inter_jar) as z, open(tiny, "wb") as f:
        f.write(z.read("mappings/mappings.tiny"))

    # 3. tiny-remapper (последний релиз)
    meta = fetch("https://maven.fabricmc.net/net/fabricmc/tiny-remapper/maven-metadata.xml").decode()
    ver = re.search(r"<release>([^<]+)</release>", meta).group(1)
    remapper = os.path.join(OUT, "tiny-remapper-fat.jar")
    fetch(f"https://maven.fabricmc.net/net/fabricmc/tiny-remapper/{ver}/tiny-remapper-{ver}-fat.jar", remapper)

    # 4. official -> intermediary
    result = os.path.join(OUT, "intermediary-client.jar")
    print("Ремап в intermediary...")
    subprocess.check_call(["java", "-jar", remapper, client, result, tiny, "official", "intermediary", *lib_files])
    print("Готово:", result)


if __name__ == "__main__":
    sys.exit(main())
