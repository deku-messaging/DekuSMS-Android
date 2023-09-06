#!/usr/bin/python3
# encoding: utf-8
# SPDX-FileCopyrightText: 2023 FC Stegerman <flx@obfusk.net>
# SPDX-License-Identifier: GPL-3.0-or-later

import os
import subprocess
import tempfile
import zipfile

# The binary representation of this dummy Android XML file:
# <manifest xmlns:android="http://schemas.android.com/apk/res/android"
#           android:versionCode="1"
#           android:versionName="1"
#           android:compileSdkVersion="29"
#           android:compileSdkVersionCodename="10.0.0"
#           package="com.example"
#           platformBuildVersionCode="29"
#           platformBuildVersionName="10.0.0">
#   <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="29"/>
# </manifest>
AXML = (
    b"\x03\x00\x08\x00\xf0\x03\x00\x00\x01\x00\x1c\x00l\x02\x00\x00"
    b"\x10\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\\\x00\x00\x00"
    b"\x00\x00\x00\x00\x00\x00\x00\x00\x1e\x00\x00\x008\x00\x00\x00R\x00\x00\x00"
    b"v\x00\x00\x00\x9c\x00\x00\x00\xd2\x00\x00\x00\xd8\x00\x00\x00"
    b"\xe8\x00\x00\x00\xfa\x00\x00\x00\x14\x01\x00\x00l\x01\x00\x00"
    b"\x80\x01\x00\x00\x92\x01\x00\x00\xc6\x01\x00\x00\xfa\x01\x00\x00\r\x00m\x00"
    b"i\x00n\x00S\x00d\x00k\x00V\x00e\x00r\x00s\x00i\x00o\x00n\x00\x00\x00\x0b\x00"
    b"v\x00e\x00r\x00s\x00i\x00o\x00n\x00C\x00o\x00d\x00e\x00\x00\x00\x0b\x00v\x00"
    b"e\x00r\x00s\x00i\x00o\x00n\x00N\x00a\x00m\x00e\x00\x00\x00\x10\x00t\x00a\x00"
    b"r\x00g\x00e\x00t\x00S\x00d\x00k\x00V\x00e\x00r\x00s\x00i\x00o\x00n\x00"
    b"\x00\x00\x11\x00c\x00o\x00m\x00p\x00i\x00l\x00e\x00S\x00d\x00k\x00V\x00e\x00"
    b"r\x00s\x00i\x00o\x00n\x00\x00\x00\x19\x00c\x00o\x00m\x00p\x00i\x00l\x00e\x00"
    b"S\x00d\x00k\x00V\x00e\x00r\x00s\x00i\x00o\x00n\x00C\x00o\x00d\x00e\x00"
    b"n\x00a\x00m\x00e\x00\x00\x00\x01\x001\x00\x00\x00\x06\x001\x000\x00.\x00"
    b"0\x00.\x000\x00\x00\x00\x07\x00a\x00n\x00d\x00r\x00o\x00i\x00d\x00"
    b"\x00\x00\x0b\x00c\x00o\x00m\x00.\x00e\x00x\x00a\x00m\x00p\x00l\x00"
    b"e\x00\x00\x00*\x00h\x00t\x00t\x00p\x00:\x00/\x00/\x00s\x00c\x00h\x00e\x00"
    b"m\x00a\x00s\x00.\x00a\x00n\x00d\x00r\x00o\x00i\x00d\x00.\x00c\x00o\x00"
    b"m\x00/\x00a\x00p\x00k\x00/\x00r\x00e\x00s\x00/\x00a\x00n\x00d\x00r\x00"
    b"o\x00i\x00d\x00\x00\x00\x08\x00m\x00a\x00n\x00i\x00f\x00e\x00s\x00"
    b"t\x00\x00\x00\x07\x00p\x00a\x00c\x00k\x00a\x00g\x00e\x00\x00\x00\x18\x00"
    b"p\x00l\x00a\x00t\x00f\x00o\x00r\x00m\x00B\x00u\x00i\x00l\x00d\x00V\x00"
    b"e\x00r\x00s\x00i\x00o\x00n\x00C\x00o\x00d\x00e\x00\x00\x00\x18\x00p\x00l\x00"
    b"a\x00t\x00f\x00o\x00r\x00m\x00B\x00u\x00i\x00l\x00d\x00V\x00e\x00r\x00"
    b"s\x00i\x00o\x00n\x00N\x00a\x00m\x00e\x00\x00\x00\x08\x00u\x00s\x00e\x00s\x00"
    b"-\x00s\x00d\x00k\x00\x00\x00\x00\x00\x80\x01\x08\x00 \x00\x00\x00"
    b"\x0c\x02\x01\x01\x1b\x02\x01\x01\x1c\x02\x01\x01p\x02\x01\x01r\x05\x01\x01"
    b"s\x05\x01\x01\x00\x01\x10\x00\x18\x00\x00\x00\x01\x00\x00\x00"
    b"\xff\xff\xff\xff\x08\x00\x00\x00\n\x00\x00\x00\x02\x01\x10\x00"
    b"\xb0\x00\x00\x00\x01\x00\x00\x00\xff\xff\xff\xff\xff\xff\xff\xff"
    b"\x0b\x00\x00\x00\x14\x00\x14\x00\x07\x00\x00\x00\x00\x00\x00\x00"
    b"\n\x00\x00\x00\x01\x00\x00\x00\xff\xff\xff\xff\x08\x00\x00\x10"
    b"\x01\x00\x00\x00\n\x00\x00\x00\x02\x00\x00\x00\x06\x00\x00\x00"
    b"\x08\x00\x00\x03\x06\x00\x00\x00\n\x00\x00\x00\x04\x00\x00\x00"
    b"\xff\xff\xff\xff\x08\x00\x00\x10\x1d\x00\x00\x00\n\x00\x00\x00"
    b"\x05\x00\x00\x00\x07\x00\x00\x00\x08\x00\x00\x03\x07\x00\x00\x00"
    b"\xff\xff\xff\xff\x0c\x00\x00\x00\t\x00\x00\x00\x08\x00\x00\x03\t\x00\x00\x00"
    b"\xff\xff\xff\xff\r\x00\x00\x00\xff\xff\xff\xff\x08\x00\x00\x10"
    b"\x1d\x00\x00\x00\xff\xff\xff\xff\x0e\x00\x00\x00\x07\x00\x00\x00"
    b"\x08\x00\x00\x03\x07\x00\x00\x00\x02\x01\x10\x00L\x00\x00\x00"
    b"\x02\x00\x00\x00\xff\xff\xff\xff\xff\xff\xff\xff\x0f\x00\x00\x00"
    b"\x14\x00\x14\x00\x02\x00\x00\x00\x00\x00\x00\x00\n\x00\x00\x00"
    b"\x00\x00\x00\x00\xff\xff\xff\xff\x08\x00\x00\x10\x15\x00\x00\x00"
    b"\n\x00\x00\x00\x03\x00\x00\x00\xff\xff\xff\xff\x08\x00\x00\x10"
    b"\x1d\x00\x00\x00\x03\x01\x10\x00\x18\x00\x00\x00\x02\x00\x00\x00"
    b"\xff\xff\xff\xff\xff\xff\xff\xff\x0f\x00\x00\x00\x03\x01\x10\x00"
    b"\x18\x00\x00\x00\x01\x00\x00\x00\xff\xff\xff\xff\xff\xff\xff\xff"
    b"\x0b\x00\x00\x00\x01\x01\x10\x00\x18\x00\x00\x00\x01\x00\x00\x00"
    b"\xff\xff\xff\xff\x08\x00\x00\x00\n\x00\x00\x00"
)


class Error(RuntimeError):
    pass


def dump_arsc(file: str) -> None:
    with open(file, "rb") as fh:
        arsc = fh.read()
    with tempfile.TemporaryDirectory() as tdir:
        apk = os.path.join(tdir, "out.apk")
        with zipfile.ZipFile(apk, "w") as zf:
            zf.writestr("AndroidManifest.xml", AXML)
            zf.writestr("resources.arsc", arsc)
        dump_arsc_apk(apk)


def dump_arsc_apk(file: str) -> None:
    aapt = ("aapt2", "dump", "resources", file)
    try:
        subprocess.run(aapt, check=True, stderr=subprocess.PIPE)
    except subprocess.CalledProcessError as e:
        raise Error("aapt2 command failed") from e
    except FileNotFoundError as e:
        raise Error("aapt2 command not found") from e


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(prog="dump-arsc.py")
    parser.add_argument("--apk", action="store_true")
    parser.add_argument("arsc_or_apk", metavar="ARSC_OR_APK")
    args = parser.parse_args()
    if args.apk:
        dump_arsc_apk(args.arsc_or_apk)
    else:
        dump_arsc(args.arsc_or_apk)

# vim: set tw=80 sw=4 sts=4 et fdm=marker :
