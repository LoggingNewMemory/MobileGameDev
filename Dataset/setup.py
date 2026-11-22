#!/usr/bin/env python3
"""
Setup script for DojoPulse Movement Training Environment.
Creates and configures the virtual environment with compatible packages for Arch Linux.
Supports bash, zsh, and fish shells.
"""

import os
import sys
import subprocess
import shutil

DATASET_DIR = os.path.dirname(os.path.abspath(__file__))
VENV_DIR = os.path.join(DATASET_DIR, ".venv")

REQUIRED_PACKAGES = [
    "opencv-python",
    "mediapipe==0.10.14",
    "protobuf<5,>=4.25.3",
    "tensorflow==2.15.1",
    "numpy<2,>=1.26.4",
    "ml-dtypes==0.3.2",
    "scikit-learn",
    "pandas",
    "joblib",
    "matplotlib"
]

def find_python():
    for p in ["/usr/bin/python3.11", shutil.which("python3.11"), shutil.which("python3")]:
        if p and os.path.exists(p):
            return p
    return sys.executable

def main():
    print("=" * 60)
    print("DojoPulse AI Movement Training Setup (Boxing and Swimming)")
    print("=" * 60)

    python_bin = find_python()
    print(f"[*] Detected Python: {python_bin}")

    uv_path = shutil.which("uv") or os.path.expanduser("~/.local/bin/uv")
    has_uv = os.path.exists(uv_path)

    if not os.path.exists(VENV_DIR):
        print(f"[*] Creating virtual environment in {VENV_DIR}...")
        if has_uv:
            subprocess.run([uv_path, "venv", VENV_DIR, "--python", python_bin], check=True)
        else:
            subprocess.run([python_bin, "-m", "venv", VENV_DIR], check=True)
    else:
        print(f"[OK] Virtual environment already exists at {VENV_DIR}")

    venv_python = os.path.join(VENV_DIR, "bin", "python")

    print("[*] Installing verified compatible packages...")
    if has_uv:
        cmd = [uv_path, "pip", "install", "--python", venv_python] + REQUIRED_PACKAGES
    else:
        cmd = [venv_python, "-m", "pip", "install"] + REQUIRED_PACKAGES

    subprocess.run(cmd, check=True)

    print("\n" + "=" * 60)
    print("Virtual environment setup complete!")
    print("=" * 60)
    print("To activate in Fish Shell (your current shell):")
    print(f"    source {os.path.join(VENV_DIR, 'bin', 'activate.fish')}")
    print("\nTo activate in Bash / Zsh:")
    print(f"    source {os.path.join(VENV_DIR, 'bin', 'activate')}")
    print("\nTo run training:")
    print(f"    {venv_python} train.py --sport all")
    print(f"    # To retrain with refreshed cache:")
    print(f"    {venv_python} train.py --sport boxing --force")
    print("=" * 60)

if __name__ == "__main__":
    main()
