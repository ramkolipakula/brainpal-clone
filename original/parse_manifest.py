import struct
import re

def extract_manifest_strings(file_path):
    with open(file_path, "rb") as f:
        data = f.read()

    offset = 8
    strings = []
    while offset < len(data):
        if offset + 8 > len(data):
            break
        chunk_type, chunk_size = struct.unpack("<II", data[offset:offset+8])
        if chunk_size < 8:
            break

        if chunk_type == 0x001C0001:  # String pool
            sp_data = data[offset:offset+chunk_size]
            string_count, style_count, flags, strings_start, styles_start = struct.unpack("<IIIII", sp_data[8:28])
            is_utf8 = bool(flags & (1 << 8))
            offsets = [struct.unpack("<I", sp_data[28+i*4:32+i*4])[0] for i in range(string_count)]
            
            for off in offsets:
                idx = strings_start + off
                if is_utf8:
                    b = sp_data[idx]
                    idx += 2 if (b & 0x80) else 1
                    b = sp_data[idx]
                    if b & 0x80:
                        u8len = ((b & 0x7F) << 8) | sp_data[idx+1]
                        idx += 2
                    else:
                        u8len = b
                        idx += 1
                    s_bytes = sp_data[idx:idx+u8len]
                    try:
                        strings.append(s_bytes.decode('utf-8', errors='replace'))
                    except:
                        strings.append("")
                else:
                    u16len = struct.unpack("<H", sp_data[idx:idx+2])[0]
                    idx += 2
                    s_bytes = sp_data[idx:idx+u16len*2]
                    try:
                        strings.append(s_bytes.decode('utf-16le', errors='replace'))
                    except:
                        strings.append("")
            break
        offset += chunk_size
    return strings

if __name__ == "__main__":
    path = r"D:\APK-Rebuild\original\extracted\AndroidManifest.xml"
    strings = extract_manifest_strings(path)
    print(f"Total strings in manifest: {len(strings)}")
    
    permissions = [s for s in strings if "permission" in s.lower() or "android.permission" in s]
    activities = [s for s in strings if "Activity" in s]
    services = [s for s in strings if "Service" in s]
    receivers = [s for s in strings if "Receiver" in s]
    providers = [s for s in strings if "Provider" in s]

    print("\n--- IDENTIFIED PERMISSIONS ---")
    for p in sorted(set(permissions)):
        print(f"  {p}")

    print("\n--- IDENTIFIED ACTIVITIES ---")
    for a in sorted(set(activities)):
        print(f"  {a}")

    print("\n--- IDENTIFIED SERVICES ---")
    for s in sorted(set(services)):
        print(f"  {s}")

    print("\n--- IDENTIFIED RECEIVERS ---")
    for r in sorted(set(receivers)):
        print(f"  {r}")

    print("\n--- ALL IDENTIFIERS / PACKAGES ---")
    pkg_like = [s for s in strings if "." in s and not s.startswith("android.")]
    for p in sorted(set(pkg_like)):
        print(f"  {p}")
