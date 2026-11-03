import struct

def parse_dex(dex_path):
    with open(dex_path, "rb") as f:
        data = f.read()

    magic = data[:8]
    if not magic.startswith(b"dex\n"):
        print(f"{dex_path}: invalid dex magic")
        return []

    string_ids_size, string_ids_off = struct.unpack("<II", data[56:64])
    type_ids_size, type_ids_off = struct.unpack("<II", data[64:72])
    class_defs_size, class_defs_off = struct.unpack("<II", data[96:104])

    print(f"{dex_path}: {string_ids_size} strings, {type_ids_size} types, {class_defs_size} classes")

    # Read string offsets
    string_offsets = [struct.unpack("<I", data[string_ids_off+i*4:string_ids_off+(i+1)*4])[0] for i in range(string_ids_size)]

    def get_string(idx):
        if idx >= len(string_offsets):
            return ""
        off = string_offsets[idx]
        # skip uleb128 utf16_size
        while data[off] & 0x80:
            off += 1
        off += 1
        # find null terminator
        end = data.find(b"\x00", off)
        if end == -1:
            return ""
        return data[off:end].decode("utf-8", errors="replace")

    # Read type_ids -> index into string_ids
    type_string_indices = [struct.unpack("<I", data[type_ids_off+i*4:type_ids_off+(i+1)*4])[0] for i in range(type_ids_size)]

    # Read class_defs -> class_idx (type_id)
    class_names = []
    for i in range(class_defs_size):
        c_off = class_defs_off + i * 32
        class_idx = struct.unpack("<I", data[c_off:c_off+4])[0]
        s_idx = type_string_indices[class_idx]
        cname = get_string(s_idx)
        class_names.append(cname)

    return class_names

if __name__ == "__main__":
    classes1 = parse_dex(r"D:\APK-Rebuild\original\extracted\classes.dex")
    classes2 = parse_dex(r"D:\APK-Rebuild\original\extracted\classes2.dex")

    all_classes = classes1 + classes2
    brainrot_classes = [c for c in all_classes if "brainrot" in c.lower()]

    print(f"\nTotal BrainRot/BrainPal classes: {len(brainrot_classes)}")
    
    # Group by packages
    pkgs = {}
    for c in brainrot_classes:
        clean = c.strip("L;").replace("/", ".")
        parts = clean.split(".")
        pkg = ".".join(parts[:-1])
        pkgs.setdefault(pkg, []).append(parts[-1])

    for pkg in sorted(pkgs.keys()):
        print(f"\nPackage: {pkg} ({len(pkgs[pkg])} classes)")
        for cls in sorted(pkgs[pkg])[:10]:
            print(f"  - {cls}")
        if len(pkgs[pkg]) > 10:
            print(f"  ... and {len(pkgs[pkg]) - 10} more")

